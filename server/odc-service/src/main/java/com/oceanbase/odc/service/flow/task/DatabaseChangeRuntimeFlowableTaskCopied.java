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
package com.oceanbase.odc.service.flow.task;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.model.PreCheckTaskResult;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.RollbackPlanTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.runtime.DatabaseChangeTask;
import com.oceanbase.odc.service.task.runtime.DatabaseChangeTaskParameters;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatabaseChangeRuntimeFlowableTaskCopied extends BaseODCFlowTaskDelegate<DatabaseChangeResult> {

    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private ConnectProperties connectProperties;
    @Autowired
    private JobScheduler jobScheduler;
    @Autowired
    private TaskFrameworkService taskFrameworkService;
    @Autowired
    private TaskFrameworkProperties taskFrameworkProperties;
    @Autowired
    private DataMaskingService dataMaskingService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FlowTaskProperties flowTaskProperties;

    private volatile Long jobId;
    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;
    private volatile boolean isCanceled = false;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        try {
            jobScheduler.cancelJob(this.jobId);
            try {
                jobScheduler.await(this.jobId, taskFrameworkProperties.getJobCancelTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Exception occurs while waiting job cancelled.", e);
            }
            JobEntity recentJob = taskFrameworkService.find(this.jobId);
            if (recentJob.getStatus() == JobStatus.CANCELED) {
                taskService.cancel(taskId);
                isCanceled = true;
                return true;
            } else {
                return false;
            }
        } catch (JobException e) {
            log.warn("Cancel job failed.", e);
            return false;
        }
    }

    @Override
    public boolean isCancelled() {
        return isCanceled;
    }

    @Override
    protected DatabaseChangeResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        DatabaseChangeResult result;
        try {
            log.info("Database change task starts, taskId={}, activityId={}", taskId, execution.getCurrentActivityId());
            taskService.start(taskId);
            TaskEntity taskEntity = taskService.detail(taskId);
            Verify.notNull(taskEntity, "taskEntity");
            RollbackPlanTaskResult rollbackPlanTaskResult = null;
            result = JsonUtils.fromJson(taskEntity.getResultJson(), DatabaseChangeResult.class);
            if (result != null) {
                rollbackPlanTaskResult = result.getRollbackPlanResult();
            }

            JobDefinition jobDefinition = buildJobDefinition(execution, taskEntity);
            this.jobId = jobScheduler.scheduleJobNow(jobDefinition);
            taskService.updateJobId(taskId, jobId);
            log.info("Database change task is scheduled, taskId={}, jobId={}", taskId, this.jobId);
            jobScheduler.await(this.jobId, FlowTaskUtil.getExecutionExpirationIntervalMillis(execution).intValue(),
                    TimeUnit.MILLISECONDS);

            JobEntity jobEntity = taskFrameworkService.find(this.jobId);
            result = JsonUtils.fromJson(jobEntity.getResultJson(), DatabaseChangeResult.class);
            result.setRollbackPlanResult(rollbackPlanTaskResult);
            if (jobEntity.getStatus() == JobStatus.DONE) {
                isSuccessful = true;
                taskService.succeed(taskId, result);
            } else {
                if (jobEntity.getStatus() == JobStatus.CANCELED) {
                    isCanceled = true;
                } else {
                    isFailure = true;
                }
                taskService.fail(taskId, jobEntity.getProgressPercentage(), result);
            }
            log.info("Database change task ends, taskId={}, jobId={}, jobStatus={}", taskId, this.jobId,
                    jobEntity.getStatus());
        } catch (Exception e) {
            log.error("Error occurs while database change task executing", e);
            if (e instanceof BaseFlowException) {
                throw e;
            } else {
                throw new ServiceTaskError(e);
            }
        }
        return result;
    }

    @Override
    protected boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    protected boolean isFailure() {
        return isFailure;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Database change task failed, taskId={}", taskId);
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Database change task succeed, taskId={}", taskId);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Database change task timeout, taskId={}", taskId);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {

    }

    private JobDefinition buildJobDefinition(DelegateExecution execution, TaskEntity taskEntity) {
        Map<String, String> jobParameters = new HashMap<>();
        DatabaseChangeTaskParameters taskParameters = new DatabaseChangeTaskParameters();
        DatabaseChangeParameters p = FlowTaskUtil.getAsyncParameter(execution);
        taskParameters.setParameterJson(JsonUtils.toJson(p));
        ConnectionConfig config = FlowTaskUtil.getConnectionConfig(execution);
        config.setDefaultSchema(FlowTaskUtil.getSchemaName(execution));
        taskParameters.setConnectionConfig(config);
        taskParameters.setFlowInstanceId(FlowTaskUtil.getFlowInstanceId(execution));
        taskParameters.setSessionTimeZone(connectProperties.getDefaultTimeZone());
        if (CollectionUtils.isNotEmpty(p.getSqlObjectIds())) {
            List<ObjectMetadata> objectMetadatas = new ArrayList<>();
            Long creatorId = FlowTaskUtil.getTaskCreator(execution).getId();
            for (String objectId : p.getSqlObjectIds()) {
                objectMetadatas.add(objectStorageFacade
                        .loadMetaData("async".concat(File.separator).concat(String.valueOf(creatorId)), objectId));
            }
            taskParameters.setSqlFileObjectMetadatas(objectMetadatas);
        }
        taskParameters.setNeedDataMasking(dataMaskingService.isMaskingEnabled());
        modifyTimeoutIfTimeConsumingSqlExists(execution, p, taskParameters, taskEntity);
        jobParameters.put(JobParametersKeyConstants.FLOW_INSTANCE_ID, getFlowInstanceId().toString());
        jobParameters.put(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY, JobUtils.toJson(taskParameters));
        jobParameters.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS, p.getTimeoutMillis() + "");
        return DefaultJobDefinition.builder().jobClass(DatabaseChangeTask.class)
                .jobType(TaskType.ASYNC.name())
                .jobParameters(jobParameters)
                .build();
    }

    private void modifyTimeoutIfTimeConsumingSqlExists(DelegateExecution execution, DatabaseChangeParameters parameters,
            DatabaseChangeTaskParameters taskParameters, TaskEntity taskEntity) {
        if (!parameters.isModifyTimeoutIfTimeConsumingSqlExists()) {
            taskParameters.setAutoModifyTimeout(false);
            return;
        }
        Long preCheckTaskId = FlowTaskUtil.getPreCheckTaskId(execution);
        TaskEntity preCheckTask = taskService.detail(preCheckTaskId);

        PreCheckTaskResult preCheckResult =
                JsonUtils.fromJson(preCheckTask.getResultJson(), PreCheckTaskResult.class);
        Validate.notNull(preCheckResult, "Pre check task result can not be null");
        long autoModifiedTimeout = flowTaskProperties.getIndexChangeMaxTimeoutMillisecond();

        if (Objects.nonNull(preCheckResult.getSqlCheckResult())
                && preCheckResult.getSqlCheckResult().isTimeConsumingSqlExists()
                && autoModifiedTimeout > parameters.getTimeoutMillis()) {
            parameters.setTimeoutMillis(autoModifiedTimeout);
            taskParameters.setAutoModifyTimeout(true);
            taskEntity.setParametersJson(JsonUtils.toJson(parameters));
            taskService.updateParametersJson(taskEntity);
        } else {
            taskParameters.setAutoModifyTimeout(false);
        }
    }

}
