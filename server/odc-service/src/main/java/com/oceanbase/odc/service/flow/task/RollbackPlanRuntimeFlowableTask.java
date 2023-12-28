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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.RollbackPlanTaskResult;
import com.oceanbase.odc.service.flow.task.util.DatabaseChangeFileReader;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.rollbackplan.GenerateRollbackPlan;
import com.oceanbase.odc.service.rollbackplan.RollbackGeneratorFactory;
import com.oceanbase.odc.service.rollbackplan.UnsupportedSqlTypeForRollbackPlanException;
import com.oceanbase.odc.service.rollbackplan.model.RollbackPlan;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.TaskService;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link RollbackPlanRuntimeFlowableTask}
 *
 * @author jingtian
 * @date 2023/5/18
 * @since ODC_release_4.2.0
 * @see BaseODCFlowTaskDelegate
 */
@Slf4j
public class RollbackPlanRuntimeFlowableTask extends BaseODCFlowTaskDelegate<RollbackPlanTaskResult> {

    @Autowired
    protected ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private RollbackProperties rollbackProperties;
    @Autowired
    private FlowTaskProperties flowTaskProperties;
    @Autowired
    private DatabaseChangeFileReader databaseChangeFileReader;
    @Autowired
    private ObjectStorageFacade objectStorageFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    private volatile boolean isSuccess = false;
    private String objectId;
    private List<OffsetString> userInputSqls;
    private SqlStatementIterator uploadFileSqlIterator;
    private InputStream uploadFileInputStream;

    private static final String ROLLBACK_PLAN_RESULT_FILE_NAME = "rollback-plan-result.sql";

