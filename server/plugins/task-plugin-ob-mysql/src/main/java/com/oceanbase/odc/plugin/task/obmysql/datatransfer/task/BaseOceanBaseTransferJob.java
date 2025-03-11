/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oceanbase.odc.plugin.task.obmysql.datatransfer.task;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferJob;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.enums.ServerMode;
import com.oceanbase.tools.loaddump.common.model.BaseParameter;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.common.model.LoadParameter;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus;
import com.oceanbase.tools.loaddump.common.model.TaskDetail;
import com.oceanbase.tools.loaddump.context.TaskContext;

import lombok.NonNull;

/**
 * {@link BaseOceanBaseTransferJob}
 *
 * @author yh263208
 * @date 2022-07-25 14:41
 * @since ODC_release_3.4.0
 */
public abstract class BaseOceanBaseTransferJob<T extends BaseParameter> implements DataTransferJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");
    private static final String HADOOP_PATH =
            Paths.get(MoreObjects.firstNonNull(SystemUtils.getEnvOrProperty("libraries.others.file.path"), ""),
                    "hadoop-3.3.6").toString();

    protected final T parameter;
    protected final boolean transferData;
    protected final boolean transferSchema;
    private final long sleepInterval;
    private volatile TaskStatus status = TaskStatus.PREPARING;
    private TaskContext schemaContext;
    private TaskContext dataContext;
    private int totalTaskCount = 0;

    public BaseOceanBaseTransferJob(@NonNull T parameter, boolean transferData, boolean transferSchema) {
        this.parameter = parameter;
        this.transferSchema = transferSchema;
        if (parameter instanceof DumpParameter) {
            /*
             * 只有 whiteListMap 中包含表对象才能导出数据，否则会报错，后端把这个异常处理掉
             */
            this.transferData = parameter.getWhiteListMap().containsKey(ObjectType.TABLE) && transferData;
        } else {
            this.transferData = transferData;
        }
        totalTaskCount += transferData ? 1 : 0;
        totalTaskCount += transferSchema ? 1 : 0;
        this.sleepInterval = 500;
    }

    protected abstract TaskContext startTransferData() throws Exception;

    protected abstract TaskContext startTransferSchema() throws Exception;

    @Override
    public List<ObjectResult> getDataObjectsStatus() {
        return dataContext == null ? Collections.emptyList()
                : transformStatus(dataContext.getSummary().getObjectStatusList());
    }

    @Override
    public List<ObjectResult> getSchemaObjectsStatus() {
        return schemaContext == null ? Collections.emptyList()
                : transformStatus(schemaContext.getSummary().getObjectStatusList());
    }

    @Override
    public double getProgress() {
        double returnVal = 0.0;
        if (this.schemaContext != null) {
            try {
                returnVal += this.schemaContext.getProgress().getProgress();
            } catch (Exception e) {
                LOGGER.warn("Failed to get progress from ob-loader-dumper, reason:{}", e.getMessage());
            }
        }
        if (this.dataContext != null) {
            try {
                returnVal += this.dataContext.getProgress().getProgress();
            } catch (Exception e) {
                LOGGER.warn("Failed to get progress from ob-loader-dumper, reason:{}", e.getMessage());
            }
        }
        if (totalTaskCount <= 0) {
            return 0.0;
        }
        return returnVal / totalTaskCount;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (mayInterruptIfRunning) {
            try {
                if (schemaContext != null) {
                    schemaContext.shutdownNow();
                }
                if (dataContext != null) {
                    dataContext.shutdownNow();
                }
            } catch (Exception ignore) {
                // eat exception
                return false;
            }
        }
        status = TaskStatus.CANCELED;
        return true;
    }

    @Override
    public boolean isCanceled() {
        return status == TaskStatus.CANCELED;
    }

    @Override
    public DataTransferTaskResult call() throws Exception {
        try {
            status = TaskStatus.RUNNING;
            System.clearProperty("logging.level");
            if (SystemUtils.isOnWindows()) {
                checkHadoopPath();
            }
            String fileSuffix = parameter.getFileSuffix();
            if (transferSchema) {
                LOGGER.info("Begin transferring schema");
                parameter.setFileSuffix(DataFormat.SQL.getDefaultFileSuffix());
                schemaContext = startTransferSchema();
                if (schemaContext == null) {
                    throw new NullPointerException("Data task context is null");
                }
                syncWaitFinished(schemaContext, true);
            }

            if (transferData) {
                LOGGER.info("Begin transferring data");
                parameter.setFileSuffix(fileSuffix);
                dataContext = startTransferData();
                if (dataContext == null) {
                    throw new NullPointerException("Data task context is null");
                }
                syncWaitFinished(dataContext, false);
            }

            LOGGER.info("Transfer task finished by ob-loader-dumper!");
        } catch (Exception e) {
            if (schemaContext != null) {
                schemaContext.shutdownNow();
            }
            if (dataContext != null) {
                dataContext.shutdownNow();
            }
            String rootMessage = Optional.ofNullable(ExceptionUtils.getRootCause(e)).orElseThrow(() -> e).getMessage();
            if (shouldEatException(rootMessage)) {
                LOGGER.warn(rootMessage);
            } else {
                throw e;
            }
        }

        validAllTasksSuccessed();
        status = TaskStatus.DONE;
        return new DataTransferTaskResult(getDataObjectsStatus(), getSchemaObjectsStatus());
    }

    /**
     * When user configured {@link DataTransferConfig#replaceSchemaWhenExists} is false, we would set
     * {@link LoadParameter#replaceObjectIfExists} to false. But ob-loader would still report error if
     * an object exists already. So we should eat these exceptions. Our strategy for handling exceptions
     * is as follows:
     * 
     * <pre>
     * +----------------+--------------------------+----------------------------+-----------------------------+
     * | stopWhenError | replaceSchemaWhenExists | Error Message Contains "already exists" | Handling Behavior |
     * +----------------+--------------------------+----------------------------+-----------------------------+
     * | true          |          N/A            |            N/A             |   Always skip errors        |
     * +----------------+--------------------------+----------------------------+-----------------------------+
     * | false         |         true            |            N/A             |   Throw exception           |
     * +----------------+--------------------------+----------------------------+-----------------------------+
     * | false         |         false           |          Yes               |   Filter out, not throw     |
     * +----------------+--------------------------+----------------------------+-----------------------------+
     * | false         |         false           |           No               |   Throw exception           |
     * +----------------+--------------------------+----------------------------+-----------------------------+
     * </pre>
     */
    @SuppressWarnings("all")
    private void syncWaitFinished(@NonNull TaskContext context, boolean isTransferSchema) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted() && !status.isTerminated()) {
            if (context.isAllTasksSuccessed()) {
                shutdownContext(context);
                return;
            } else if (context.isAllTasksFinished()) {
                shutdownContext(context);
                Collection<TaskDetail> failedTasks = context.getFailureTaskDetails();
                if (CollectionUtils.isEmpty(failedTasks)) {
                    throw new IllegalStateException("No failed task details");
                }
                if (isTransferSchema && parameter instanceof LoadParameter) {
                    if (((LoadParameter) parameter).getMaxErrors() == -1) {
                        return;
                    }
                    if (!((LoadParameter) parameter).isReplaceObjectIfExists()) {
                        failedTasks = failedTasks.stream()
                                .filter(i -> !isCreateExistsObjectError(i.getError())).collect(Collectors.toList());
                        if (failedTasks.isEmpty()) {
                            return;
                        }
                    }
                }
                String errorMsg = failedTasks.stream()
                        .map(i -> i.getSchemaTable() + ": " + i.getError())
                        .collect(Collectors.joining("\n"));
                throw new IllegalStateException(errorMsg);
            }
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
        throw new InterruptedException("loop interrupted");
    }

    /**
     * @return a bool value. If it's true, the exception should be eaten.
     */
    private boolean shouldEatException(String errorMessage) {
        if (StringUtils.isEmpty(errorMessage)) {
            return false;
        }
        if (errorMessage.contains("No subfiles are generated from path")) {
            return true;
        }
        if (errorMessage.contains("table or view does not exist")) {
            ServerMode serverMode = parameter.getConnectionKey().getServerMode();
            if (serverMode.isMysqlMode()) {
                LOGGER.warn(
                        "The current user may lack access to the oceanbase database. "
                                + "You can execute the following SQL and retry the transfer task:\n"
                                + "grant select on oceanbase.* to {}",
                        parameter.getUser());
            } else {
                LOGGER.warn(
                        "The current user may lack of DBA permission. "
                                + "You can execute the following SQL and retry the transfer task:\n"
                                + "grant select any dictionary to {}",
                        parameter.getUser());
            }
        }
        return false;
    }

    private void shutdownContext(TaskContext context) {
        try {
            context.shutdown();
            LOGGER.info("shutdown task context finished");
        } catch (Exception e) {
            try {
                context.shutdownNow();
                LOGGER.info("shutdown task context immediately finished");
            } catch (Exception ex) {
                LOGGER.warn("shutdown task context immediately failed", ex);
            }
        } finally {
            LOGGER.info(context.getProgress().toString());
            LOGGER.info(context.getSummary().toHumanReadableFormat());
        }
    }

    public void validAllTasksSuccessed() {
        validAllTasksFinished();
        validContext(TaskContext::isAllTasksSuccessed, TaskContext::isAllTasksSuccessed);
    }

    public void validAllTasksFinished() {
        validContext(TaskContext::isAllTasksFinished, TaskContext::isAllTasksFinished);
    }

    private void validContext(Predicate<TaskContext> schemaPredicate,
            Predicate<TaskContext> dataContextPredicate) {
        if (schemaContext != null) {
            Verify.verify(schemaPredicate.test(schemaContext), "Schema context verify failed");
        }
        if (dataContext != null) {
            Verify.verify(dataContextPredicate.test(dataContext), "Data context verify failed");
        }
    }

    /**
     * 导入导出组件在某些场景下（例如停止任务），会出现 {@link ObjectStatus#getName()} 为 {@code null} 的场景，这会造成前端显示错误。这里过滤掉这个异常值。
     */
    private List<ObjectResult> transformStatus(List<ObjectStatus> origin) {
        return origin.stream()
                .filter(s -> s.getName() != null)
                .map(ObjectResult::of)
                .collect(Collectors.toList());
    }

    private boolean isCreateExistsObjectError(String error) {
        return StringUtils.containsIgnoreCase(error, "already exist") ||
                StringUtils.containsIgnoreCase(error, "already used by an existing object");
    }

    private void checkHadoopPath() {
        File hadoop = new File(HADOOP_PATH);
        if (!hadoop.exists()) {
            throw new IllegalArgumentException("HADOOP_HOME is not set or not exists");
        }
        System.setProperty("hadoop.home.dir", hadoop.getAbsolutePath());
    }

}
