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
package com.oceanbase.odc.service.task.base.rollback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.task.model.RollbackPlanTaskResult;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.objectstorage.util.ObjectStorageUtils;
import com.oceanbase.odc.service.rollbackplan.GenerateRollbackPlan;
import com.oceanbase.odc.service.rollbackplan.RollbackGeneratorFactory;
import com.oceanbase.odc.service.rollbackplan.UnsupportedSqlTypeForRollbackPlanException;
import com.oceanbase.odc.service.rollbackplan.model.RollbackPlan;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.base.TaskBase;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/2/6 11:03
 */
@Slf4j
public class RollbackPlanTask extends TaskBase<FlowTaskResult> {

    private RollbackPlanTaskParameters parameters;
    private List<OffsetString> userInputSqls;
    private InputStream uploadFileInputStream;
    private SqlStatementIterator uploadFileSqlIterator;
    private RollbackPlanTaskResult rollbackPlanTaskResult;
    private long taskId;
    private volatile boolean success = false;
    private volatile boolean aborted = false;

    public RollbackPlanTask() {}

    @Override
    protected void doInit(JobContext context) throws Exception {
        this.taskId = getJobContext().getJobIdentity().getId();
        log.info("Initiating generate-rollback-plan task, taskId={}", taskId);
        this.parameters =
                JobUtils.fromJson(jobContext.getJobParameters().get(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY),
                        RollbackPlanTaskParameters.class);
        log.info("Load generate-rollback-plan task parameters successfully, taskId={}", taskId);
        loadUserInputSqlContent();
        loadUploadFileInputStream();
        log.info("Load sql content successfully, taskId={}", taskId);
    }

    public boolean start() throws Exception {
        try {
            long startTimeMills = System.currentTimeMillis();
            ConnectionConfig connectionConfig = parameters.getConnectionConfig();
            Verify.notNull(connectionConfig, "connectionConfig");
            if (CollectionUtils.isEmpty(userInputSqls)
                    && (uploadFileSqlIterator == null || !uploadFileSqlIterator.hasNext())) {
                log.info("No sql content to execute, taskId={}", taskId);
                this.success = true;
                this.rollbackPlanTaskResult = RollbackPlanTaskResult.skip();
                return this.success;
            }
            ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
            ConnectionSessionUtil.setCurrentSchema(session, parameters.getDefaultSchema());
            try {
                StringBuilder rollbackPlans = new StringBuilder();
                int totalChangeLines = 0;
                int totalMaxChangeLines = parameters.getRollbackProperties().getTotalMaxChangeLines();
                Long timeoutMills = parameters.getRollbackProperties().getMaxTimeoutMillisecond();
                while (CollectionUtils.isNotEmpty(userInputSqls)
                        || (uploadFileSqlIterator != null && uploadFileSqlIterator.hasNext())) {
                    if (aborted) {
                        log.info("Generate rollback plan task has been aborted, taskId={}", taskId);
                        break;
                    }
                    String sql;
                    if (CollectionUtils.isNotEmpty(userInputSqls)) {
                        sql = userInputSqls.remove(0).getStr();
                    } else {
                        sql = uploadFileSqlIterator.next().getStr();
                    }
                    try {
                        long timeoutForCurrentSql = timeoutMills - (System.currentTimeMillis() - startTimeMills);
                        if (timeoutForCurrentSql <= 0) {
                            log.warn("Generate rollback plan task has timeout, timeout milliseconds={}, taskId={}",
                                    timeoutMills, taskId);
                            rollbackPlans.append("/* Generate rollback plan task has timeout, timeout milliseconds=")
                                    .append(timeoutMills).append(", generate rollback plan will be stopped. */\n");
                            handleRollbackResult(rollbackPlans.toString());
                            this.success = true;
                            return this.success;
                        }
                        GenerateRollbackPlan rollbackPlan = RollbackGeneratorFactory.create(sql,
                                parameters.getRollbackProperties(), session, timeoutForCurrentSql);
                        RollbackPlan result = rollbackPlan.generate();
                        totalChangeLines += result.getChangeLineCount();
                        rollbackPlans.append(result);
                        if (totalChangeLines > totalMaxChangeLines) {
                            log.info(
                                    "The number of changed lines for taskId={} exceeds the maximum limit, changed line count={}",
                                    taskId, totalChangeLines);
                            rollbackPlans.append("/* The number of changed lines exceeds the maximum limit:")
                                    .append(totalMaxChangeLines)
                                    .append(", generate rollback plan will be stopped. */\n");
                            handleRollbackResult(rollbackPlans.toString());
                            this.success = true;
                            return this.success;
                        }
                    } catch (UnsupportedSqlTypeForRollbackPlanException unsupportedSqlTypeException) {
                        log.info(unsupportedSqlTypeException.getMessage());
                    } catch (Exception e) {
                        // Continue to generate rollback plan for the next sql
                        log.warn("Failed to generate rollback plan for sql:{}, error message:{}", sql, e.getMessage());
                    }
                }
                handleRollbackResult(rollbackPlans.toString());
                this.success = true;
            } finally {
                session.expire();
            }
        } catch (Exception e) {
            rollbackPlanTaskResult = RollbackPlanTaskResult.fail(e.getMessage());
            context.getExceptionListener().onException(e);
            throw e;
        } finally {
            tryCloseInputStream();
        }
        return this.success;
    }

