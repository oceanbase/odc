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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.VerifyException;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.database.model.DBResource;
import com.oceanbase.odc.service.connection.database.model.UnauthorizedDBResource;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.PreCheckTaskResult;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabasePermissionCheckResult;
import com.oceanbase.odc.service.flow.task.model.PreCheckTaskProperties;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;
import com.oceanbase.odc.service.flow.task.model.SqlCheckTaskResult;
import com.oceanbase.odc.service.flow.task.util.DatabaseChangeFileReader;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.table.TablePermissionService;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.resultset.ResultSetExportTaskParameter;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor.DBSchemaIdentity;
import com.oceanbase.odc.service.sqlcheck.SqlCheckService;
import com.oceanbase.odc.service.sqlcheck.model.CheckResult;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/6/20 15:47
 * @Description: []
 */
@Slf4j
public class PreCheckRuntimeFlowableTask extends BaseODCFlowTaskDelegate<Void> {

    private volatile boolean success = false;
    private volatile boolean overLimit = false;
    private volatile SqlCheckTaskResult sqlCheckResult = null;
    private volatile DatabasePermissionCheckResult permissionCheckResult = null;
    private Long creatorId;
    private List<OffsetString> userInputSqls;
    private InputStream uploadFileInputStream;
    private SqlStatementIterator uploadFileSqlIterator;
    private ConnectionConfig connectionConfig;
    private Long preCheckTaskId;
    @Autowired
    private ApprovalFlowConfigSelector approvalFlowConfigSelector;
    @Autowired
    private DBResourcePermissionHelper dbResourcePermissionHelper;
    @Autowired
    private SqlCheckService sqlCheckService;
    @Autowired
    private PreCheckTaskProperties preCheckTaskProperties;
    @Autowired
    private ObjectStorageFacade storageFacade;

    @Autowired
    private TablePermissionService tablePermissionService;

