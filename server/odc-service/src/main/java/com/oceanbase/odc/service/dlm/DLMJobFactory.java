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

import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.job.DLMJobParameters;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.configure.LogicTableConfig;
import com.oceanbase.tools.migrator.common.dto.HistoryJob;
import com.oceanbase.tools.migrator.common.enums.ShardingStrategy;
import com.oceanbase.tools.migrator.core.IJobStore;
import com.oceanbase.tools.migrator.core.JobFactory;
import com.oceanbase.tools.migrator.core.JobReq;
import com.oceanbase.tools.migrator.core.meta.ClusterMeta;
import com.oceanbase.tools.migrator.core.meta.TenantMeta;
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

    public Job createJob(DlmTask parameters) {
        HistoryJob historyJob = new HistoryJob();
        historyJob.setId(parameters.getId());
        historyJob.setJobType(parameters.getJobType());
        historyJob.setDateStart("19700101");
        historyJob.setDateEnd(sdf.format(parameters.getFireTime()));
        historyJob.setTaskGeneratorId(parameters.getTaskGeneratorId());
        historyJob.setTableId(-1L);
        historyJob.setPrintSqlTrace(false);
        historyJob.setSourceTable(parameters.getTableName());
        historyJob.setTargetTable(parameters.getTargetTableName());
        LogicTableConfig logicTableConfig = parameters.getLogicTableConfig();
        logicTableConfig.setReaderTaskCount((int) (singleTaskThreadPoolSize * readWriteRatio / (1 + readWriteRatio)));
        logicTableConfig.setWriterTaskCount(singleTaskThreadPoolSize - logicTableConfig.getReaderTaskCount());
        logicTableConfig.setGeneratorBatchSize(defaultScanBatchSize);
        DataSourceInfo sourceInfo = DataSourceInfoMapper.toDataSourceInfo(parameters.getSourceDs());
        DataSourceInfo targetInfo = DataSourceInfoMapper.toDataSourceInfo(parameters.getTargetDs());
        sourceInfo.setConnectionCount(2 * (logicTableConfig.getReaderTaskCount()
                + parameters.getLogicTableConfig().getWriterTaskCount()));
        targetInfo.setConnectionCount(2 * (logicTableConfig.getReaderTaskCount()
                + parameters.getLogicTableConfig().getWriterTaskCount()));
        sourceInfo.setQueryTimeout(taskConnectionQueryTimeout);
        targetInfo.setQueryTimeout(taskConnectionQueryTimeout);
        log.info("Begin to create dlm job,params={}", logicTableConfig);
        // ClusterMeta and TenantMeta used to calculate min limit size.
        JobReq req =
                new JobReq(historyJob, parameters.getLogicTableConfig(), sourceInfo, targetInfo, new ClusterMeta(),
                        new ClusterMeta(), new TenantMeta(), new TenantMeta());
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

        LogicTableConfig logicTableConfig = new LogicTableConfig();
        logicTableConfig.setMigrateRule(params.getTables().get(tableIndex).getConditionExpression());
        logicTableConfig.setCheckMode(CheckMode.MULTIPLE_GET);
        logicTableConfig.setReaderBatchSize(params.getRateLimit().getBatchSize());
        logicTableConfig.setWriterBatchSize(params.getRateLimit().getBatchSize());
        logicTableConfig.setMigrationInsertAction(params.getMigrationInsertAction());
        logicTableConfig.setReaderTaskCount(params.getReadThreadCount());
        logicTableConfig.setWriterTaskCount(params.getWriteThreadCount());
        logicTableConfig.setGeneratorBatchSize(params.getScanBatchSize());


        JobReq req =
                new JobReq(historyJob, logicTableConfig, params.getSourceDs(), params.getTargetDs(),
                        new ClusterMeta(),
                        new ClusterMeta(), new TenantMeta(), new TenantMeta());
        return createJob(req);
    }
}