    @Override
    public void stop() {
        this.aborted = true;
        tryCloseInputStream();
    }

    @Override
    public void close() {
        tryCloseInputStream();
    }

    @Override
    public double getProgress() {
        return this.success ? 100D : 0D;
    }

    @Override
    public FlowTaskResult getTaskResult() {
        return this.rollbackPlanTaskResult;
    }

    private void loadUserInputSqlContent() {
        String delimiter = parameters.getDelimiter() != null ? parameters.getDelimiter() : ";";
        String sqlContent = parameters.getSqlContent();
        if (StringUtils.isNotBlank(sqlContent)) {
            this.userInputSqls = SqlUtils.splitWithOffset(this.parameters.getConnectionConfig().getDialectType(),
                    sqlContent, delimiter);
        }
    }

    private void loadUploadFileInputStream() throws IOException {
        String delimiter = parameters.getDelimiter() != null ? parameters.getDelimiter() : ";";
        List<ObjectMetadata> objectMetadataList = this.parameters.getSqlFileObjectMetadatas();
        if (CollectionUtils.isNotEmpty(objectMetadataList)) {
            this.uploadFileInputStream =
                    ObjectStorageUtils
                            .loadObjectsForTask(objectMetadataList, context.getSharedStorage(),
                                    JobUtils.getExecutorDataPath(),
                                    parameters.getRollbackProperties().getMaxRollbackContentSizeBytes())
                            .getInputStream();
            this.uploadFileSqlIterator = SqlUtils.iterator(this.parameters.getConnectionConfig().getDialectType(),
                    delimiter, this.uploadFileInputStream, StandardCharsets.UTF_8);
        }
    }

    private void handleRollbackResult(String rollbackResult) {
        if (StringUtils.isNotBlank(rollbackResult)) {
            long totalSize = rollbackResult.getBytes().length;
            if (totalSize > parameters.getRollbackProperties().getMaxRollbackContentSizeBytes()) {
                log.warn("Rollback plan result file size exceeds maximum, totalSize={}, taskId={}", totalSize, taskId);
                throw new UnsupportedSqlTypeForRollbackPlanException("Rollback plan result file size exceeds maximum");
            }
            try {
                String resultFileDownloadUrl = String.format(
                        "/api/v2/flow/flowInstances/%s/tasks/rollbackPlan/download", parameters.getFlowInstanceId());
                String resultFileRootPath = FileManager.generatePath(FileBucket.ROLLBACK_PLAN);
                String resultFileId = StringUtils.uuid();
                String filePath = String.format("%s/%s.sql", resultFileRootPath, resultFileId);
                FileUtils.writeStringToFile(new File(filePath), rollbackResult, StandardCharsets.UTF_8);
                CloudObjectStorageService cloudObjectStorageService = context.getSharedStorage();
                if (Objects.nonNull(cloudObjectStorageService) && cloudObjectStorageService.supported()) {
                    File tempFile = new File(filePath);
                    try {
                        String objectName = cloudObjectStorageService.uploadTemp(resultFileId + ".sql", tempFile);
                        resultFileDownloadUrl = cloudObjectStorageService.getBucketName() + "/" + objectName;
                        log.info("Upload generated rollback plan task result file to OSS, file name={}", resultFileId);
                    } finally {
                        OdcFileUtil.deleteFiles(tempFile);
                    }
                }
                this.rollbackPlanTaskResult = RollbackPlanTaskResult.success(resultFileId, resultFileDownloadUrl);
            } catch (Exception e) {
                log.warn("Failed to put generated rollback plan file for taskId={}", taskId);
                throw new UnexpectedException("Failed to put generated rollback plan file for taskId=" + taskId);
            }
        } else {
            this.rollbackPlanTaskResult = RollbackPlanTaskResult.skip();
        }
    }

    private void tryCloseInputStream() {
        if (Objects.nonNull(this.uploadFileInputStream)) {
            try {
                this.uploadFileInputStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
