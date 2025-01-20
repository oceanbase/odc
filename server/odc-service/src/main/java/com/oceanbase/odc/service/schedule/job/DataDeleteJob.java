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

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.tools.migrator.common.enums.JobType;

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
        executeInTaskFramework(context);
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
        parameters.setShardingStrategy(dataDeleteParameters.getShardingStrategy());

        Long jobId =
                publishJob(parameters, dataDeleteParameters.getTimeoutMillis(), dataDeleteParameters.getDatabaseId());
        scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
        scheduleTaskRepository.updateTaskResult(taskEntity.getId(), JsonUtils.toJson(parameters));
        log.info("Publish data-delete job to task framework succeed,scheduleTaskId={},jobIdentity={}",
                taskEntity.getId(),
                jobId);
    }


}
