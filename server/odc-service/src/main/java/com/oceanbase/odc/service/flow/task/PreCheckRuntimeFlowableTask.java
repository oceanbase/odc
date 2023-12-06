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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.VerifyException;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.PreCheckTaskResult;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabasePermissionCheckResult;
import com.oceanbase.odc.service.flow.task.model.SqlCheckTaskResult;
import com.oceanbase.odc.service.flow.task.util.DatabaseChangeFileReader;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.session.util.SchemaExtractor;
import com.oceanbase.odc.service.sqlcheck.SqlCheckService;
import com.oceanbase.odc.service.sqlcheck.model.CheckResult;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.model.ExecutorInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/6/20 15:47
 * @Description: []
 */
@Slf4j
public class PreCheckRuntimeFlowableTask extends BaseODCFlowTaskDelegate<Void> {

    private volatile boolean success = false;
    private volatile SqlCheckTaskResult sqlCheckResult = null;
    private volatile DatabasePermissionCheckResult permissionCheckResult = null;
    private static final String CHECK_RESULT_FILE_NAME = "sql-check-result.json";
    private Long creatorId;
    private List<OffsetString> databaseChangeRelatedSqls;
    private ConnectionConfig connectionConfig;
    private Long preCheckTaskId;
    @Autowired
    private ApprovalFlowConfigSelector approvalFlowConfigSelector;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private DatabaseChangeFileReader databaseChangeFileReader;
    @Autowired
    private SqlCheckService sqlCheckService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) {
        this.serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.EXECUTING);
        this.preCheckTaskId = FlowTaskUtil.getPreCheckTaskId(execution);
        TaskEntity preCheckTaskEntity = taskService.detail(this.preCheckTaskId);
        TaskEntity taskEntity = taskService.detail(taskId);
        if (taskEntity == null) {
            throw new ServiceTaskError(new RuntimeException("Can not find task entity by id " + taskId));
        }
        if (preCheckTaskEntity == null) {
            throw new ServiceTaskError(new RuntimeException("Can not find task entity by id " + preCheckTaskId));
        }
        this.creatorId = FlowTaskUtil.getTaskCreator(execution).getCreatorId();
        try {
            this.connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        } catch (VerifyException e) {
            log.info(e.getMessage());
        }
        RiskLevelDescriber riskLevelDescriber = FlowTaskUtil.getRiskLevelDescriber(execution);
        if (Objects.nonNull(this.connectionConfig)) {
            // Skip SQL pre-check if connection config is null
            this.databaseChangeRelatedSqls = getFlowRelatedSqls(taskEntity.getTaskType(),
                    taskEntity.getParametersJson(), connectionConfig.getDialectType());
            try {
                preCheck(taskEntity, preCheckTaskEntity, riskLevelDescriber);
            } catch (Exception e) {
                log.warn("pre check failed, e");
                throw new ServiceTaskError(e);
            }
            if (this.sqlCheckResult != null) {
                riskLevelDescriber.setSqlCheckResult(sqlCheckResult.getMaxLevel() + "");
            }
        }
        try {
            RiskLevel riskLevel = approvalFlowConfigSelector.select(riskLevelDescriber);
            taskEntity.setRiskLevelId(riskLevel.getId());
            taskEntity.setExecutionExpirationIntervalSeconds(
                    riskLevel.getApprovalFlowConfig().getExecutionExpirationIntervalSeconds());
            taskService.update(taskEntity);
            FlowTaskUtil.setExecutionExpirationInterval(execution,
                    riskLevel.getApprovalFlowConfig().getExecutionExpirationIntervalSeconds(), TimeUnit.SECONDS);
            FlowTaskUtil.setRiskLevel(execution, riskLevel.getLevel());
            success = true;
        } catch (Exception ex) {
            log.warn("risk detect failed, ", ex);
            this.serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.FAILED);
            throw new ServiceTaskError(ex);
        }
        return null;
    }

    @Override
    protected boolean isSuccessful() {
        return success;
    }

    @Override
    protected boolean isFailure() {
        return false;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("RiskLevel Detect task failed, taskId={}", this.preCheckTaskId);
        try {
            taskService.fail(this.preCheckTaskId, 100, buildPreCheckResult());
            this.serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.FAILED);
        } catch (Exception e) {
            log.warn("Failed to store task result", e);
        }
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Risk detect task succeed, taskId={}", this.preCheckTaskId);
        try {
            taskService.succeed(this.preCheckTaskId, buildPreCheckResult());
            this.serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED);
        } catch (Exception e) {
            log.warn("Failed to store task result", e);
        }
        super.onSuccessful(taskId, taskService);
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

    private void preCheck(TaskEntity taskEntity, TaskEntity preCheckTaskEntity, RiskLevelDescriber riskLevelDescriber) {
        TaskType taskType = taskEntity.getTaskType();
        if (taskType.needsPreCheck()) {
            if (taskType == TaskType.ALTER_SCHEDULE) {
                AlterScheduleParameters parameters =
                        JsonUtils.fromJson(taskEntity.getParametersJson(), AlterScheduleParameters.class);
                if (parameters.getType() != JobType.SQL_PLAN) {
                    return;
                }
            }
            doSqlCheck(preCheckTaskEntity, connectionConfig, riskLevelDescriber);
            doDatabasePermissionCheck();
            if (isIntercepted(this.sqlCheckResult, this.permissionCheckResult)) {
                throw new ServiceTaskError(new RuntimeException());
            }
        }
    }

    private boolean isIntercepted(SqlCheckTaskResult sqlCheckResult,
            DatabasePermissionCheckResult permissionCheckResult) {
        if (Objects.isNull(sqlCheckResult) && Objects.isNull(permissionCheckResult)) {
            return false;
        }
        if (Objects.nonNull(sqlCheckResult)) {
            for (CheckResult result : sqlCheckResult.getResults()) {
                for (CheckViolation violation : result.getViolations()) {
                    if (violation.getLevel() > 1) {
                        return true;
                    }
                }
            }
        }
        if (Objects.nonNull(permissionCheckResult)) {
            return CollectionUtils.isNotEmpty(permissionCheckResult.getUnauthorizedDatabaseNames());
        }
        return false;
    }

    private void doDatabasePermissionCheck() {
        Set<String> unauthorizedDatabaseNames =
                databaseService.filterUnAuthorizedDatabaseNames(
                        SchemaExtractor.listSchemaNames(
                                this.databaseChangeRelatedSqls.stream().map(OffsetString::getStr).collect(
                                        Collectors.toList()),
                                this.connectionConfig.getDialectType()),
                        connectionConfig.getId());
        this.permissionCheckResult =
                new DatabasePermissionCheckResult(unauthorizedDatabaseNames);
    }

    private void doSqlCheck(TaskEntity preCheckTaskEntity, ConnectionConfig connectionConfig,
            RiskLevelDescriber describer) {
        if (Objects.isNull(this.databaseChangeRelatedSqls)) {
            return;
        }
        List<CheckViolation> violations = this.sqlCheckService.check(
                Long.valueOf(describer.getEnvironmentId()), describer.getDatabaseName(), this.databaseChangeRelatedSqls,
                connectionConfig);
        this.sqlCheckResult = SqlCheckTaskResult.success(violations);
        try {
            storeTaskResultToFile(preCheckTaskEntity.getId(), this.sqlCheckResult);
            sqlCheckResult.setFileName(CHECK_RESULT_FILE_NAME);
        } catch (Exception e) {
            throw new ServiceTaskError(e);
        }
    }

    private List<OffsetString> getFlowRelatedSqls(TaskType taskType, String parametersJson, DialectType dialectType) {
        if (taskType == TaskType.ASYNC) {
            DatabaseChangeParameters params = JsonUtils.fromJson(parametersJson, DatabaseChangeParameters.class);
            String bucketName = "async".concat(File.separator).concat(this.creatorId.toString());
            try {
                return this.databaseChangeFileReader.loadSqlContents(params,
                        dialectType, bucketName, -1);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if (taskType == TaskType.ONLINE_SCHEMA_CHANGE) {
            OnlineSchemaChangeParameters params =
                    JsonUtils.fromJson(parametersJson, OnlineSchemaChangeParameters.class);
            return SqlUtils.splitWithOffset(dialectType, params.getSqlContent(), params.getDelimiter());
        }
        if (taskType == TaskType.EXPORT_RESULT_SET) {
            ResultSetExportTaskParameter parameters =
                    JsonUtils.fromJson(parametersJson, ResultSetExportTaskParameter.class);
            return SqlUtils.splitWithOffset(dialectType, parameters.getSql(), ";");
        }
        if (taskType == TaskType.ALTER_SCHEDULE) {
            AlterScheduleParameters alterScheduleParameters =
                    JsonUtils.fromJson(parametersJson, AlterScheduleParameters.class);
            if (alterScheduleParameters.getType() != JobType.SQL_PLAN) {
                return null;
            }
            DatabaseChangeParameters databaseChangeParameters =
                    (DatabaseChangeParameters) alterScheduleParameters.getScheduleTaskParameters();
            String bucketName = "async".concat(File.separator).concat(this.creatorId.toString());
            try {
                return this.databaseChangeFileReader.loadSqlContents(databaseChangeParameters,
                        dialectType, bucketName, -1);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    private void storeTaskResultToFile(Long preCheckTaskId, SqlCheckTaskResult result) throws IOException {
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
                OutputStream output = new FileOutputStream(jsonFile)) {
            int len = IOUtils.copy(input, output);
            log.info("Data written successfully, size={}", BinarySizeUnit.B.of(len));
        }
    }

    private PreCheckTaskResult buildPreCheckResult() {
        PreCheckTaskResult result = new PreCheckTaskResult();
        result.setExecutorInfo(new ExecutorInfo(this.hostProperties));
        if (Objects.nonNull(this.sqlCheckResult)) {
            this.sqlCheckResult.setResults(null);
        }
        result.setSqlCheckResult(this.sqlCheckResult);
        result.setPermissionCheckResult(this.permissionCheckResult);
        return result;
    }

}
