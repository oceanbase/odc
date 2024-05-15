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

import java.util.LinkedList;
import java.util.List;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.tools.migrator.common.configure.LogicTableConfig;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.task.CheckMode;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/7/13 17:24
 * @Descripition:
 */

@Slf4j
public class DataDeleteJob extends AbstractDlmJob {

    @Override
    public void executeJob(JobExecutionContext context) {

        jobThread = Thread.currentThread();

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();

        // execute in task framework.
        if (taskFrameworkProperties.isEnabled()) {
            executeInTaskFramework(context);
            return;
        }

        List<DlmTask> dlmTasks = getTaskUnits(taskEntity);

        executeTask(taskEntity.getId(), dlmTasks);
        TaskStatus taskStatus = getTaskStatus(dlmTasks);
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);
    }

    @Override
    public List<DlmTask> splitTask(ScheduleTaskEntity taskEntity) {

        DataDeleteParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataDeleteParameters.class);
        List<DlmTask> dlmTasks = new LinkedList<>();
        parameters.getTables().forEach(table -> {
            String condition = StringUtils.isNotEmpty(table.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(table.getConditionExpression(), parameters.getVariables(),
                            taskEntity.getFireTime())
                    : "";
            DlmTask dlmTask = new DlmTask();

            dlmTask.setId(DlmJobIdUtil.generateHistoryJobId(taskEntity.getJobName(), taskEntity.getJobGroup(),
                    taskEntity.getId(),
                    dlmTasks.size()));
            dlmTask.setTableName(table.getTableName());
            dlmTask.setSourceDatabaseId(parameters.getDatabaseId());
            dlmTask.setTargetDatabaseId(parameters.getDatabaseId());
            dlmTask.setFireTime(taskEntity.getFireTime());

            LogicTableConfig logicTableConfig = new LogicTableConfig();
            logicTableConfig.setMigrateRule(condition);
            logicTableConfig.setCheckMode(CheckMode.MULTIPLE_GET);
            dlmTask.setLogicTableConfig(logicTableConfig);
            dlmTask.setStatus(TaskStatus.PREPARING);
            dlmTask.setJobType(parameters.getDeleteByUniqueKey() ? JobType.QUICK_DELETE : JobType.DEIRECT_DELETE);
            dlmTasks.add(dlmTask);
        });
        return dlmTasks;
    }


    private void executeInTaskFramework(JobExecutionContext context) {
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        DataDeleteParameters dataDeleteParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataDeleteParameters.class);
        DLMJobParameters parameters = new DLMJobParameters();
        parameters.setJobName(taskEntity.getJobName());
        parameters.setScheduleTaskId(taskEntity.getId());
        parameters.setJobType(JobType.DELETE);
        parameters.setTables(dataDeleteParameters.getTables());
        for (DataArchiveTableConfig tableConfig : parameters.getTables()) {
            tableConfig.setConditionExpression(StringUtils.isNotEmpty(tableConfig.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(tableConfig.getConditionExpression(),
                            dataDeleteParameters.getVariables(),
                            context.getFireTime())
                    : "");
        }
        parameters.setNeedPrintSqlTrace(dataDeleteParameters.isNeedPrintSqlTrace());
        parameters
                .setRateLimit(limiterService.getByOrderIdOrElseDefaultConfig(Long.parseLong(taskEntity.getJobName())));
        parameters.setWriteThreadCount(dataDeleteParameters.getWriteThreadCount());
        parameters.setReadThreadCount(dataDeleteParameters.getReadThreadCount());
        parameters.setScanBatchSize(dataDeleteParameters.getScanBatchSize());
        parameters
                .setSourceDs(DataSourceInfoMapper.toDataSourceInfo(
                        databaseService.findDataSourceForConnectById(dataDeleteParameters.getDatabaseId())));
        parameters
                .setTargetDs(DataSourceInfoMapper.toDataSourceInfo(
                        databaseService.findDataSourceForConnectById(dataDeleteParameters.getDatabaseId())));
        parameters.getSourceDs().setDatabaseName(dataDeleteParameters.getDatabaseName());
        parameters.getTargetDs().setDatabaseName(dataDeleteParameters.getDatabaseName());
        parameters.getSourceDs().setConnectionCount(2 * (parameters.getReadThreadCount()
                + parameters.getWriteThreadCount()));
        parameters.getTargetDs().setConnectionCount(parameters.getSourceDs().getConnectionCount());

        Long jobId = publishJob(parameters);
        scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
        scheduleTaskRepository.updateTaskResult(taskEntity.getId(), JsonUtils.toJson(parameters));
        log.info("Publish data-delete job to task framework succeed,scheduleTaskId={},jobIdentity={}",
                taskEntity.getId(),
                jobId);
    }


}
