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
package com.oceanbase.odc.service.task.base.dataarchive;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.dlm.DLMJobFactory;
import com.oceanbase.odc.service.dlm.DLMJobStore;
import com.oceanbase.odc.service.dlm.DLMTableStructureSynchronizer;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.DlmTableUnitParameters;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.job.DLMJobReq;
import com.oceanbase.odc.service.schedule.model.DlmTableUnitStatistic;
import com.oceanbase.odc.service.task.base.TaskBase;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.job.Job;
import com.oceanbase.tools.migrator.task.CheckMode;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/1/24 11:09
 * @Descripition:
 */

@Slf4j
public class DataArchiveTask extends TaskBase<List<DlmTableUnit>> {

    private DLMJobFactory jobFactory;
    private DLMJobStore jobStore;
    private double progress = 0.0;
    private Job job;
    private Map<String, DlmTableUnit> result;
    private boolean isToStop = false;

    public DataArchiveTask() {}

    @Override
    protected void doInit(JobContext context) {
        jobStore = new DLMJobStore(JobUtils.getMetaDBConnectionConfig());
        jobFactory = new DLMJobFactory(jobStore);
        log.info("Init data-archive job env succeed,jobIdentity={}", context.getJobIdentity());
    }

    @Override
    public boolean start() throws Exception {

        DLMJobReq parameters =
                JsonUtils.fromJson(
                        jobContext.getJobParameters().get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                        DLMJobReq.class);
        if (parameters.getFireTime() == null) {
            parameters.setFireTime(new Date());
        }
        try {
            result = getDlmTableUnits(parameters).stream()
                    .collect(Collectors.toMap(DlmTableUnit::getDlmTableUnitId, o -> o));
            jobStore.setDlmTableUnits(result);
        } catch (Exception e) {
            log.warn("Get dlm job failed!", e);
            context.getExceptionListener().onException(e);
            return false;
        }
        Set<String> dlmTableUnitIds = result.keySet();

        for (String dlmTableUnitId : dlmTableUnitIds) {
            DlmTableUnit dlmTableUnit = result.get(dlmTableUnitId);
            if (isToStop) {
                log.info("Job is terminated,jobIdentity={}", jobContext.getJobIdentity());
                break;
            }
            if (dlmTableUnit.getStatus() == TaskStatus.DONE) {
                log.info("The table had been completed,tableName={}", dlmTableUnit.getTableName());
                continue;
            }
            startTableUnit(dlmTableUnitId);
            if (parameters.getJobType() == JobType.MIGRATE) {
                try {
                    DLMTableStructureSynchronizer.sync(
                            DataSourceInfoMapper.toConnectionConfig(parameters.getSourceDs()),
                            DataSourceInfoMapper.toConnectionConfig(parameters.getTargetDs()),
                            dlmTableUnit.getTableName(), dlmTableUnit.getTargetTableName(),
                            parameters.getSyncTableStructure());
                } catch (Exception e) {
                    log.warn("Failed to sync target table structure,table will be ignored,tableName={}",
                            dlmTableUnit.getTableName(), e);
                    // jobStore.updateDlmTableUnitStatus(dlmTableUnit.getDlmTableUnitId(), TaskStatus.FAILED);
                    finishTableUnit(dlmTableUnitId, TaskStatus.FAILED);
                    continue;
                }
            }
            try {
                job = jobFactory.createJob(dlmTableUnit);
                log.info("Init {} job succeed,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                log.info("{} job start,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                if (isToStop) {
                    finishTableUnit(dlmTableUnitId, TaskStatus.CANCELED);
                    job.stop();
                    log.info("The task has stopped.");
                    break;
                } else {
                    job.run();
                }
                log.info("{} job finished,DLMJobId={}", dlmTableUnit.getType(), dlmTableUnitId);
                finishTableUnit(dlmTableUnitId, TaskStatus.DONE);
            } catch (Throwable e) {
                log.error("{} job failed,DLMJobId={},errorMsg={}", dlmTableUnit.getType(), dlmTableUnitId, e);
                // set task status to failed if any job failed.
                if (job != null && job.getJobMeta().isToStop()) {
                    finishTableUnit(dlmTableUnitId, TaskStatus.CANCELED);
                } else {
                    finishTableUnit(dlmTableUnitId, TaskStatus.FAILED);
                    context.getExceptionListener().onException(e);
                }
            }
        }
        return true;
    }

    private void startTableUnit(String dlmTableUnitId) {
        result.get(dlmTableUnitId).setStatus(TaskStatus.RUNNING);
        result.get(dlmTableUnitId).setStartTime(new Date());
    }

    private void finishTableUnit(String dlmTableUnitId, TaskStatus status) {
        result.get(dlmTableUnitId).setStatus(status);
        result.get(dlmTableUnitId).setEndTime(new Date());
    }

    private List<DlmTableUnit> getDlmTableUnits(DLMJobReq req) throws SQLException {
        List<DlmTableUnit> dlmTableUnits = new LinkedList<>();
        req.getTables().forEach(table -> {
            DlmTableUnit dlmTableUnit = new DlmTableUnit();
            dlmTableUnit.setScheduleTaskId(req.getScheduleTaskId());
            DlmTableUnitParameters jobParameter = new DlmTableUnitParameters();
            jobParameter.setMigrateRule(table.getConditionExpression());
            jobParameter.setCheckMode(CheckMode.MULTIPLE_GET);
            jobParameter.setReaderBatchSize(req.getRateLimit().getBatchSize());
            jobParameter.setWriterBatchSize(req.getRateLimit().getBatchSize());
            jobParameter.setMigrationInsertAction(req.getMigrationInsertAction());
            jobParameter.setMigratePartitions(table.getPartitions());
            jobParameter.setSyncDBObjectType(req.getSyncTableStructure());
            jobParameter.setShardingStrategy(req.getShardingStrategy());
            dlmTableUnit.setParameters(jobParameter);
            dlmTableUnit.setDlmTableUnitId(DlmJobIdUtil.generateHistoryJobId(req.getJobName(), req.getJobType().name(),
                    req.getScheduleTaskId(), dlmTableUnits.size()));
            dlmTableUnit.setTableName(table.getTableName());
            dlmTableUnit.setTargetTableName(table.getTargetTableName());
            dlmTableUnit.setSourceDatasourceInfo(req.getSourceDs());
            dlmTableUnit.setTargetDatasourceInfo(req.getTargetDs());
            dlmTableUnit.setFireTime(req.getFireTime());
            dlmTableUnit.setStatus(TaskStatus.PREPARING);
            dlmTableUnit.setType(req.getJobType());
            dlmTableUnit.setStatistic(new DlmTableUnitStatistic());
            dlmTableUnits.add(dlmTableUnit);
        });
        return dlmTableUnits;
    }

    @Override
    public void stop() throws Exception {
        isToStop = true;
        if (job != null) {
            try {
                job.stop();
                result.forEach((k, v) -> {
                    if (!v.getStatus().isTerminated()) {
                        v.setStatus(TaskStatus.CANCELED);
                    }
                });
            } catch (Exception e) {
                log.warn("Update dlm table unit status failed,DlmTableUnitId={}", job.getJobMeta().getJobId());
            }
        }
    }

    @Override
    public void close() throws Exception {
        jobStore.destroy();
    }

    @Override
    public boolean modify(Map<String, String> jobParameters) {
        if (!super.modify(jobParameters)) {
            return false;
        }
        updateLimiter(jobParameters);
        return true;
    }

    public void updateLimiter(Map<String, String> jobParameters) {
        if (job == null || job.getJobMeta() == null) {
            return;
        }
        JobMeta jobMeta = job.getJobMeta();
        try {
            RateLimitConfiguration params;
            if (jobParameters.containsKey(JobParametersKeyConstants.DLM_RATE_LIMIT_CONFIG)) {
                params = JsonUtils.fromJson(
                        jobParameters.get(JobParametersKeyConstants.DLM_RATE_LIMIT_CONFIG),
                        RateLimitConfiguration.class);
            } else {
                DLMJobReq dlmJobReq = JsonUtils.fromJson(
                        jobParameters.get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                        DLMJobReq.class);
                params = dlmJobReq.getRateLimit();
            }
            if (params.getDataSizeLimit() != null) {
                jobMeta.getSourceLimiterConfig().setDataSizeLimit(params.getDataSizeLimit());
                jobMeta.getTargetLimiterConfig().setDataSizeLimit(params.getDataSizeLimit());
                log.info("Update rate limit success,dataSizeLimit={}", params.getDataSizeLimit());
            }
            if (params.getRowLimit() != null) {
                jobMeta.getSourceLimiterConfig().setRowLimit(params.getRowLimit());
                jobMeta.getTargetLimiterConfig().setRowLimit(params.getRowLimit());
                log.info("Update rate limit success,rowLimit={}", params.getRowLimit());
            }
        } catch (Exception e) {
            log.warn("Update rate limit failed,errorMsg={}", e.getMessage());
        }
    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public List<DlmTableUnit> getTaskResult() {
        return new ArrayList<>(result.values());
    }
}