    private static final String CHECK_RESULT_FILE_NAME = "sql-check-result.json";
    private final Map<String, Object> riskLevelResult = new HashMap<>();

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) throws Exception {
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
        this.creatorId = FlowTaskUtil.getTaskCreator(execution).getId();
        try {
            this.connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        } catch (VerifyException e) {
            log.info(e.getMessage());
        }
        RiskLevelDescriber riskLevelDescriber = FlowTaskUtil.getRiskLevelDescriber(execution);
        if (Objects.nonNull(this.connectionConfig)) {
            // Skip SQL pre-check if connection config is null
            loadUserInputSqlContent(taskEntity.getTaskType(), taskEntity.getParametersJson());
            loadUploadFileInputStream(taskEntity.getTaskType(), taskEntity.getParametersJson());
            try {
                preCheck(taskEntity, preCheckTaskEntity, riskLevelDescriber);
            } catch (Exception e) {
                log.warn("pre check failed, e");
                throw new ServiceTaskError(e);
            } finally {
                if (Objects.nonNull(this.uploadFileInputStream)) {
                    try {
                        this.uploadFileInputStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            if (this.sqlCheckResult != null) {
                riskLevelDescriber.setSqlCheckResult(sqlCheckResult.getMaxLevel() + "");
            }
            if (this.overLimit) {
                riskLevelDescriber.setOverLimit(true);
            }
        }
        try {
            RiskLevel riskLevel = approvalFlowConfigSelector.select(riskLevelDescriber);
            taskEntity.setRiskLevelId(riskLevel.getId());
            taskEntity.setExecutionExpirationIntervalSeconds(
                    riskLevel.getApprovalFlowConfig().getExecutionExpirationIntervalSeconds());
            taskService.update(taskEntity);

            Integer executionExpirationSeconds = riskLevel.getApprovalFlowConfig()
                    .getExecutionExpirationIntervalSeconds();
            PreConditions.notNegative(executionExpirationSeconds, "ExecutionExpirationSeconds");
            long executionExpirationIntervalMilliSecs =
                    TimeUnit.MILLISECONDS.convert(executionExpirationSeconds, TimeUnit.SECONDS);
            riskLevelResult.put(RuntimeTaskConstants.TIMEOUT_MILLI_SECONDS, executionExpirationIntervalMilliSecs);
            riskLevelResult.put(RuntimeTaskConstants.RISKLEVEL, riskLevel.getLevel());
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
        super.callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED, riskLevelResult);

    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        super.callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.EXPIRED, null);
    }

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
            // doSqlCheckAndDatabasePermissionCheck(preCheckTaskEntity, riskLevelDescriber, taskType);
            doSqlCheckAndResourcePermissionCheck(preCheckTaskEntity, riskLevelDescriber, taskType);
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
            return CollectionUtils.isNotEmpty(permissionCheckResult.getUnauthorizedDBResources());
        }
        return false;
    }

    private void doSqlCheckAndResourcePermissionCheck(TaskEntity preCheckTaskEntity, RiskLevelDescriber describer,
            TaskType taskType) {
        List<OffsetString> sqls = new ArrayList<>();
        this.overLimit = getSqlContentUntilOverLimit(sqls, preCheckTaskProperties.getMaxSqlContentBytes());
        List<UnauthorizedDBResource> unauthorizedDBResource = new ArrayList<>();
        List<CheckViolation> violations = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(sqls)) {
            violations.addAll(this.sqlCheckService.check(Long.valueOf(describer.getEnvironmentId()),
                    describer.getDatabaseName(), sqls, connectionConfig));
            Map<DBSchemaIdentity, Set<SqlType>> identity2Types = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                    sqls.stream().map(e -> SqlTuple.newTuple(e.getStr())).collect(Collectors.toList()),
                    this.connectionConfig.getDialectType(), preCheckTaskEntity.getDatabaseName());
            Map<DBResource, Set<DatabasePermissionType>> resource2PermissionTypes = new HashMap<>();
            for (Entry<DBSchemaIdentity, Set<SqlType>> entry : identity2Types.entrySet()) {
                DBSchemaIdentity identity = entry.getKey();
                Set<SqlType> sqlTypes = entry.getValue();
                if (CollectionUtils.isNotEmpty(sqlTypes)) {
                    Set<DatabasePermissionType> permissionTypes = sqlTypes.stream().map(DatabasePermissionType::from)
                            .filter(Objects::nonNull).collect(Collectors.toSet());
                    permissionTypes.addAll(DatabasePermissionType.from(taskType));
                    if (CollectionUtils.isNotEmpty(permissionTypes)) {
                        resource2PermissionTypes.put(
                                DBResource.from(connectionConfig, identity.getSchema(), identity.getTable()),
                                permissionTypes);
                    }
                }
            }
            unauthorizedDBResource =
                    dbResourcePermissionHelper.filterUnauthorizedDBResources(resource2PermissionTypes, false);
        }
        this.permissionCheckResult = new DatabasePermissionCheckResult(unauthorizedDBResource);
        this.sqlCheckResult = SqlCheckTaskResult.success(violations);
        try {
            storeTaskResultToFile(preCheckTaskEntity.getId(), this.sqlCheckResult);
            sqlCheckResult.setFileName(CHECK_RESULT_FILE_NAME);
        } catch (Exception e) {
            throw new ServiceTaskError(e);
        }
    }

    /**
     * Get the sql content from the databaseChangeRelatedSqls and sqlIterator, and put them into the
     * sqlBuffer. If the sql content is over the maxSqlBytes, return true, else return false.
     *
     * @param sqlBuffer the buffer to store the sql content
     * @param maxSqlBytes the max sql content bytes
     * @return true if the sql content is over the maxSqlBytes, else return false
     */
    boolean getSqlContentUntilOverLimit(@NonNull List<OffsetString> sqlBuffer, long maxSqlBytes) {
        long curSqlBytes = 0;
        if (CollectionUtils.isNotEmpty(userInputSqls)) {
            for (OffsetString sql : userInputSqls) {
                int sqlBytes = sql.getStr().getBytes(StandardCharsets.UTF_8).length;
                if (curSqlBytes + sqlBytes > maxSqlBytes) {
                    return true;
                }
                curSqlBytes += sqlBytes;
                sqlBuffer.add(sql);
            }
        }
        if (Objects.nonNull(this.uploadFileSqlIterator)) {
            while (this.uploadFileSqlIterator.hasNext()) {
                String sql = this.uploadFileSqlIterator.next().getStr();
                int sqlBytes = sql.getBytes(StandardCharsets.UTF_8).length;
                if (curSqlBytes + sqlBytes > maxSqlBytes) {
                    return true;
                }
                curSqlBytes += sqlBytes;
                sqlBuffer.add(new OffsetString(0, sql));
            }
        }
        return false;
    }

    private void loadUserInputSqlContent(TaskType taskType, String parameter) {
        String sqlContent = null;
        String delimiter = ";";
        if (taskType == TaskType.ASYNC) {
            DatabaseChangeParameters params = JsonUtils.fromJson(parameter, DatabaseChangeParameters.class);
            sqlContent = params.getSqlContent();
            delimiter = params.getDelimiter();
        } else if (taskType == TaskType.ONLINE_SCHEMA_CHANGE) {
            OnlineSchemaChangeParameters params = JsonUtils.fromJson(parameter, OnlineSchemaChangeParameters.class);
            sqlContent = params.getSqlContent();
            delimiter = params.getDelimiter();
        } else if (taskType == TaskType.EXPORT_RESULT_SET) {
            ResultSetExportTaskParameter params = JsonUtils.fromJson(parameter, ResultSetExportTaskParameter.class);
            sqlContent = params.getSql();
        } else if (taskType == TaskType.ALTER_SCHEDULE) {
            AlterScheduleParameters params = JsonUtils.fromJson(parameter, AlterScheduleParameters.class);
            if (params.getType() != JobType.SQL_PLAN) {
                return;
            }
            DatabaseChangeParameters dcParams = (DatabaseChangeParameters) params.getScheduleTaskParameters();
            sqlContent = dcParams.getSqlContent();
            delimiter = dcParams.getDelimiter();
        }
        if (StringUtils.isNotBlank(sqlContent)) {
            this.userInputSqls = SqlUtils.splitWithOffset(connectionConfig.getDialectType(), sqlContent, delimiter);
        }
    }

    private void loadUploadFileInputStream(TaskType taskType, String parametersJson) {
        String bucketName = "async".concat(File.separator).concat(this.creatorId.toString());
        DatabaseChangeParameters params = null;
        if (taskType == TaskType.ASYNC) {
            params = JsonUtils.fromJson(parametersJson, DatabaseChangeParameters.class);
        } else if (taskType == TaskType.ALTER_SCHEDULE) {
            AlterScheduleParameters asParams = JsonUtils.fromJson(parametersJson, AlterScheduleParameters.class);
            if (asParams.getType() != JobType.SQL_PLAN) {
                return;
            }
            params = (DatabaseChangeParameters) asParams.getScheduleTaskParameters();
        }
        if (Objects.nonNull(params)) {
            this.uploadFileInputStream =
                    DatabaseChangeFileReader.readInputStreamFromSqlObjects(storageFacade, params, bucketName, -1);
            if (Objects.nonNull(this.uploadFileInputStream)) {
                this.uploadFileSqlIterator = SqlUtils.iterator(connectionConfig.getDialectType(), params.getDelimiter(),
                        this.uploadFileInputStream, StandardCharsets.UTF_8);
            }
        }
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
        result.setOverLimit(this.overLimit);
        if (Objects.nonNull(this.sqlCheckResult)) {
            this.sqlCheckResult.setResults(null);
        }
        result.setSqlCheckResult(this.sqlCheckResult);
        result.setPermissionCheckResult(this.permissionCheckResult);
        return result;
    }

}
