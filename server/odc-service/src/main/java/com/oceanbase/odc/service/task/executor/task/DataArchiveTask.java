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
import com.oceanbase.odc.service.dlm.model.DLMJobParameters;
import com.oceanbase.odc.service.dlm.model.DlmJob;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.job.DLMJobReq;
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
        List<DlmJob> dlmJobs;
        try {
            dlmJobs = getDlmJobs(parameters);
        } catch (Exception e) {
            log.warn("Get dlm job failed!", e);
            return false;
        }

        for (DlmJob dlmJob : dlmJobs) {
            if (getStatus().isTerminated()) {
                log.info("Job is terminated,jobIdentity={}", context.getJobIdentity());
                break;
            }
            if (parameters.getJobType() == JobType.MIGRATE && !parameters.getSyncTableStructure().isEmpty()) {
                try {
                    DLMTableStructureSynchronizer.sync(
                            DataSourceInfoMapper.toConnectionConfig(parameters.getSourceDs()),
                            DataSourceInfoMapper.toConnectionConfig(parameters.getTargetDs()),
                            dlmJob.getTableName(), parameters.getSyncTableStructure());
                } catch (Exception e) {
                    log.warn("Failed to sync target table structure,table will be ignored,tableName={}",
                            dlmJob.getTableName(), e);
                    continue;
                }
            }
            try {
                job = jobFactory.createJob(dlmJob);
                log.info("Init {} job succeed,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                log.info("{} job start,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                job.run();
                log.info("{} job finished,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
            } catch (Throwable e) {
                log.error("{} job failed,DLMJobId={},errorMsg={}", job.getJobMeta().getJobType(),
                        job.getJobMeta().getJobId(),
                        e);
                // set task status to failed if any job failed.
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    private List<DlmJob> getDlmJobs(DLMJobReq req) throws SQLException {

        List<DlmJob> existsDlmJobs = jobStore.getDlmJobs(req.getScheduleTaskId());
        if (!existsDlmJobs.isEmpty()) {
            return existsDlmJobs;
        }
        List<DlmJob> dlmJobs = new LinkedList<>();
        req.getTables().forEach(table -> {
            DlmJob dlmJob = new DlmJob();
            dlmJob.setScheduleTaskId(req.getScheduleTaskId());
            DLMJobParameters jobParameter = new DLMJobParameters();
            jobParameter.setMigrateRule(table.getConditionExpression());
            jobParameter.setCheckMode(CheckMode.MULTIPLE_GET);
            jobParameter.setReaderBatchSize(req.getRateLimit().getBatchSize());
            jobParameter.setWriterBatchSize(req.getRateLimit().getBatchSize());
            jobParameter.setMigrationInsertAction(req.getMigrationInsertAction());
            jobParameter.setMigratePartitions(table.getPartitions());
            jobParameter.setSyncDBObjectType(req.getSyncTableStructure());
            dlmJob.setParameters(jobParameter);
            dlmJob.setDlmJobId(DlmJobIdUtil.generateHistoryJobId(req.getJobName(), req.getJobType().name(),
                    req.getScheduleTaskId(), dlmJobs.size()));
            dlmJob.setTableName(table.getTableName());
            dlmJob.setTargetTableName(table.getTargetTableName());
            dlmJob.setSourceDatasourceInfo(req.getSourceDs());
            dlmJob.setTargetDatasourceInfo(req.getTargetDs());
            dlmJob.setFireTime(req.getFireTime());
            dlmJob.setStatus(TaskStatus.PREPARING);
            dlmJob.setType(JobType.MIGRATE);
            dlmJobs.add(dlmJob);
        });
        jobStore.storeDlmJob(dlmJobs);
        return dlmJobs;
    }

    @Override
    protected void doStop() throws Exception {
        job.getJobMeta().setToStop(true);
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
