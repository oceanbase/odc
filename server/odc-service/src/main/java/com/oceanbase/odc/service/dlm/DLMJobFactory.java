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

import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.DlmTableUnitParameters;
import com.oceanbase.tools.migrator.common.dto.HistoryJob;
import com.oceanbase.tools.migrator.core.JobFactory;
import com.oceanbase.tools.migrator.core.JobReq;
import com.oceanbase.tools.migrator.core.store.IJobStore;
import com.oceanbase.tools.migrator.job.Job;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/9 14:38
 * @Descripition:
 */
@Slf4j
public class DLMJobFactory extends JobFactory {

    public DLMJobFactory(IJobStore jobStore) {
        super(jobStore);
    }

    public Job createJob(DlmTableUnit parameters) {
        HistoryJob historyJob = new HistoryJob();
        historyJob.setId(parameters.getDlmTableUnitId());
        historyJob.setJobType(parameters.getType());
        historyJob.setPrintSqlTrace(false);
        historyJob.setSourceTable(parameters.getTableName());
        historyJob.setTargetTable(parameters.getTargetTableName());
        historyJob.setJobParameter(parameters.getParameters());
        DlmTableUnitParameters jobParameter = parameters.getParameters();
        parameters.getSourceDatasourceInfo().setConnectionCount(2 * (jobParameter.getReaderTaskCount()
                + jobParameter.getWriterTaskCount()));
        parameters.getTargetDatasourceInfo().setConnectionCount(2 * (jobParameter.getReaderTaskCount()
                + jobParameter.getWriterTaskCount()));
        log.info("Begin to create dlm job,params={}", historyJob);
        JobReq req = new JobReq();
        req.setHistoryJob(historyJob);
        req.setSourceDs(parameters.getSourceDatasourceInfo());
        req.setTargetDs(parameters.getTargetDatasourceInfo());
        req.setSourceLimitConfig(parameters.getSourceLimitConfig());
        req.setTargetLimitConfig(parameters.getTargetLimitConfig());
        return super.createJob(req);
    }
}
