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
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.flow.task.model.RollbackPlanTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.base.rollback.RollbackPlanTask;
import com.oceanbase.odc.service.task.base.rollback.RollbackPlanTaskParameters;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/2/6 10:06
 */
@Slf4j
public class RollbackPlanRuntimeFlowableTaskCopied extends BaseODCFlowTaskDelegate<RollbackPlanTaskResult> {

    @Autowired
    private RollbackProperties rollbackProperties;
    @Autowired
    private JobScheduler jobScheduler;
    @Autowired
    private TaskFrameworkService taskFrameworkService;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    private volatile boolean success = false;
    private volatile boolean failure = false;

    @Override
    protected RollbackPlanTaskResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.EXECUTING);
        TaskEntity taskEntity = taskService.detail(taskId);
        Verify.notNull(taskEntity, "taskEntity");
        ConnectionConfig connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        Verify.notNull(connectionConfig, "connectionConfig");
        try {
            JobDefinition jobDefinition = buildJobDefinition(taskEntity, connectionConfig);
            Long jobId = jobScheduler.scheduleJobNow(jobDefinition);
            // Bind generate-rollback-plan job id to database-change task id, so that we can store the result
            // of generate-rollback-plan job to the whole database-change task. And the job id related to the
            // task id will be updated when the database-change job is scheduled.
            taskService.updateJobId(taskId, jobId);
            jobScheduler.await(jobId, rollbackProperties.getMaxTimeoutMillisecond().intValue(), TimeUnit.MILLISECONDS);
            JobEntity jobEntity = taskFrameworkService.find(jobId);
            if (Objects.isNull(jobEntity) || jobEntity.getStatus() != JobStatus.DONE) {
                throw new ServiceTaskError(new RuntimeException("Generate rollback plan task failed"));
            }
            RollbackPlanTaskResult result = JsonUtils.fromJson(jobEntity.getResultJson(), RollbackPlanTaskResult.class);
            DatabaseChangeResult databaseChangeResult = new DatabaseChangeResult();
            databaseChangeResult.setRollbackPlanResult(result);
            taskEntity.setResultJson(JsonUtils.toJson(databaseChangeResult));
            taskService.update(taskEntity);
            serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED);
            success = result.isSuccess();
            return result;
        } catch (Exception e) {
            failure = true;
            try {
                RollbackPlanTaskResult result = RollbackPlanTaskResult.fail(e.getMessage());
                DatabaseChangeResult databaseChangeResult = new DatabaseChangeResult();
                databaseChangeResult.setRollbackPlanResult(result);
                taskEntity.setResultJson(JsonUtils.toJson(databaseChangeResult));
                taskService.update(taskEntity);
                this.serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.FAILED);
            } catch (Exception e1) {
                log.warn("Failed to store rollback plan task result for taskId={}, error message={}", taskId, e1);
            }
            throw e;
        }
    }

    @Override
    protected boolean isSuccessful() {
        return this.success;
    }

    @Override
    protected boolean isFailure() {
        return this.failure;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Generate rollback plan task failed, taskId={}", taskId);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Generate rollback plan task succeed, taskId={}", taskId);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {}

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {}

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    private JobDefinition buildJobDefinition(@NonNull TaskEntity entity, @NonNull ConnectionConfig config) {
        Map<String, String> jobParams = new HashMap<>();
        jobParams.put(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY,
                JobUtils.toJson(buildRollbackPlanTaskParameters(entity, config)));
        jobParams.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS,
                String.valueOf(rollbackProperties.getMaxTimeoutMillisecond()));
        return DefaultJobDefinition.builder()
                .jobClass(RollbackPlanTask.class)
                .jobType(TaskType.GENERATE_ROLLBACK.name())
                .jobParameters(jobParams)
                .build();
    }

    private RollbackPlanTaskParameters buildRollbackPlanTaskParameters(@NonNull TaskEntity entity,
            @NonNull ConnectionConfig config) {
        RollbackPlanTaskParameters rollbackPlanTaskParameters = new RollbackPlanTaskParameters();
        rollbackPlanTaskParameters.setFlowInstanceId(getFlowInstanceId());
        rollbackPlanTaskParameters.setRollbackProperties(copyRollbackPropertiesBean());
        rollbackPlanTaskParameters.setConnectionConfig(config);
        rollbackPlanTaskParameters.setDefaultSchema(entity.getDatabaseName());
        DatabaseChangeParameters p = JsonUtils.fromJson(entity.getParametersJson(), DatabaseChangeParameters.class);
        rollbackPlanTaskParameters.setSqlContent(p.getSqlContent());
        if (CollectionUtils.isNotEmpty(p.getSqlObjectIds())) {
            List<ObjectMetadata> objectMetadatas = new ArrayList<>();
            String bucket = "async".concat(File.separator).concat(String.valueOf(entity.getCreatorId()));
            for (String objectId : p.getSqlObjectIds()) {
                ObjectMetadata metadata = objectStorageFacade.loadMetaData(bucket, objectId);
                objectMetadatas.add(metadata);
            }
            rollbackPlanTaskParameters.setSqlFileObjectMetadatas(objectMetadatas);
        }
        rollbackPlanTaskParameters.setDelimiter(p.getDelimiter());
        return rollbackPlanTaskParameters;
    }

    /**
     * Copy the rollback properties bean in case GSON cannot serialize Spring bean
     * 
     * @return A new rollback properties simple Java object
     */
    private RollbackProperties copyRollbackPropertiesBean() {
        RollbackProperties rollbackProperties = new RollbackProperties();
        rollbackProperties.setEachSqlMaxChangeLines(this.rollbackProperties.getEachSqlMaxChangeLines());
        rollbackProperties.setQueryDataBatchSize(this.rollbackProperties.getQueryDataBatchSize());
        rollbackProperties.setDefaultTimeZone(this.rollbackProperties.getDefaultTimeZone());
        rollbackProperties.setMaxTimeoutMillisecond(this.rollbackProperties.getMaxTimeoutMillisecond());
        rollbackProperties.setMaxRollbackContentSizeBytes(this.rollbackProperties.getMaxRollbackContentSizeBytes());
        rollbackProperties.setTotalMaxChangeLines(this.rollbackProperties.getTotalMaxChangeLines());
        return rollbackProperties;
    }

}
