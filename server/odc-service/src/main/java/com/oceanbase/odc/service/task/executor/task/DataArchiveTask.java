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
package com.oceanbase.odc.service.task.executor.task;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.dlm.DLMJobFactory;
import com.oceanbase.odc.service.dlm.DLMJobStore;
import com.oceanbase.odc.service.dlm.DLMTableStructureSynchronizer;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.schedule.job.DLMJobParameters;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.job.Job;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/1/24 11:09
 * @Descripition:
 */

@Slf4j
public class DataArchiveTask extends BaseTask<Boolean> {

    private DLMJobFactory jobFactory;
    private DLMJobStore jobStore;
    private boolean isSuccess = true;
    private double progress = 0.0;
    private Job job;

    @Override
    protected void doInit(JobContext context) {
        jobStore = new DLMJobStore(JobUtils.getMetaDBConnectionConfig());
        jobFactory = new DLMJobFactory(jobStore);
        log.info("Init data-archive job env succeed,jobIdentity={}", context.getJobIdentity());
    }

    @Override
    protected boolean doStart(JobContext context) throws Exception {

        String taskParameters = context.getJobParameters().get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON);
        DLMJobParameters parameters = JsonUtils.fromJson(taskParameters,
                DLMJobParameters.class);

        for (int tableIndex = 0; tableIndex < parameters.getTables().size(); tableIndex++) {
            if (getStatus().isTerminated()) {
                log.info("Job is terminated,jobIdentity={}", context.getJobIdentity());
                break;
            }
            if (parameters.getJobType() == JobType.MIGRATE && !parameters.getSyncTableStructure().isEmpty()) {
                try {
                    DLMTableStructureSynchronizer.sync(
                            DataSourceInfoMapper.toConnectionConfig(parameters.getSourceDs()),
                            DataSourceInfoMapper.toConnectionConfig(parameters.getTargetDs()),
                            parameters.getTables().get(tableIndex).getTableName(), parameters.getSyncTableStructure());
                } catch (Exception e) {
                    log.warn("Failed to sync target table structure,table will be ignored,tableName={}",
                            parameters.getTables().get(tableIndex), e);
                    continue;
                }
            }
            try {
                job = jobFactory.createJob(tableIndex, parameters);
                log.info("Init {} job succeed,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                log.info("{} job start,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
                job.run();
                log.info("{} job finished,DLMJobId={}", job.getJobMeta().getJobType(), job.getJobMeta().getJobId());
            } catch (Throwable e) {
                log.error("{} job failed,DLMJobId={},errorMsg={}", job.getJobMeta().getJobType(),
                        job.getJobMeta().getJobId(),
                        e);
                // set task status to failed if any job failed.
                isSuccess = false;
            }
            progress = (tableIndex + 1.0) / parameters.getTables().size();
        }
        return isSuccess;
    }

    @Override
    protected void doStop() throws Exception {
        job.getJobMeta().setToStop(true);
    }

    @Override
    protected void doClose() throws Exception {
        jobStore.destroy();
    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public Boolean getTaskResult() {
        return isSuccess;
    }
}
