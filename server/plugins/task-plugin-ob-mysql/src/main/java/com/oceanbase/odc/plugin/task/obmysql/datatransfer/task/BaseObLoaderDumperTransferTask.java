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

import static com.oceanbase.odc.core.shared.constant.OdcConstants.DEFAULT_ZERO_DATE_TIME_BEHAVIOR;
import static com.oceanbase.tools.loaddump.common.constants.Constants.JdbcConsts.JDBC_URL_USE_SERVER_PREP_STMTS;
import static com.oceanbase.tools.loaddump.common.constants.Constants.JdbcConsts.JDBC_URL_ZERO_DATETIME_BEHAVIOR;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferTask;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus.Status;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.TransferTaskStatus;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.BaseParameter;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.common.model.TaskDetail;
import com.oceanbase.tools.loaddump.context.TaskContext;
import com.oceanbase.tools.loaddump.manager.session.SessionProperties;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BaseObLoaderDumperTransferTask}
 *
 * @author yh263208
 * @date 2022-07-25 14:41
 * @since ODC_release_3.4.0
 */
public abstract class BaseObLoaderDumperTransferTask<T extends BaseParameter> implements DataTransferTask {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    protected final T parameter;
    protected final boolean transferData;
    protected final boolean transferSchema;
    private final long sleepInterval;
    private final boolean usePrepStmts;
    private TaskContext schemaContext;
    private TaskContext dataContext;

    public BaseObLoaderDumperTransferTask(@NonNull T parameter, boolean transferData, boolean transferSchema,
            boolean usePrepStmts) {
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
        this.sleepInterval = 500;
        this.usePrepStmts = usePrepStmts;
    }

    protected abstract TaskContext startTransferData() throws Exception;

    protected abstract TaskContext startTransferSchema() throws Exception;

    protected abstract void postHandle(TransferTaskStatus result);

    @Override
    public TransferTaskStatus getStatus() {
        TransferTaskStatus status = new TransferTaskStatus();
        if (schemaContext != null) {
            status.setSchemaObjectsInfo(transformStatus(schemaContext.getSummary().getObjectStatusList()));
        }
        if (dataContext != null) {
            status.setDataObjectsInfo(transformStatus(dataContext.getSummary().getObjectStatusList()));
        }
        return status;
    }

    @Override
    public double getProgress() {
        double returnVal = 0.0;
        int totalTaskCount = 0;
        if (this.schemaContext != null) {
            totalTaskCount++;
            returnVal += this.schemaContext.getProgress().getProgress();
        }
        if (this.dataContext != null) {
            totalTaskCount++;
            returnVal += this.dataContext.getProgress().getProgress();
        }
        if (totalTaskCount <= 0) {
            return 0.0;
        }
        return returnVal / totalTaskCount;
    }

    @Override
    public TransferTaskStatus transfer() throws Exception {
        try {
            setSessionProperties();

            String fileSuffix = parameter.getFileSuffix();
            if (transferSchema) {
                LOGGER.info("Begin transferring schema");
                parameter.setFileSuffix(DataFormat.SQL.getDefaultFileSuffix());
                schemaContext = startTransferSchema();
                if (schemaContext == null) {
                    throw new NullPointerException("Data task context is null");
                }
                syncWaitFinished(schemaContext);
            }

            if (transferData) {
                LOGGER.info("Begin transferring data");
                parameter.setFileSuffix(fileSuffix);
                dataContext = startTransferData();
                if (dataContext == null) {
                    throw new NullPointerException("Data task context is null");
                }
                syncWaitFinished(dataContext);
            }

            LOGGER.info("Transfer task finished by ob-loader-dumper!");
        } catch (Exception e) {
            if (schemaContext != null) {
                schemaContext.shutdownNow();
            }
            if (dataContext != null) {
                dataContext.shutdownNow();
            }
            throw e;
        }

        validAllTasksSuccessed();

        TransferTaskStatus status = getStatus();
        postHandle(status);
        return status;
    }

    @SuppressWarnings("all")
    private void syncWaitFinished(@NonNull TaskContext context) throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            if (context.isAllTasksSuccessed()) {
                shutdownContext(context);
                return;
            } else if (context.isAllTasksFinished()) {
                shutdownContext(context);
                Collection<TaskDetail> failedTasks = context.getFailureTaskDetails();
                if (CollectionUtils.isEmpty(failedTasks)) {
                    throw new IllegalStateException("No failed task details");
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
        Verify.verify(dataContext != null || schemaContext != null, "No context available");
        if (schemaContext != null) {
            Verify.verify(schemaPredicate.test(schemaContext), "Schema context verify failed");
        }
        if (dataContext != null) {
            Verify.verify(dataContextPredicate.test(dataContext), "Data context verify failed");
        }
    }

    private void setSessionProperties() {
        SessionProperties.setString(JDBC_URL_USE_SERVER_PREP_STMTS, String.valueOf(usePrepStmts));
        SessionProperties.setString(JDBC_URL_ZERO_DATETIME_BEHAVIOR, DEFAULT_ZERO_DATE_TIME_BEHAVIOR);
    }

    /**
     * 导入导出组件在某些场景下（例如停止任务），会出现 {@link com.oceanbase.tools.loaddump.common.model.ObjectStatus#getName()}
     * 为 {@code null} 的场景，这会造成前端显示错误。在这里过滤掉这个异常值。
     */
    private List<ObjectStatus> transformStatus(List<com.oceanbase.tools.loaddump.common.model.ObjectStatus> origin) {
        return origin.stream()
                .filter(s -> s.getName() != null)
                .peek(object -> {
                    if (StringUtils.isNotBlank(object.getType())) {
                        object.setType(ObjectType.valueOfName(object.getType()).name());
                    }
                }).map(object -> {
                    ObjectStatus target = new ObjectStatus();
                    target.setStatus(Status.valueOf(object.getStatus().name()));
                    target.setType(object.getType());
                    target.setCount(object.getCount().get());
                    target.setTotal(object.getTotal().get());
                    target.setSchema(object.getSchema());
                    target.setName(object.getName());
                    return target;
                }).collect(Collectors.toList());
    }

}
