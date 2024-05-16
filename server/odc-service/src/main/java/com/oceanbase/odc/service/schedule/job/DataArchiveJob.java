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
package com.oceanbase.odc.service.schedule.job;

import java.sql.SQLException;
import java.util.List;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.dlm.DLMTableStructureSynchronizer;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.tools.migrator.common.enums.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/9 14:46
 * @Descripition:
 */
@Slf4j
public class DataArchiveJob extends AbstractDlmJob {
    @Override
    public void executeJob(JobExecutionContext context) {
        // execute in task framework.
        if (taskFrameworkProperties.isEnabled()) {
            executeInTaskFramework(context);
            return;
        }
        jobThread = Thread.currentThread();

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();

        List<DlmTask> taskUnits = getTaskUnits(taskEntity);

        executeTask(taskEntity.getId(), taskUnits);
        TaskStatus taskStatus = getTaskStatus(taskUnits);
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);

        DataArchiveParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);

        if (taskStatus == TaskStatus.DONE && parameters.isDeleteAfterMigration()) {
            log.info("Start to create clear job,scheduleTaskId={}", taskEntity.getId());
            scheduleService.dataArchiveDelete(Long.parseLong(taskEntity.getJobName()), taskEntity.getId());
            log.info("Clear job is created,");
        }
    }

    @Override
    public void initTask(DlmTask taskUnit) {
        super.initTask(taskUnit);
        try {
            DLMTableStructureSynchronizer.sync(taskUnit.getSourceDs(), taskUnit.getTargetDs(), taskUnit.getTableName(),
                    taskUnit.getSyncDBObjectTypes());
        } catch (SQLException e) {
            log.warn("Sync table structure failed,tableName={}", taskUnit.getTableName(), e);
        }
    }

    private void executeInTaskFramework(JobExecutionContext context) {
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        DataArchiveParameters dataArchiveParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);
        DLMJobParameters parameters = new DLMJobParameters();
        parameters.setJobName(taskEntity.getJobName());
        parameters.setScheduleTaskId(taskEntity.getId());
        parameters.setJobType(JobType.MIGRATE);
        parameters.setTables(dataArchiveParameters.getTables());
        for (DataArchiveTableConfig tableConfig : parameters.getTables()) {
            tableConfig.setConditionExpression(StringUtils.isNotEmpty(tableConfig.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(tableConfig.getConditionExpression(),
                            dataArchiveParameters.getVariables(),
                            context.getFireTime())
                    : "");
        }
        parameters.setDeleteAfterMigration(dataArchiveParameters.isDeleteAfterMigration());
        parameters.setMigrationInsertAction(dataArchiveParameters.getMigrationInsertAction());
        parameters.setNeedPrintSqlTrace(dataArchiveParameters.isNeedPrintSqlTrace());
        parameters
                .setRateLimit(limiterService.getByOrderIdOrElseDefaultConfig(Long.parseLong(taskEntity.getJobName())));
        parameters.setWriteThreadCount(dataArchiveParameters.getWriteThreadCount());
        parameters.setReadThreadCount(dataArchiveParameters.getReadThreadCount());
        parameters.setShardingStrategy(dataArchiveParameters.getShardingStrategy());
        parameters.setScanBatchSize(dataArchiveParameters.getScanBatchSize());
        parameters
                .setSourceDs(DataSourceInfoMapper.toDataSourceInfo(
                        databaseService.findDataSourceForConnectById(dataArchiveParameters.getSourceDatabaseId())));
        parameters
                .setTargetDs(DataSourceInfoMapper.toDataSourceInfo(
                        databaseService.findDataSourceForConnectById(dataArchiveParameters.getTargetDataBaseId())));
        parameters.getSourceDs().setDatabaseName(dataArchiveParameters.getSourceDatabaseName());
        parameters.getTargetDs().setDatabaseName(dataArchiveParameters.getTargetDatabaseName());
        parameters.getSourceDs().setConnectionCount(2 * (parameters.getReadThreadCount()
                + parameters.getWriteThreadCount()));
        parameters.getTargetDs().setConnectionCount(parameters.getSourceDs().getConnectionCount());
        parameters.setSyncTableStructure(dataArchiveParameters.getSyncTableStructure());

        Long jobId = publishJob(parameters);
        scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
        scheduleTaskRepository.updateTaskResult(taskEntity.getId(), JsonUtils.toJson(parameters));
        log.info("Publish data-archive job to task framework succeed,scheduleTaskId={},jobIdentity={}",
                taskEntity.getId(),
                jobId);
    }

}
