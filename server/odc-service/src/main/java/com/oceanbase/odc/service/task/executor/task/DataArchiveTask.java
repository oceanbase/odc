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
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.job.DLMJobReq;
import com.oceanbase.odc.service.schedule.model.DlmTableUnitStatistic;
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
public class DataArchiveTask extends BaseTask<List<DlmTableUnit>> {

    private DLMJobFactory jobFactory;
    private DLMJobStore jobStore;
    private double progress = 0.0;
    private Job job;
    private Map<String, DlmTableUnit> result;
    private DlmTableUnit dlmTableUnit;
    private boolean isToStop = false;


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
        try {
            result = getDlmTableUnits(parameters).stream()
                    .collect(Collectors.toMap(DlmTableUnit::getDlmTableUnitId, o -> o));
        } catch (Exception e) {
            log.warn("Get dlm job failed!", e);
            return false;
        }
        Set<String> dlmTableUnitIds = result.keySet();

        for (String dlmTableUnitId : dlmTableUnitIds) {
            dlmTableUnit = result.get(dlmTableUnitId);
            dlmTableUnit.setStatus(TaskStatus.RUNNING);
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
                    // jobStore.updateDlmTableUnitStatus(dlmTableUnit.getDlmTableUnitId(), TaskStatus.FAILED);
                    dlmTableUnit.setStatus(TaskStatus.FAILED);
                    continue;
                }
            }
            dlmTableUnit.setStartTime(new Date());
            try {
                job = jobFactory.createJob(dlmTableUnit);
                log.info("Init {} job succeed,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                log.info("{} job start,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                if (isToStop) {
                    job.stop();
                    dlmTableUnit.setStatus(TaskStatus.CANCELED);
                    log.info("The task has stopped.");
                    break;
                } else {
                    job.run();
                }
                log.info("{} job finished,DLMJobId={}", dlmTableUnit.getType(), dlmTableUnitId);
                dlmTableUnit.setStatus(TaskStatus.DONE);
            } catch (Throwable e) {
                log.error("{} job failed,DLMJobId={},errorMsg={}", dlmTableUnit.getType(), dlmTableUnitId, e);
                // set task status to failed if any job failed.
                if (job != null && job.getJobMeta().isToStop()) {
                    dlmTableUnit.setStatus(TaskStatus.CANCELED);
                } else {
                    dlmTableUnit.setStatus(TaskStatus.FAILED);
                }
            }
            dlmTableUnit.setEndTime(new Date());
        }
        return true;
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
    protected void doStop() throws Exception {
        if (job != null) {
            try {
                job.stop();
                dlmTableUnit.setStatus(TaskStatus.CANCELED);
            } catch (Exception e) {
                log.warn("Update dlm table unit status failed,DlmTableUnitId={}", job.getJobMeta().getJobId());
            }
        }
        isToStop = true;
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
    public List<DlmTableUnit> getTaskResult() {
        if (job != null) {
            JobMeta jobMeta = job.getJobMeta();
            log.info("Update statistic:{}", jobMeta.getJobStat());
            result.get(jobMeta.getJobId()).getStatistic()
                    .setReadRowsPerSecond(jobMeta.getJobStat().getAvgReadRowCount());
            result.get(jobMeta.getJobId()).getStatistic().setReadRowCount(jobMeta.getJobStat().getReadRowCount());
            result.get(jobMeta.getJobId()).getStatistic()
                    .setProcessedRowsPerSecond(jobMeta.getJobStat().getAvgRowCount());
            result.get(jobMeta.getJobId()).getStatistic().setProcessedRowCount(jobMeta.getJobStat().getRowCount());
        }
        log.info("Get result:{}", result);
        return new ArrayList<>(result.values());
    }
}
