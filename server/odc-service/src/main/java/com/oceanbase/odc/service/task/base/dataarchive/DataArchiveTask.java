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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.dlm.DLMJobFactory;
import com.oceanbase.odc.service.dlm.DLMJobStore;
import com.oceanbase.odc.service.dlm.DLMTableStructureSynchronizer;
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
import com.oceanbase.tools.migrator.limiter.LimiterConfig;
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
    private List<DlmTableUnit> toDoList;
    private int currentIndex = 0;
    private boolean isToStop = false;

    public DataArchiveTask() {}

    @Override
    protected void doInit(JobContext context) {
        jobStore = new DLMJobStore(JobUtils.getMetaDBConnectionConfig());
        jobFactory = new DLMJobFactory(jobStore);
        try {
            DLMJobReq parameters =
                    JsonUtils.fromJson(
                            jobContext.getJobParameters().get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                            DLMJobReq.class);
            initTableUnit(parameters);
        } catch (Exception e) {
            log.warn("Initialization of the DLM job was failed,jobIdentity={}", context.getJobIdentity(), e);
        }
        log.info("Initialization of the DLM job was successful. Number of tables to be processed = {},jobIdentity={}",
                toDoList.size(), context.getJobIdentity());
    }

    @Override
    public boolean start() throws Exception {
        while (!isToStop && currentIndex < toDoList.size()) {
            DlmTableUnit dlmTableUnit = toDoList.get(currentIndex);
            if (dlmTableUnit.getStatus() == TaskStatus.DONE) {
                log.info("The table had been completed,tableName={}", dlmTableUnit.getTableName());
                currentIndex++;
                continue;
            }
            syncTableStructure(dlmTableUnit);
            try {
                jobStore.setDlmTableUnit(dlmTableUnit);
                job = jobFactory.createJob(dlmTableUnit);
            } catch (Throwable e) {
                log.error("Failed to create job,dlmTableUnitId={}", dlmTableUnit.getDlmTableUnitId(), e);
                dlmTableUnit.setStatus(isToStop ? TaskStatus.CANCELED : TaskStatus.FAILED);
                currentIndex++;
                continue;
            }
            log.info("Init {} job succeed,dlmTableUnitId={}", dlmTableUnit.getType(), dlmTableUnit.getDlmTableUnitId());
            try {
                dlmTableUnit.setStatus(TaskStatus.RUNNING);
                dlmTableUnit.setStartTime(new Date());
                job.run();
                log.info("{} job finished,dlmTableUnitId={}", dlmTableUnit.getType(), dlmTableUnit.getDlmTableUnitId());
                dlmTableUnit.setStatus(TaskStatus.DONE);
            } catch (Throwable e) {
                dlmTableUnit.setStatus(isToStop ? TaskStatus.CANCELED : TaskStatus.FAILED);
                context.getExceptionListener().onException(e);
            }
            dlmTableUnit.setEndTime(new Date());
            currentIndex++;
        }
        log.info("All tables have been processed,jobIdentity={}.\n{}", jobContext.getJobIdentity(), buildReport());
        return true;
    }

    private void syncTableStructure(DlmTableUnit tableUnit) {
        if (tableUnit.getType() != JobType.MIGRATE) {
            return;
        }
        try {
            DLMTableStructureSynchronizer.sync(tableUnit.getSourceDatasourceInfo(), tableUnit.getTargetDatasourceInfo(),
                    tableUnit.getTableName(), tableUnit.getTargetTableName(),
                    tableUnit.getSyncTableStructure());
        } catch (Exception e) {
            log.warn("Failed to sync target table structure,tableName={}",
                    tableUnit.getTableName(), e);
        }
    }

    private void initTableUnit(DLMJobReq req) {
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
            jobParameter.setPartName2MinKey(table.getPartName2MinKey());
            jobParameter.setPartName2MaxKey(table.getPartName2MaxKey());
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
            dlmTableUnit.setSyncTableStructure(req.getSyncTableStructure());
            LimiterConfig limiterConfig = new LimiterConfig();
            limiterConfig.setDataSizeLimit(req.getRateLimit().getDataSizeLimit());
            limiterConfig.setRowLimit(req.getRateLimit().getRowLimit());
            dlmTableUnit.setSourceLimitConfig(limiterConfig);
            dlmTableUnit.setTargetLimitConfig(limiterConfig);
            dlmTableUnits.add(dlmTableUnit);
        });
        toDoList = new LinkedList<>(dlmTableUnits);
    }

    private String buildReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Job report:\n");
        sb.append("Total tables: ").append(toDoList.size()).append("\n");
        sb.append("Success tables: ")
                .append(toDoList.stream().filter(t -> t.getStatus() == TaskStatus.DONE).map(DlmTableUnit::getTableName)
                        .collect(
                                Collectors.joining(",")))
                .append("\n");
        sb.append("Failed tables: ")
                .append(toDoList.stream().filter(t -> t.getStatus() == TaskStatus.FAILED)
                        .map(DlmTableUnit::getTableName).collect(
                                Collectors.joining(",")))
                .append("\n");
        sb.append("Canceled tables: ")
                .append(toDoList.stream().filter(t -> t.getStatus() == TaskStatus.CANCELED)
                        .map(DlmTableUnit::getTableName).collect(
                                Collectors.joining(",")))
                .append("\n");
        return sb.toString();
    }

    @Override
    public void stop() throws Exception {
        isToStop = true;
        if (job != null) {
            try {
                job.stop();
                toDoList.forEach(t -> {
                    if (!t.getStatus().isTerminated()) {
                        t.setStatus(TaskStatus.CANCELED);
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
        return new ArrayList<>(toDoList);
    }
}
