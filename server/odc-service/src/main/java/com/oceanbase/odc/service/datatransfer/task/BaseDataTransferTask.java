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
package com.oceanbase.odc.service.datatransfer.task;

import static com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConstants.LOG_PATH_NAME;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.tools.loaddump.common.enums.DataFormat;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.BaseParameter;
import com.oceanbase.tools.loaddump.common.model.DumpParameter;
import com.oceanbase.tools.loaddump.common.model.TaskDetail;
import com.oceanbase.tools.loaddump.context.TaskContext;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BaseDataTransferTask}
 *
 * @author yh263208
 * @date 2022-07-25 14:41
 * @since ODC_release_3.4.0
 */
@Slf4j
public abstract class BaseDataTransferTask<T extends BaseParameter> implements Callable<DataTransferTaskResult> {
    protected final T parameter;
    private final boolean transferData;
    private final boolean transferSchema;
    private final long sleepInterval;
    private volatile CountDownLatch contextLatch;
    private volatile DataTransferTaskContext context;
    @Setter
    protected boolean mergeSchemaFiles;

    public BaseDataTransferTask(@NonNull T parameter, boolean transferData, boolean transferSchema) {
        this.parameter = parameter;
        this.transferSchema = transferSchema;
        if (parameter instanceof DumpParameter) {
            /**
             * 只有 whiteListMap 中包含表对象才能导出数据，否则会报错，后端把这个异常处理掉
             */
            this.transferData = parameter.getWhiteListMap().containsKey(ObjectType.TABLE) && transferData;
        } else {
            this.transferData = transferData;
        }
        this.sleepInterval = 500;
        ThreadContext.put(LOG_PATH_NAME, parameter.getLogsPath());
    }

    public static DataTransferTaskContext start(@NonNull ThreadPoolTaskExecutor executor,
            @NonNull BaseDataTransferTask<? extends BaseParameter> task) {
        if (task.contextLatch != null || task.context != null) {
            throw new IllegalStateException("Task is running, can not start");
        }
        task.contextLatch = new CountDownLatch(1);
        Future<DataTransferTaskResult> future = executor.submit(task);
        DataTransferTaskContext context = new DataTransferTaskContext(future);
        task.context = context;
        task.contextLatch.countDown();
        return context;
    }

    public static void validAllTasksSuccessed(@NonNull DataTransferTaskContext context) {
        validAllTasksFinished(context);
        validContext(context, TaskContext::isAllTasksSuccessed, TaskContext::isAllTasksSuccessed);
    }

    public static void validAllTasksFinished(@NonNull DataTransferTaskContext context) {
        validContext(context, TaskContext::isAllTasksFinished, TaskContext::isAllTasksFinished);
    }

    private static void validContext(@NonNull DataTransferTaskContext context,
            Predicate<TaskContext> schemaPredicate, Predicate<TaskContext> dataContextPredicate) {
        TaskContext dataContext = context.dataContext;
        TaskContext schemaContext = context.schemaContext;
        Verify.verify(dataContext != null || schemaContext != null, "No context available");
        if (schemaContext != null) {
            Verify.verify(schemaPredicate.test(schemaContext), "Schema context verify failed");
        }
        if (dataContext != null) {
            Verify.verify(dataContextPredicate.test(dataContext), "Data context verify failed");
        }
    }

    @Override
    public DataTransferTaskResult call() throws Exception {
        if (contextLatch == null) {
            throw new NullPointerException("Context latch is null");
        }
        if (!this.contextLatch.await(3, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Failed to set context");
        } else if (context == null) {
            throw new NullPointerException("Data transfer task context is null");
        }
        try {
            String fileSuffix = parameter.getFileSuffix();
            if (transferSchema) {
                log.info("Begin transferring schema");
                parameter.setFileSuffix(DataFormat.SQL.getDefaultFileSuffix());
                TaskContext taskContext = startTransferSchema(parameter);
                if (taskContext == null) {
                    throw new NullPointerException("Schema task context is null");
                }
                context.schemaContext = taskContext;
                syncWaitFinished(taskContext);
            }
            if (transferData) {
                log.info("Begin transferring data");
                parameter.setFileSuffix(fileSuffix);
                TaskContext taskContext = startTransferData(parameter);
                if (taskContext == null) {
                    throw new NullPointerException("Data task context is null");
                }
                context.dataContext = taskContext;
                syncWaitFinished(taskContext);
            }
            DataTransferTaskResult result = DataTransferTaskResult.of(context);
            afterHandle(parameter, context, result);
            return result;
        } catch (Exception e) {
            log.warn("Failed to execute data transfer task", e);
            if (context.schemaContext != null) {
                context.schemaContext.shutdownNow();
            }
            if (context.dataContext != null) {
                context.dataContext.shutdownNow();
            }
            throw e;
        }
    }

    protected abstract TaskContext startTransferData(T parameter) throws Exception;

    protected abstract TaskContext startTransferSchema(T parameter) throws Exception;

    protected abstract void afterHandle(T parameter, DataTransferTaskContext context,
            DataTransferTaskResult result) throws Exception;

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
            log.info("shutdown task context finished");
        } catch (Exception e) {
            try {
                context.shutdownNow();
                log.info("shutdown task context immediately finished");
            } catch (Exception ex) {
                log.warn("shutdown task context immediately failed", ex);
            }
        } finally {
            log.info(context.getProgress().toString());
            log.info(context.getSummary().toHumanReadableFormat());
        }
    }

}
