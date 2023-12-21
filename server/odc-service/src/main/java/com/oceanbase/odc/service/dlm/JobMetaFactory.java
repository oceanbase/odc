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
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.configure.LogicTableConfig;
import com.oceanbase.tools.migrator.common.dto.HistoryJob;
import com.oceanbase.tools.migrator.common.enums.JobStatus;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.common.enums.ShardingStrategy;
import com.oceanbase.tools.migrator.core.AbstractJobMetaFactory;
import com.oceanbase.tools.migrator.core.JobReq;
import com.oceanbase.tools.migrator.core.meta.ClusterMeta;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.core.meta.TenantMeta;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/4/18 16:10
 * @Descripition:
 */
@Slf4j
public class JobMetaFactory extends AbstractJobMetaFactory {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private int singleTaskThreadPoolSize;

    private int taskConnectionQueryTimeout;
    private double readWriteRatio;

    private int defaultScanBatchSize;

    private ShardingStrategy defaultShardingStrategy;

    public JobMeta create(DlmTask parameters) throws Exception {
        HistoryJob historyJob = new HistoryJob();
        historyJob.setId(parameters.getId());
        historyJob.setJobType(JobType.MIGRATE);
        historyJob.setJobStatus(JobStatus.RUNNING);
        historyJob.setDateStart("19700101");
        historyJob.setDateEnd(sdf.format(parameters.getFireTime()));
        historyJob.setTaskGeneratorId(parameters.getTaskGeneratorId());
        historyJob.setTableId(-1L);
        historyJob.setPrintSqlTrace(false);
        historyJob.setSourceTable(parameters.getTableName());
        historyJob.setTargetTable(parameters.getTableName());
        LogicTableConfig logicTableConfig = parameters.getLogicTableConfig();
        logicTableConfig.setReaderTaskCount((int) (singleTaskThreadPoolSize * readWriteRatio / (1 + readWriteRatio)));
        logicTableConfig.setWriterTaskCount(singleTaskThreadPoolSize - logicTableConfig.getReaderTaskCount());
        logicTableConfig.setGeneratorBatchSize(defaultScanBatchSize);
        DataSourceInfo sourceInfo = DataSourceInfoBuilder.build(parameters.getSourceDs());
        DataSourceInfo targetInfo = DataSourceInfoBuilder.build(parameters.getTargetDs());
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
        JobMeta jobMeta = super.create(req);
        jobMeta.setShardingStrategy(defaultShardingStrategy);
        return jobMeta;
    }

    public void setReadWriteRatio(double readWriteRatio) {
        this.readWriteRatio = readWriteRatio;
    }

    public void setSingleTaskThreadPoolSize(int singleTaskThreadPoolSize) {
        this.singleTaskThreadPoolSize = singleTaskThreadPoolSize;
    }

    public void setTaskConnectionQueryTimeout(int taskConnectionQueryTimeout) {
        this.taskConnectionQueryTimeout = taskConnectionQueryTimeout;
    }

    public void setDefaultShardingStrategy(ShardingStrategy defaultShardingStrategy) {
        this.defaultShardingStrategy = defaultShardingStrategy;
    }

    public void setDefaultScanBatchSize(int defaultScanBatchSize) {
        this.defaultScanBatchSize = defaultScanBatchSize;
    }

}
