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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
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
import com.oceanbase.tools.migrator.common.configure.JoinCondition;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.job.Job;
import com.oceanbase.tools.migrator.limiter.LimiterConfig;

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
    private int currentIndex = -1;
    private boolean isToStop = false;
    private boolean isTimeout = false;

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
            log.info("Start to init dlm job,tables={}", parameters.getTables());
            initTableUnit(parameters);
            currentIndex = -1;
            log.info(buildToDoTableInfo());
        } catch (Exception e) {
            log.warn("Initialization of the DLM job was failed,jobIdentity={}", context.getJobIdentity(), e);
        }
        log.info("Initialization of the DLM job was successful. Number of tables to be processed = {},jobIdentity={}",
                toDoList.size(), context.getJobIdentity());
    }

    @Override
    public boolean start() throws Exception {
        while (!isToStop) {
            DlmTableUnit dlmTableUnit = getNextTableUnit();
            if (dlmTableUnit == null) {
                break;
            }
            syncTableStructure(dlmTableUnit);
            try {
                jobStore.setDlmTableUnit(dlmTableUnit);
                job = jobFactory.createJob(dlmTableUnit);
            } catch (Throwable e) {
                log.error("Failed to create job,dlmTableUnitId={}", dlmTableUnit.getDlmTableUnitId(), e);
                dlmTableUnit.setStatus(isToStop ? TaskStatus.CANCELED : TaskStatus.FAILED);
                continue;
            }
            log.info("Init {} job succeed,dlmTableUnitId={}", dlmTableUnit.getType(), dlmTableUnit.getDlmTableUnitId());
            try {
                if (!isToStop && dlmTableUnit.getStatus() == TaskStatus.PREPARING) {
                    dlmTableUnit.setStatus(TaskStatus.RUNNING);
                    dlmTableUnit.setStartTime(new Date());
                    job.run();
                    log.info("{} job finished,dlmTableUnitId={}", dlmTableUnit.getType(),
                            dlmTableUnit.getDlmTableUnitId());
                    dlmTableUnit.setStatus(TaskStatus.DONE);
                } else {
                    log.warn("Job is canceled,dlmTableUnitId={},status={}", dlmTableUnit.getDlmTableUnitId(),
                            dlmTableUnit.getStatus());
                }
            } catch (Throwable e) {
                dlmTableUnit.setStatus(isToStop ? TaskStatus.CANCELED : TaskStatus.FAILED);
                context.getExceptionListener().onException(e);
            }
            dlmTableUnit.setEndTime(new Date());
        }
        log.info("All tables have been processed,jobIdentity={}.\n{}", jobContext.getJobIdentity(), buildReport());
        return true;
    }

    private DlmTableUnit getNextTableUnit() {
        if (CollectionUtils.isEmpty(toDoList)) {
            log.warn("The table list is empty,the task will exit.");
            return null;
        }
        currentIndex++;
        // skip tables where the state is not "PREPARING"
        while (currentIndex < toDoList.size() && toDoList.get(currentIndex).getStatus() != TaskStatus.PREPARING) {
            log.info("Skip table {},tableUnitId={},status={}", toDoList.get(currentIndex).getTableName(),
                    toDoList.get(currentIndex).getDlmTableUnitId(), toDoList.get(currentIndex).getStatus());
            currentIndex++;
        }
        if (currentIndex >= toDoList.size()) {
            log.info("All tables are processed,the task will exit.");
            return null;
        }
        DlmTableUnit dlmTableUnit = toDoList.get(currentIndex);
        log.info("Next table is = {},tableUnitId={},tableIndex = {}/{}.", dlmTableUnit.getTableName(),
                dlmTableUnit.getDlmTableUnitId(), currentIndex + 1, toDoList.size());
        return toDoList.get(currentIndex);
    }

    private void syncTableStructure(DlmTableUnit tableUnit) {
        if (tableUnit.getType() != JobType.MIGRATE) {
            return;
        }
        if (tableUnit.getParameters().isCreateTempTableInSource()) {
            DLMTableStructureSynchronizer.createTempTable(tableUnit.getSourceDatasourceInfo(), tableUnit.getTableName(),
                    tableUnit.getParameters().getTempTableName());
            return;
        }
        long startTimeMillis = System.currentTimeMillis();
        try {
            DLMTableStructureSynchronizer.sync(tableUnit.getSourceDatasourceInfo(), tableUnit.getTargetDatasourceInfo(),
                    tableUnit.getTableName(), tableUnit.getTargetTableName(),
                    tableUnit.getSyncTableStructure());
        } catch (Exception e) {
            log.warn("Failed to sync target table structure,tableName={}",
                    tableUnit.getTableName(), e);
        }
        log.info("Sync table structure cost {} millis.", System.currentTimeMillis() - startTimeMillis);
    }

    private void initTableUnit(DLMJobReq req) {
        List<DlmTableUnit> dlmTableUnits = new LinkedList<>();
        req.getTables().forEach(table -> {
            DlmTableUnit dlmTableUnit = new DlmTableUnit();
            dlmTableUnit.setScheduleTaskId(req.getScheduleTaskId());
            DlmTableUnitParameters jobParameter = new DlmTableUnitParameters();
            jobParameter.setMigrateRule(table.getConditionExpression());
            jobParameter.setReaderBatchSize(req.getRateLimit().getBatchSize());
            jobParameter.setWriterBatchSize(req.getRateLimit().getBatchSize());
            jobParameter.setMigrationInsertAction(req.getMigrationInsertAction());
            jobParameter.setMigratePartitions(table.getPartitions());
            jobParameter.setSyncDBObjectType(req.getSyncTableStructure());
            jobParameter.setShardingStrategy(req.getShardingStrategy());
            jobParameter.setPartName2MinKey(table.getPartName2MinKey());
            jobParameter.setPartName2MaxKey(table.getPartName2MaxKey());
            if (req.getReadThreadCount() != 0) {
                jobParameter.setReaderTaskCount(req.getReadThreadCount());
            }
            if (req.getWriteThreadCount() != 0) {
                jobParameter.setWriterTaskCount(req.getWriteThreadCount());
            }
            jobParameter.setCreateTempTableInSource(
                    req.isDeleteAfterMigration() && req.getTargetDs().getType().isFileSystem());
            jobParameter.setDirtyRowAction(req.getDirtyRowAction());
            jobParameter.setMaxAllowedDirtyRowCount(req.getMaxAllowedDirtyRowCount());
            jobParameter.setJoinConditions(table.getJoinTableConfigs().stream().map(joinTableConfig -> {
                JoinCondition joinCondition = new JoinCondition();
                joinCondition.setTableName(joinTableConfig.getTableName());
                joinCondition.setCondition(joinTableConfig.getJoinCondition());
                return joinCondition;
            }).collect(Collectors.toList()));
            jobParameter.setPrintSqlTrace(true);
            dlmTableUnit.setParameters(jobParameter);
            dlmTableUnit.setDlmTableUnitId(DlmJobIdUtil.generateHistoryJobId(req.getJobName(), req.getJobType().name(),
                    req.getScheduleTaskId(), dlmTableUnits.size()));
            dlmTableUnit.setTableName(table.getTableName());
            dlmTableUnit.setTargetTableName(table.getTargetTableName());
            dlmTableUnit.setSourceDatasourceInfo(req.getSourceDs());
            dlmTableUnit.setTargetDatasourceInfo(req.getTargetDs());
            dlmTableUnit.setFireTime(req.getFireTime());
            dlmTableUnit.setStatus(
                    table.getLastProcessedStatus() == null ? TaskStatus.PREPARING : table.getLastProcessedStatus());
            dlmTableUnit.setType(req.getJobType());
            dlmTableUnit.setStatistic(new DlmTableUnitStatistic());
            dlmTableUnit.setSyncTableStructure(req.getSyncTableStructure());
            LimiterConfig limiterConfig = new LimiterConfig();
            limiterConfig.setDataSizeLimit(req.getRateLimit().getDataSizeLimit());
            limiterConfig.setRowLimit(req.getRateLimit().getRowLimit());
            dlmTableUnit.setSourceLimitConfig(limiterConfig);
            dlmTableUnit.setTargetLimitConfig(limiterConfig);
            if (StringUtils.isNotEmpty(table.getTempTableName())) {
                // save data to temporary table
                if (req.getJobType() == JobType.MIGRATE && req.isDeleteAfterMigration()
                        && req.getTargetDs().getType().isFileSystem()) {
                    jobParameter.setCreateTempTableInSource(true);
                    jobParameter.setTempTableName(table.getTempTableName());
                }
                // check data by temporary table
                if (req.getJobType() == JobType.DELETE && req.getTargetDs().getType().isFileSystem()) {
                    dlmTableUnit.setTargetDatasourceInfo(req.getSourceDs());
                    dlmTableUnit.setTargetTableName(table.getTempTableName());
                    jobParameter.setTempTableName(table.getTempTableName());
                    jobParameter.setDeleteTempTableAfterDelete(req.isDeleteTemporaryTable());
                }
                if (req.getJobType() == JobType.ROLLBACK && req.getSourceDs().getType().isFileSystem()) {
                    dlmTableUnit.setSourceDatasourceInfo(req.getTargetDs());
                    dlmTableUnit.setTableName(table.getTempTableName());
                }
            }
            dlmTableUnits.add(dlmTableUnit);
        });
        dlmTableUnits.sort(Comparator.comparing(DlmTableUnit::getDlmTableUnitId));
        toDoList = Collections.unmodifiableList(dlmTableUnits);
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

    private String buildToDoTableInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("To do table list:\n");
        for (int i = 0; i < toDoList.size(); i++) {
            sb.append("[").append(i).append("] ").append(toDoList.get(i).getTableName());
        }
        return sb.toString();
    }

    @Override
    public void stop() throws Exception {
        isToStop = true;
        if (job != null) {
            try {
                job.stop();
            } catch (Exception e) {
                log.warn("Update dlm table unit status failed,DlmTableUnitId={}", job.getJobMeta().getJobId());
            }
        }
        if (toDoList != null) {
            toDoList.forEach(t -> {
                if (!t.getStatus().isTerminated()) {
                    t.setStatus(isTimeout ? TaskStatus.EXEC_TIMEOUT : TaskStatus.CANCELED);
                }
            });
            log.info("Stop all table success.");
        }
    }

    @Override
    public void timeout() throws Exception {
        this.isTimeout = true;
        stop();
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
        JobMeta jobMeta = job != null && job.getJobMeta() != null ? job.getJobMeta() : null;
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
                if (jobMeta != null) {
                    jobMeta.getSourceLimiterConfig().setDataSizeLimit(params.getDataSizeLimit());
                    jobMeta.getTargetLimiterConfig().setDataSizeLimit(params.getDataSizeLimit());
                }
                toDoList.forEach(t -> {
                    t.getSourceLimitConfig().setDataSizeLimit(params.getDataSizeLimit());
                    t.getTargetLimitConfig().setDataSizeLimit(params.getDataSizeLimit());
                });
                log.info("Update rate limit success,dataSizeLimit={}", params.getDataSizeLimit());
            }
            if (params.getRowLimit() != null) {
                if (jobMeta != null) {
                    jobMeta.getSourceLimiterConfig().setRowLimit(params.getRowLimit());
                    jobMeta.getTargetLimiterConfig().setRowLimit(params.getRowLimit());
                }
                toDoList.forEach(t -> {
                    t.getSourceLimitConfig().setRowLimit(params.getRowLimit());
                    t.getTargetLimitConfig().setRowLimit(params.getRowLimit());
                });
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
