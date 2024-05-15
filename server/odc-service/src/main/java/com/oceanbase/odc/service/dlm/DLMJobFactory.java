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

import java.text.SimpleDateFormat;

import com.oceanbase.odc.service.dlm.model.DlmJob;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.job.DLMJobParameters;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
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

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

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
        historyJob.setId(parameters.getId());
        historyJob.setJobType(parameters.getType());
        historyJob.setTableId(-1L);
        historyJob.setPrintSqlTrace(false);
        historyJob.setSourceTable(parameters.getTableName());
        historyJob.setTargetTable(parameters.getTableName());
        JobParameter jobParameter = new JobParameter();
        jobParameter.setReaderTaskCount((int) (singleTaskThreadPoolSize * readWriteRatio / (1 + readWriteRatio)));
        jobParameter.setWriterTaskCount(singleTaskThreadPoolSize - jobParameter.getReaderTaskCount());
        jobParameter.setGeneratorBatchSize(defaultScanBatchSize);
        DataSourceInfo sourceInfo = DataSourceInfoBuilder.build(parameters.getSourceDs());
        DataSourceInfo targetInfo = DataSourceInfoBuilder.build(parameters.getTargetDs());
        sourceInfo.setConnectionCount(2 * (jobParameter.getReaderTaskCount()
                + jobParameter.getWriterTaskCount()));
        targetInfo.setConnectionCount(2 * (jobParameter.getReaderTaskCount()
                + jobParameter.getWriterTaskCount()));
        sourceInfo.setQueryTimeout(taskConnectionQueryTimeout);
        targetInfo.setQueryTimeout(taskConnectionQueryTimeout);
        log.info("Begin to create dlm job,params={}", jobParameter);
        // ClusterMeta and TenantMeta used to calculate min limit size.
        JobReq req = new JobReq(historyJob, sourceInfo, targetInfo);
        return super.createJob(req);
    }

    // adapt to task framework
    public Job createJob(int tableIndex, DLMJobParameters params) {
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

        JobReq req = new JobReq(historyJob, params.getSourceDs(), params.getTargetDs());
        return createJob(req);
    }
}
