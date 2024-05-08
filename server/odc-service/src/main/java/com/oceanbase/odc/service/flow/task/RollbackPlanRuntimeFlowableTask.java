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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
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
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeResult;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeSqlContent;
import com.oceanbase.odc.service.flow.task.model.RollbackPlanTaskResult;
import com.oceanbase.odc.service.flow.task.util.DatabaseChangeFileReader;
import com.oceanbase.odc.service.flow.task.util.TaskDownloadUrlsProvider;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.rollbackplan.GenerateRollbackPlan;
import com.oceanbase.odc.service.rollbackplan.RollbackGeneratorFactory;
import com.oceanbase.odc.service.rollbackplan.UnsupportedSqlTypeForRollbackPlanException;
import com.oceanbase.odc.service.rollbackplan.model.RollbackPlan;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.TaskService;

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
    private CloudObjectStorageService cloudObjectStorageService;
    @Autowired
    private ObjectStorageFacade storageFacade;
    private volatile boolean isSuccess = false;
    private String resultFileDownloadUrl;
    private String resultFileId;
    private List<OffsetString> userInputSqls;
    private SqlStatementIterator uploadFileSqlIterator;
    private InputStream uploadFileInputStream;

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
            DatabaseChangeSqlContent sqlContent =
                    DatabaseChangeFileReader.getSqlContent(storageFacade, params, connectionConfig.getDialectType(),
                            "async".concat(File.separator).concat(creator.getId() + ""));
            userInputSqls = sqlContent.getUserInputSqls();
            uploadFileSqlIterator = sqlContent.getUploadFileSqlIterator();
            uploadFileInputStream = sqlContent.getUploadFileInputStream();
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
                int totalMaxChangeLinesLimit = rollbackProperties.getTotalMaxChangeLines();
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
            long totalSizeBytes = rollbackResult.getBytes().length;
            long maxSizeBytes = rollbackProperties.getMaxRollbackContentSizeBytes();
            if (totalSizeBytes > maxSizeBytes) {
                log.warn("Rollback plan result file size exceeds maximum, totalSize={}, taskId={}", totalSizeBytes,
                        getTaskId());
                throw new UnsupportedSqlTypeForRollbackPlanException(
                        "Rollback plan result file size exceeds maximum, totalSize=" + totalSizeBytes
                                + " Byte, max size=" + maxSizeBytes + " Byte");
            }
            try {
                String resultFileDownloadUrl = String.format(
                        "/api/v2/flow/flowInstances/%s/tasks/rollbackPlan/download", getFlowInstanceId());
                String resultFileRootPath = FileManager.generatePath(FileBucket.ROLLBACK_PLAN);
                String resultFileId = StringUtils.uuid();
                String filePath = String.format("%s/%s.sql", resultFileRootPath, resultFileId);
                FileUtils.writeStringToFile(new File(filePath), rollbackResult, StandardCharsets.UTF_8);
                if (Objects.nonNull(cloudObjectStorageService) && cloudObjectStorageService.supported()) {
                    File tempFile = new File(filePath);
                    try {
                        String objectName = cloudObjectStorageService.uploadTemp(resultFileId + ".sql", tempFile);
                        resultFileDownloadUrl = TaskDownloadUrlsProvider
                                .concatBucketAndObjectName(cloudObjectStorageService.getBucketName(), objectName);
                        log.info("Upload generated rollback plan task result file to OSS, file name={}", resultFileId);
                    } finally {
                        OdcFileUtil.deleteFiles(tempFile);
                    }
                }
                this.resultFileId = resultFileId;
                this.resultFileDownloadUrl = resultFileDownloadUrl;
                this.isSuccess = true;
                return RollbackPlanTaskResult.success(resultFileId, resultFileDownloadUrl);
            } catch (Exception e) {
                log.warn("Failed to put generated rollback plan file for taskId={}", getTaskId(), e);
                throw new UnexpectedException("Failed to put generated rollback plan file for taskId=" + getTaskId());
            }
        } else {
            this.isSuccess = true;
            return RollbackPlanTaskResult.skip();
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
        // flow continue if rollback task failed
        super.callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED, null);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Generate rollback plan task succeed, taskId={}", taskId);
        try {
            TaskEntity taskEntity = taskService.detail(taskId);
            RollbackPlanTaskResult result;
            if (this.resultFileDownloadUrl != null && this.resultFileId != null) {
                result = RollbackPlanTaskResult.success(this.resultFileId, this.resultFileDownloadUrl);
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
        super.callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED, null);

    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        // flow continue if rollback task timeout
        super.callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED, null);
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
}
