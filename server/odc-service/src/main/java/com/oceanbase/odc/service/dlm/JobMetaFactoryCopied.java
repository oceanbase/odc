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
package com.oceanbase.odc.service.dlm;

import com.oceanbase.odc.service.schedule.job.DLMJobParameters;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.tools.migrator.common.configure.LogicTableConfig;
import com.oceanbase.tools.migrator.common.dto.HistoryJob;
import com.oceanbase.tools.migrator.common.enums.JobStatus;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.core.AbstractJobMetaFactory;
import com.oceanbase.tools.migrator.core.JobReq;
import com.oceanbase.tools.migrator.core.meta.ClusterMeta;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.core.meta.TenantMeta;
import com.oceanbase.tools.migrator.task.CheckMode;

/**
 * @Author：tinker
 * @Date: 2024/2/1 15:06
 * @Descripition:
 */
public class JobMetaFactoryCopied extends AbstractJobMetaFactory {


    public JobMeta create(int tableIndex, JobIdentity jobIdentity, DLMJobParameters parameters)
            throws Exception {
        HistoryJob historyJob = new HistoryJob();
        historyJob.setId(String.format("%s-%s-%s", JobType.MIGRATE, jobIdentity, tableIndex));
        historyJob.setJobType(JobType.MIGRATE);
        historyJob.setJobStatus(JobStatus.RUNNING);
        historyJob.setTableId(-1L);
        historyJob.setPrintSqlTrace(false);
        historyJob.setSourceTable(parameters.getTables().get(tableIndex).getTableName());
        historyJob.setTargetTable(parameters.getTables().get(tableIndex).getTargetTableName());

        LogicTableConfig logicTableConfig = new LogicTableConfig();
        logicTableConfig.setMigrateRule(parameters.getTables().get(tableIndex).getConditionExpression());
        logicTableConfig.setCheckMode(CheckMode.MULTIPLE_GET);
        logicTableConfig.setReaderBatchSize(parameters.getRateLimit().getBatchSize());
        logicTableConfig.setWriterBatchSize(parameters.getRateLimit().getBatchSize());
        logicTableConfig.setMigrationInsertAction(parameters.getMigrationInsertAction());
        logicTableConfig.setReaderTaskCount(parameters.getReadThreadCount());
        logicTableConfig.setWriterTaskCount(parameters.getWriteThreadCount());
        logicTableConfig.setGeneratorBatchSize(parameters.getScanBatchSize());


        JobReq req =
                new JobReq(historyJob, logicTableConfig, parameters.getSourceDs(), parameters.getTargetDs(),
                        new ClusterMeta(),
                        new ClusterMeta(), new TenantMeta(), new TenantMeta());
        JobMeta jobMeta = super.create(req);
        jobMeta.setShardingStrategy(parameters.getShardingStrategy());
        return jobMeta;
    }

}
