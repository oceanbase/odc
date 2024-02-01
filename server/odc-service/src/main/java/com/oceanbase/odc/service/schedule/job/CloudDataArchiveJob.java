/*
 * Copyright (c) 2024 OceanBase.
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

import java.util.HashMap;
import java.util.Map;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.task.DataArchiveTask;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/1/25 10:21
 * @Descripition:
 */
@Slf4j
public class CloudDataArchiveJob extends DataArchiveJob {
    @Override
    public void execute(JobExecutionContext context) {

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        DataArchiveParameters dataArchiveParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);
        InnerDataArchiveJobParameters innerDataArchiveJobParameters = new InnerDataArchiveJobParameters();
        innerDataArchiveJobParameters.setTables(dataArchiveParameters.getTables());
        for (DataArchiveTableConfig tableConfig : innerDataArchiveJobParameters.getTables()) {
            tableConfig.setConditionExpression(StringUtils.isNotEmpty(tableConfig.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(tableConfig.getConditionExpression(),
                            dataArchiveParameters.getVariables(),
                            context.getFireTime())
                    : "");
        }
        innerDataArchiveJobParameters
                .setSourceDs(databaseService.findDataSourceForConnectById(dataArchiveParameters.getSourceDatabaseId()));
        innerDataArchiveJobParameters
                .setSourceDs(databaseService.findDataSourceForConnectById(dataArchiveParameters.getTargetDataBaseId()));
        innerDataArchiveJobParameters.setDeleteAfterMigration(dataArchiveParameters.isDeleteAfterMigration());
        innerDataArchiveJobParameters.setMigrationInsertAction(dataArchiveParameters.getMigrationInsertAction());
        innerDataArchiveJobParameters.setNeedPrintSqlTrace(dataArchiveParameters.isNeedPrintSqlTrace());
        innerDataArchiveJobParameters.setRateLimit(dataArchiveParameters.getRateLimit());
        innerDataArchiveJobParameters.setWriteThreadCount(dataArchiveParameters.getWriteThreadCount());
        innerDataArchiveJobParameters.setReadThreadCount(dataArchiveParameters.getReadThreadCount());
        innerDataArchiveJobParameters.setQueryTimeout(dataArchiveParameters.getQueryTimeout());
        innerDataArchiveJobParameters.setShardingStrategy(dataArchiveParameters.getShardingStrategy());
        innerDataArchiveJobParameters.setScanBatchSize(dataArchiveParameters.getScanBatchSize());

        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON,
                JsonUtils.toJson(innerDataArchiveJobParameters));

        DefaultJobDefinition jobDefinition = DefaultJobDefinition.builder().jobClass(DataArchiveTask.class)
                .jobType(JobType.DATA_ARCHIVE.name())
                .jobParameters(jobData)
                .build();
        JobScheduler jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        Long jobId = jobScheduler.scheduleJobNow(jobDefinition);

    }
}