    @Override
    protected RollbackPlanTaskResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        TaskEntity taskEntity = taskService.detail(taskId);
        if (taskEntity == null) {
            throw new IllegalStateException("Can not find task entity by id " + taskId);
        }
        try {
            log.info("Generate rollback plan task starts, taskId={}", taskId);
            long startTimestamp = System.currentTimeMillis();
            Long timeOutMilliSeconds = rollbackProperties.getMaxTimeoutMillisecond();
            this.serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.EXECUTING);
            User creator = FlowTaskUtil.getTaskCreator(execution);
            ConnectionConfig connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
            Verify.notNull(creator, "TaskCreator");
            Verify.notNull(connectionConfig, "ConnectionConfig");
            DatabaseChangeParameters params =
                    JsonUtils.fromJson(taskEntity.getParametersJson(), DatabaseChangeParameters.class);
            String bucketName = "async".concat(File.separator).concat(creator.getId() + "");
            if (StringUtils.isNotBlank(params.getSqlContent())) {
                this.userInputSqls = SqlUtils.splitWithOffset(connectionConfig.getDialectType(), params.getSqlContent(),
                        params.getDelimiter());
            }
            if (CollectionUtils.isNotEmpty(params.getSqlObjectIds())) {
                this.uploadFileInputStream = databaseChangeFileReader.readInputStreamFromSqlObjects(params, bucketName,
                        flowTaskProperties.getMaxRollbackContentSizeBytes());
                if (this.uploadFileInputStream != null) {
                    this.uploadFileSqlIterator = SqlUtils.iterator(connectionConfig.getDialectType(),
                            params.getDelimiter(), this.uploadFileInputStream, StandardCharsets.UTF_8);
                }
            }
            if (CollectionUtils.isEmpty(userInputSqls)
                    && (uploadFileSqlIterator == null || !uploadFileSqlIterator.hasNext())) {
                this.isSuccess = true;
                return RollbackPlanTaskResult.skip();
            }
            ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
            ConnectionSessionUtil.setCurrentSchema(session, FlowTaskUtil.getSchemaName(execution));
            try {
                StringBuilder rollbackPlans = new StringBuilder();
                int totalChangeLineConunt = 0;
                int totalMaxChangeLinesLimit = flowTaskProperties.getTotalMaxChangeLines();
                while (CollectionUtils.isNotEmpty(userInputSqls)
                        || (uploadFileSqlIterator != null && uploadFileSqlIterator.hasNext())) {
                    String sql;
                    if (CollectionUtils.isNotEmpty(userInputSqls)) {
                        sql = userInputSqls.remove(0).getStr();
                    } else {
                        sql = uploadFileSqlIterator.next().getStr();
                    }
                    try {
                        long timeoutForCurrentSql = timeOutMilliSeconds - (System.currentTimeMillis() - startTimestamp);
                        if (timeoutForCurrentSql <= 0) {
                            log.warn("Generate rollback plan task has timeout, timeout milliseconds={}, taskId={}",
                                    timeOutMilliSeconds, taskId);
                            rollbackPlans.append("/* Generate rollback plan task has timeout, timeout milliseconds=")
                                    .append(timeOutMilliSeconds)
                                    .append(", generate rollback plan will be stopped. */\n");
                            return handleRollbackResult(rollbackPlans.toString());
                        }
                        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql, rollbackProperties,
                                session, timeoutForCurrentSql);
                        RollbackPlan result = rollbackPlan.generate();
                        totalChangeLineConunt += result.getChangeLineCount();
                        rollbackPlans.append(result.toString());
                        if (totalChangeLineConunt > totalMaxChangeLinesLimit) {
                            log.info(
                                    "The number of changed lines for taskId={} exceeds the maximum limit, changed line count={}",
                                    taskId, totalChangeLineConunt);
                            rollbackPlans.append("/* The number of changed lines exceeds the maximum limit:")
                                    .append(totalMaxChangeLinesLimit)
                                    .append(", generate rollback plan will be stopped. */\n");
                            return handleRollbackResult(rollbackPlans.toString());
                        }
                    } catch (UnsupportedSqlTypeForRollbackPlanException unsupportedSqlTypeException) {
                        log.info(unsupportedSqlTypeException.getMessage());
                    } catch (Exception e) {
                        // Continue to generate rollback plan for the next sql
                        log.warn(
                                "Failed to generate rollback plan for sql:{}, error message:{}", sql, e.getMessage());
                    }
                }
                return handleRollbackResult(rollbackPlans.toString());
            } finally {
                session.expire();
            }
        } catch (Exception e) {
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
        } finally {
            if (uploadFileInputStream != null) {
                try {
                    uploadFileInputStream.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private RollbackPlanTaskResult handleRollbackResult(String rollbackResult) {
        if (StringUtils.isNotBlank(rollbackResult)) {
            String objectId =
                    putRollbackPlan(rollbackResult, flowTaskProperties.getMaxRollbackContentSizeBytes());
            this.objectId = objectId;
            this.isSuccess = true;
            return RollbackPlanTaskResult.success(this.objectId);
        } else {
            this.isSuccess = true;
            return RollbackPlanTaskResult.skip();
        }
    }

    private String putRollbackPlan(@NonNull String rollbackPlans, @NonNull long maxSizeBytes) {
        long totalSize = rollbackPlans.getBytes().length;
        if (totalSize > maxSizeBytes) {
            log.warn("Rollback plan result file size exceeds maximum, totalSize={}, taskId={}", totalSize, getTaskId());
            throw new UnsupportedSqlTypeForRollbackPlanException(
                    "Rollback plan result file size exceeds maximum, totalSize="
                            + totalSize + " Byte, max size=" + maxSizeBytes + " Byte");
        }
        try {
            String bucketName = "async".concat(File.separator).concat(authenticationFacade.currentUserIdStr());
            objectStorageFacade.createBucketIfNotExists(bucketName);
            InputStream inputStream = new ByteArrayInputStream(rollbackPlans.getBytes());
            ObjectMetadata metadata =
                    objectStorageFacade.putObject(bucketName, ROLLBACK_PLAN_RESULT_FILE_NAME, totalSize, inputStream);
            return metadata.getObjectId();
        } catch (Exception e) {
            log.warn("Failed to put generated rollback plan file for taskId={}", getTaskId());
            throw new UnexpectedException("Failed to put generated rollback plan file for taskId=" + getTaskId());
        }
    }

    @Override
    protected boolean isSuccessful() {
        return this.isSuccess;
    }

    @Override
    protected boolean isFailure() {
        return false;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Generate rollback plan task failed, taskId={}", taskId);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Generate rollback plan task succeed, taskId={}", taskId);
        try {
            TaskEntity taskEntity = taskService.detail(taskId);
            RollbackPlanTaskResult result;
            if (this.objectId != null) {
                result = RollbackPlanTaskResult.success(this.objectId);
            } else {
                result = RollbackPlanTaskResult.skip();
            }
            DatabaseChangeResult databaseChangeResult = new DatabaseChangeResult();
            databaseChangeResult.setRollbackPlanResult(result);
            taskEntity.setResultJson(JsonUtils.toJson(databaseChangeResult));
            taskService.update(taskEntity);
            this.serviceTaskRepository.updateStatusById(getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED);
        } catch (Exception e) {
            log.warn("Failed to store generate rollback plan task result", e);
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
}
