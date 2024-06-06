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
package com.oceanbase.odc.service.task.executor.task;

import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.dlm.DLMJobFactory;
import com.oceanbase.odc.service.dlm.DLMJobStore;
import com.oceanbase.odc.service.dlm.DLMTableStructureSynchronizer;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.DlmTableUnitParameters;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.job.DLMJobReq;
import com.oceanbase.odc.service.schedule.model.DlmTableUnitStatistic;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.job.Job;
import com.oceanbase.tools.migrator.task.CheckMode;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/1/24 11:09
 * @Descripition:
 */

@Slf4j
public class DataArchiveTask extends BaseTask<Boolean> {

    private DLMJobFactory jobFactory;
    private DLMJobStore jobStore;
    private boolean isSuccess = true;
    private double progress = 0.0;
    private Job job;

    @Override
    protected void doInit(JobContext context) {
        jobStore = new DLMJobStore(JobUtils.getMetaDBConnectionConfig());
        jobFactory = new DLMJobFactory(jobStore);
        log.info("Init data-archive job env succeed,jobIdentity={}", context.getJobIdentity());
    }

    @Override
    protected boolean doStart(JobContext context) throws Exception {

        String taskParameters = context.getJobParameters().get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON);
        DLMJobReq parameters = JsonUtils.fromJson(taskParameters,
                DLMJobReq.class);
        if (parameters.getFireTime() == null) {
            parameters.setFireTime(new Date());
        }
        List<DlmTableUnit> dlmTableUnits;
        try {
            dlmTableUnits = getDlmTableUnits(parameters);
        } catch (Exception e) {
            log.warn("Get dlm job failed!", e);
            return false;
        }

        for (DlmTableUnit dlmTableUnit : dlmTableUnits) {
            if (getStatus().isTerminated()) {
                log.info("Job is terminated,jobIdentity={}", context.getJobIdentity());
                break;
            }
            if (dlmTableUnit.getStatus() == TaskStatus.DONE) {
                log.info("The table had been completed,tableName={}", dlmTableUnit.getTableName());
                continue;
            }
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
                    jobStore.updateDlmTableUnitStatus(dlmTableUnit.getDlmTableUnitId(), TaskStatus.FAILED);
                    continue;
                }
            }
            try {
                job = jobFactory.createJob(dlmTableUnit);
                jobStore.updateDlmTableUnitStatus(dlmTableUnit.getDlmTableUnitId(), TaskStatus.RUNNING);
                log.info("Init {} job succeed,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                log.info("{} job start,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                job.run();
                log.info("{} job finished,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                jobStore.updateDlmTableUnitStatus(dlmTableUnit.getDlmTableUnitId(), TaskStatus.DONE);
            } catch (Throwable e) {
                log.error("{} job failed,DLMJobId={},errorMsg={}", job.getJobMeta().getJobType(),
                        job.getJobMeta().getJobId(),
                        e);
                // set task status to failed if any job failed.
                isSuccess = false;
                if (job.getJobMeta().isToStop()) {
                    jobStore.updateDlmTableUnitStatus(dlmTableUnit.getDlmTableUnitId(), TaskStatus.CANCELED);
                } else {
                    jobStore.updateDlmTableUnitStatus(dlmTableUnit.getDlmTableUnitId(), TaskStatus.FAILED);
                }
            }
        }
        return isSuccess;
    }

    private List<DlmTableUnit> getDlmTableUnits(DLMJobReq req) throws SQLException {

        List<DlmTableUnit> existsDlmJobs = jobStore.getDlmTableUnits(req.getScheduleTaskId());
        if (!existsDlmJobs.isEmpty()) {
            return existsDlmJobs;
        }
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
        jobStore.storeDlmTableUnit(dlmTableUnits);
        return dlmTableUnits;
    }

    @Override
    protected void doStop() throws Exception {
        job.stop();
        try {
            jobStore.updateDlmTableUnitStatus(job.getJobMeta().getJobId(), TaskStatus.CANCELED);
        } catch (Exception e) {
            log.warn("Update dlm table unit status failed,DlmTableUnitId={}", job.getJobMeta().getJobId());
        }
    }

    @Override
    protected void doClose() throws Exception {
        jobStore.destroy();
    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public Boolean getTaskResult() {
        return isSuccess;
    }
}
