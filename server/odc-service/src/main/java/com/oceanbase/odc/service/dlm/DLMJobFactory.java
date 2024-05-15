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

import com.oceanbase.odc.service.dlm.model.DlmJob;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.job.DLMJobReq;
import com.oceanbase.tools.migrator.common.dto.HistoryJob;
import com.oceanbase.tools.migrator.common.dto.JobParameter;
import com.oceanbase.tools.migrator.common.enums.ShardingStrategy;
import com.oceanbase.tools.migrator.core.IJobStore;
import com.oceanbase.tools.migrator.core.JobFactory;
import com.oceanbase.tools.migrator.core.JobReq;
import com.oceanbase.tools.migrator.job.Job;
import com.oceanbase.tools.migrator.task.CheckMode;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/9 14:38
 * @Descripition:
 */
@Slf4j
public class DLMJobFactory extends JobFactory {

    @Setter
    private int singleTaskThreadPoolSize;

    @Setter
    private int taskConnectionQueryTimeout;
    @Setter
    private double readWriteRatio;
    @Setter
    private int defaultScanBatchSize;

    @Setter
    private ShardingStrategy defaultShardingStrategy;

    public DLMJobFactory(IJobStore jobStore) {
        super(jobStore);
    }

    public Job createJob(DlmJob parameters) {
        HistoryJob historyJob = new HistoryJob();
        historyJob.setId(parameters.getDlmJobId());
        historyJob.setJobType(parameters.getType());
        historyJob.setTableId(-1L);
        historyJob.setPrintSqlTrace(false);
        historyJob.setSourceTable(parameters.getTableName());
        historyJob.setTargetTable(parameters.getTableName());
        JobParameter jobParameter = new JobParameter();
        jobParameter.setReaderTaskCount((int) (singleTaskThreadPoolSize * readWriteRatio / (1 + readWriteRatio)));
        jobParameter.setWriterTaskCount(singleTaskThreadPoolSize - jobParameter.getReaderTaskCount());
        jobParameter.setGeneratorBatchSize(defaultScanBatchSize);
        parameters.getSourceDatasourceInfo().setConnectionCount(2 * (jobParameter.getReaderTaskCount()
                + jobParameter.getWriterTaskCount()));
        parameters.getTargetDatasourceInfo().setConnectionCount(2 * (jobParameter.getReaderTaskCount()
                + jobParameter.getWriterTaskCount()));
        parameters.getSourceDatasourceInfo().setQueryTimeout(taskConnectionQueryTimeout);
        parameters.getTargetDatasourceInfo().setQueryTimeout(taskConnectionQueryTimeout);
        log.info("Begin to create dlm job,params={}", jobParameter);
        // ClusterMeta and TenantMeta used to calculate min limit size.
        JobReq req = new JobReq();
        req.setHistoryJob(historyJob);
        req.setSourceDs(parameters.getSourceDatasourceInfo());
        req.setTargetDs(parameters.getTargetDatasourceInfo());
        return super.createJob(req);
    }

    // adapt to task framework
    public Job createJob(int tableIndex, DLMJobReq params) {
        HistoryJob historyJob = new HistoryJob();
        historyJob.setId(DlmJobIdUtil.generateHistoryJobId(params.getJobName(), params.getJobType().name(),
                params.getScheduleTaskId(), tableIndex));
        historyJob.setJobType(params.getJobType());
        historyJob.setTableId(-1L);
        historyJob.setPrintSqlTrace(false);
        historyJob.setSourceTable(params.getTables().get(tableIndex).getTableName());
        historyJob.setTargetTable(params.getTables().get(tableIndex).getTargetTableName());

        JobParameter jobParameter = new JobParameter();
        jobParameter.setMigrateRule(params.getTables().get(tableIndex).getConditionExpression());
        jobParameter.setCheckMode(CheckMode.MULTIPLE_GET);
        jobParameter.setMigrationInsertAction(params.getMigrationInsertAction());
        jobParameter.setReaderTaskCount(params.getReadThreadCount());
        jobParameter.setWriterTaskCount(params.getWriteThreadCount());
        jobParameter.setGeneratorBatchSize(params.getScanBatchSize());
        jobParameter.setReaderBatchSize(params.getRateLimit().getBatchSize());
        jobParameter.setWriterBatchSize(params.getRateLimit().getBatchSize());

        JobReq req = new JobReq();
        req.setHistoryJob(historyJob);
        req.setSourceDs(params.getSourceDs());
        req.setTargetDs(params.getTargetDs());
        return createJob(req);
    }
}
