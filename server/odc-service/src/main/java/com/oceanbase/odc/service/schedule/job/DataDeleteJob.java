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
import java.util.Objects;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.DlmTableUnitParameters;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.model.DlmTableUnitStatistic;
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

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();

        // execute in task framework.
        if (taskFrameworkProperties.isEnabled()) {
            executeInTaskFramework(context);
            return;
        }

        List<DlmTableUnit> dlmTasks = getTaskUnits(taskEntity);
        DataDeleteParameters params = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataDeleteParameters.class);
        executeTask(taskEntity.getId(), dlmTasks, params.getTimeoutMillis());
        TaskStatus taskStatus = getTaskStatus(taskEntity.getId());
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);
    }

    @Override
    public List<DlmTableUnit> splitTask(ScheduleTaskEntity taskEntity) {

        DataDeleteParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataDeleteParameters.class);
        List<DlmTableUnit> dlmTasks = new LinkedList<>();
        parameters.getTables().forEach(table -> {
            String condition = StringUtils.isNotEmpty(table.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(table.getConditionExpression(), parameters.getVariables(),
                            taskEntity.getFireTime())
                    : "";
            DlmTableUnit dlmTableUnit = new DlmTableUnit();
            dlmTableUnit.setScheduleTaskId(taskEntity.getId());
            dlmTableUnit.setDlmTableUnitId(
                    DlmJobIdUtil.generateHistoryJobId(taskEntity.getJobName(), taskEntity.getJobGroup(),
                            taskEntity.getId(),
                            dlmTasks.size()));
            dlmTableUnit.setTableName(table.getTableName());
            dlmTableUnit.setTargetTableName(table.getTargetTableName());
            dlmTableUnit.setSourceDatasourceInfo(getDataSourceInfo(parameters.getDatabaseId()));
            dlmTableUnit.setTargetDatasourceInfo(
                    Objects.isNull(parameters.getTargetDatabaseId()) ? dlmTableUnit.getSourceDatasourceInfo()
                            : getDataSourceInfo(parameters.getTargetDatabaseId()));
            dlmTableUnit.getSourceDatasourceInfo().setQueryTimeout(parameters.getQueryTimeout());
            dlmTableUnit.getTargetDatasourceInfo().setQueryTimeout(parameters.getQueryTimeout());
            dlmTableUnit.setFireTime(taskEntity.getFireTime());
            DlmTableUnitParameters parameter = new DlmTableUnitParameters();
            parameter.setMigrateRule(condition);
            parameter.setCheckMode(CheckMode.MULTIPLE_GET);
            parameter.setGeneratorBatchSize(parameters.getScanBatchSize());
            parameter.setReaderTaskCount(parameters.getReadThreadCount());
            parameter.setWriterTaskCount(parameters.getWriteThreadCount());
            RateLimitConfiguration limiterConfig =
                    limiterService.getByOrderIdOrElseDefaultConfig(Long.parseLong(taskEntity.getJobName()));
            parameter.setReaderBatchSize(limiterConfig.getBatchSize());
            parameter.setWriterBatchSize(limiterConfig.getBatchSize());
            parameter.setMigratePartitions(table.getPartitions());
            dlmTableUnit.setParameters(parameter);
            dlmTableUnit.setStatus(TaskStatus.PREPARING);
            dlmTableUnit.setStatistic(new DlmTableUnitStatistic());
            JobType jobType = parameters.getNeedCheckBeforeDelete() ? JobType.DELETE : JobType.QUICK_DELETE;
            dlmTableUnit.setType(parameters.getDeleteByUniqueKey() ? jobType : JobType.DEIRECT_DELETE);
            dlmTasks.add(dlmTableUnit);
        });
        dlmService.createDlmTableUnits(dlmTasks);
        return dlmTasks;
    }


    private void executeInTaskFramework(JobExecutionContext context) {
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        DataDeleteParameters dataDeleteParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataDeleteParameters.class);
        DLMJobReq parameters = new DLMJobReq();
        parameters.setJobName(taskEntity.getJobName());
        parameters.setScheduleTaskId(taskEntity.getId());
        JobType jobType = dataDeleteParameters.getNeedCheckBeforeDelete() ? JobType.DELETE : JobType.QUICK_DELETE;
        parameters.setJobType(dataDeleteParameters.getDeleteByUniqueKey() ? jobType : JobType.DEIRECT_DELETE);
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
        parameters.setSourceDs(getDataSourceInfo(dataDeleteParameters.getDatabaseId()));
        parameters.setTargetDs(getDataSourceInfo(dataDeleteParameters.getTargetDatabaseId() == null
                ? dataDeleteParameters.getDatabaseId()
                : dataDeleteParameters.getTargetDatabaseId()));
        parameters.getSourceDs().setQueryTimeout(dataDeleteParameters.getQueryTimeout());
        parameters.getTargetDs().setQueryTimeout(dataDeleteParameters.getQueryTimeout());

        Long jobId = publishJob(parameters, dataDeleteParameters.getTimeoutMillis());
        scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
        scheduleTaskRepository.updateTaskResult(taskEntity.getId(), JsonUtils.toJson(parameters));
        log.info("Publish data-delete job to task framework succeed,scheduleTaskId={},jobIdentity={}",
                taskEntity.getId(),
                jobId);
    }


}
