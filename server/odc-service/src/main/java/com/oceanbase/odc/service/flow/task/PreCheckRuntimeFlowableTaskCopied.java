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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.VerifyException;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.PreCheckTaskResult;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabasePermissionCheckResult;
import com.oceanbase.odc.service.flow.task.model.PreCheckTaskProperties;
import com.oceanbase.odc.service.flow.task.model.SqlCheckTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.sqlcheck.model.CheckResult;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.runtime.PreCheckTask;
import com.oceanbase.odc.service.task.runtime.PreCheckTaskParameters;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/1/30 11:06
 */
@Slf4j
public class PreCheckRuntimeFlowableTaskCopied extends BaseODCFlowTaskDelegate<Void> {

    @Autowired
    private PreCheckTaskProperties preCheckTaskProperties;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private ApprovalFlowConfigSelector approvalFlowConfigSelector;
    @Autowired
    private TaskFrameworkService taskFrameworkService;
    @Autowired
    private JobScheduler jobScheduler;

    private Long preCheckTaskId;
    private PreCheckTaskResult preCheckResult;
    private volatile boolean success = false;

    private static final String CHECK_RESULT_FILE_NAME = "sql-check-result.json";

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) throws Exception {
        serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.EXECUTING);
        this.preCheckTaskId = FlowTaskUtil.getPreCheckTaskId(execution);
        RiskLevelDescriber riskLevelDescriber = FlowTaskUtil.getRiskLevelDescriber(execution);
        TaskEntity taskEntity;
        ConnectionConfig connectionConfig = null;
        try {
            taskEntity = taskService.detail(taskId);
        } catch (Exception e) {
            throw new ServiceTaskError(e);
        }
        try {
            connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        } catch (VerifyException e) {
            log.info(e.getMessage());
        }
        if (Objects.nonNull(connectionConfig) && needCreatePreCheckTask(taskEntity)) {
            // Skip SQL pre-check if connection config is null
            JobDefinition jobDefinition = buildJobDefinition(taskEntity, connectionConfig, riskLevelDescriber);
            Long jobId = jobScheduler.scheduleJobNow(jobDefinition);
            taskService.updateJobId(this.preCheckTaskId, jobId);
            try {
                jobScheduler.await(jobId, preCheckTaskProperties.getExecutionTimeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("Exception occurred while waiting for pre-check task to complete", e);
            }
            JobEntity jobEntity = taskFrameworkService.find(jobId);
            if (jobEntity.getStatus() != JobStatus.DONE) {
                throw new ServiceTaskError(new RuntimeException("Pre-check task failed"));
            }
            this.preCheckResult = JsonUtils.fromJson(jobEntity.getResultJson(), PreCheckTaskResult.class);
            if (Objects.nonNull(this.preCheckResult)) {
                this.preCheckResult.setExecutorInfo(new ExecutorInfo(this.hostProperties));
                storeTaskResultToFile(this.preCheckResult.getSqlCheckResult());
                this.preCheckResult.getSqlCheckResult().setFileName(CHECK_RESULT_FILE_NAME);
                if (isIntercepted(this.preCheckResult)) {
                    throw new ServiceTaskError(new RuntimeException());
                }
                riskLevelDescriber
                        .setSqlCheckResult(String.valueOf(this.preCheckResult.getSqlCheckResult().getMaxLevel()));
                riskLevelDescriber.setOverLimit(this.preCheckResult.isOverLimit());
            }
        }
        try {
            RiskLevel riskLevel = approvalFlowConfigSelector.select(riskLevelDescriber);
            Integer intervalSeconds = riskLevel.getApprovalFlowConfig().getExecutionExpirationIntervalSeconds();
            taskEntity.setRiskLevelId(riskLevel.getId());
            taskEntity.setExecutionExpirationIntervalSeconds(intervalSeconds);
            taskService.update(taskEntity);
            FlowTaskUtil.setExecutionExpirationInterval(execution, intervalSeconds, TimeUnit.SECONDS);
            FlowTaskUtil.setRiskLevel(execution, riskLevel.getLevel());
            this.success = true;
        } catch (Exception e) {
            log.warn("Failed to detect risk level", e);
            serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.FAILED);
            throw new ServiceTaskError(e);
        }
        return null;
    }

    @Override
    protected boolean isSuccessful() {
        return this.success;
    }

    @Override
    protected boolean isFailure() {
        return false;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Pre-check task failed, preCheckTaskId={}", this.preCheckTaskId);
        try {
            taskService.fail(this.preCheckTaskId, 100, this.preCheckResult);
            serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.FAILED);
        } catch (Exception e) {
            log.warn("Failed to store task result", e);
        }
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Pre-check task succeed, preCheckTaskId={}", this.preCheckTaskId);
        try {
            taskService.succeed(this.preCheckTaskId, this.preCheckResult);
            this.serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED);
        } catch (Exception e) {
            log.warn("Failed to store task result", e);
        }
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

    private JobDefinition buildJobDefinition(@NonNull TaskEntity taskEntity, @NonNull ConnectionConfig connectionConfig,
            @NonNull RiskLevelDescriber riskLevelDescriber) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY,
                JobUtils.toJson(buildPreCheckTaskParameters(taskEntity, connectionConfig, riskLevelDescriber)));
        parameters.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS,
                preCheckTaskProperties.getExecutionTimeoutMillis() + "");
        return DefaultJobDefinition.builder()
                .jobClass(PreCheckTask.class)
                .jobType(TaskType.PRE_CHECK.name())
                .jobParameters(parameters)
                .build();
    }

    private PreCheckTaskParameters buildPreCheckTaskParameters(@NonNull TaskEntity taskEntity,
            @NonNull ConnectionConfig connectionConfig, @NonNull RiskLevelDescriber riskLevelDescriber) {
        PreCheckTaskParameters parameters = new PreCheckTaskParameters();
        Environment environment = environmentService.detail(Long.valueOf(riskLevelDescriber.getEnvironmentId()));
        parameters.setMaxReadContentBytes(preCheckTaskProperties.getMaxSqlContentBytes());
        parameters.setTaskType(taskEntity.getTaskType());
        parameters.setParameterJson(taskEntity.getParametersJson());
        parameters.setRiskLevelDescriber(riskLevelDescriber);
        parameters.setRules(ruleService.list(environment.getRulesetId(), QueryRuleMetadataParams.builder().build()));
        parameters.setConnectionConfig(connectionConfig);
        parameters.setDefaultSchema(taskEntity.getDatabaseName());
        parameters.setAuthorizedDatabase(databaseService.getAllAuthorizedDatabases(connectionConfig.getId()));
        parameters.setSqlFileObjectMetadatas(getSqlFileObjectMetadatas(taskEntity));
        return parameters;
    }

    private List<ObjectMetadata> getSqlFileObjectMetadatas(@NonNull TaskEntity taskEntity) {
        TaskType taskType = taskEntity.getTaskType();
        String parameterJson = taskEntity.getParametersJson();
        DatabaseChangeParameters params = null;
        if (taskType == TaskType.ASYNC) {
            params = JsonUtils.fromJson(parameterJson, DatabaseChangeParameters.class);
        } else if (taskType == TaskType.ALTER_SCHEDULE) {
            AlterScheduleParameters asParams = JsonUtils.fromJson(parameterJson, AlterScheduleParameters.class);
            if (asParams.getType() == JobType.SQL_PLAN) {
                params = (DatabaseChangeParameters) asParams.getScheduleTaskParameters();
            }
        }
        if (Objects.nonNull(params) && CollectionUtils.isNotEmpty(params.getSqlObjectIds())) {
            List<ObjectMetadata> objectMetadatas = new ArrayList<>();
            String bucket = "async".concat(File.separator).concat(String.valueOf(taskEntity.getCreatorId()));
            for (String objectId : params.getSqlObjectIds()) {
                ObjectMetadata metadata = objectStorageFacade.loadMetaData(bucket, objectId);
                objectMetadatas.add(metadata);
            }
            return objectMetadatas;
        }
        return null;
    }

    private boolean isIntercepted(@NonNull PreCheckTaskResult result) {
        SqlCheckTaskResult sqlCheckResult = result.getSqlCheckResult();
        DatabasePermissionCheckResult permissionCheckResult = result.getPermissionCheckResult();
        if (Objects.isNull(sqlCheckResult) && Objects.isNull(permissionCheckResult)) {
            return false;
        }
        if (Objects.nonNull(sqlCheckResult)) {
            for (CheckResult r : sqlCheckResult.getResults()) {
                for (CheckViolation violation : r.getViolations()) {
                    if (violation.getLevel() > 1) {
                        return true;
                    }
                }
            }
        }
        if (Objects.nonNull(permissionCheckResult)) {
            return CollectionUtils.isNotEmpty(permissionCheckResult.getUnauthorizedDBResources());
        }
        return false;
    }

    private void storeTaskResultToFile(SqlCheckTaskResult result) throws IOException {
        String json = JsonUtils.toJson(result);
        if (json == null) {
            throw new IllegalStateException("Can not get json string");
        }
        String dir = FileManager.generateDir(FileBucket.PRE_CHECK) + File.separator + preCheckTaskId;
        FileUtils.forceMkdir(new File(dir));
        File jsonFile = new File(dir + File.separator + CHECK_RESULT_FILE_NAME);
        if (!jsonFile.exists() && !jsonFile.createNewFile()) {
            throw new IllegalStateException("Can not create a new file, " + jsonFile.getAbsolutePath());
        }
        try (InputStream input = new ByteArrayInputStream(json.getBytes());
                OutputStream output = Files.newOutputStream(jsonFile.toPath())) {
            int len = IOUtils.copy(input, output);
            log.info("Data written successfully, size={}", BinarySizeUnit.B.of(len));
        }
    }

    private boolean needCreatePreCheckTask(TaskEntity taskEntity) {
        TaskType taskType = taskEntity.getTaskType();
        if (!taskType.needsPreCheck()) {
            return false;
        }
        if (taskType == TaskType.ALTER_SCHEDULE) {
            AlterScheduleParameters params =
                    JsonUtils.fromJson(taskEntity.getParametersJson(), AlterScheduleParameters.class);
            return params.getType() == JobType.SQL_PLAN;
        }
        return true;
    }

}
