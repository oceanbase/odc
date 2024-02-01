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
import com.oceanbase.odc.service.dlm.CloudDLMJobStore;
import com.oceanbase.odc.service.dlm.JobMetaFactoryCopied;
import com.oceanbase.odc.service.schedule.job.InnerDataArchiveJobParameters;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.job.MigrateJob;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/1/24 11:09
 * @Descripition:
 */

@Slf4j
public class DataArchiveTask extends BaseTask<Void> {

    private JobMetaFactoryCopied jobMetaFactory;

    @Override
    protected void doInit(JobContext context) throws Exception {
        jobMetaFactory = new JobMetaFactoryCopied();
        jobMetaFactory.setJobStore(new CloudDLMJobStore());
        log.info("Init data-archive job env succeed,jobIdentity={}", context.getJobIdentity());
    }

    @Override
    protected void doStart(JobContext context) throws Exception {

        String taskParameters = context.getJobParameters().get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON);
        InnerDataArchiveJobParameters parameters = JsonUtils.fromJson(taskParameters,
                InnerDataArchiveJobParameters.class);

        for (int tableIndex = 0; tableIndex < parameters.getTables().size(); tableIndex++) {
            JobMeta jobMeta = jobMetaFactory.create(tableIndex, context.getJobIdentity(), parameters);
            log.info("Init data-archive job succeed,migrateJobId={}", jobMeta.getJobId());
            MigrateJob migrateJob = new MigrateJob();
            migrateJob.setJobMeta(jobMeta);
            try {
                migrateJob.run();
                log.error("Data archive job finished,migrateJobId={}", jobMeta.getJobId());
            } catch (Throwable e) {
                log.error("Data archive job failed,migrateJobId={},errorMsg={}", jobMeta.getJobId(), e);
            }
        }

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    protected void onFail(Throwable e) {

    }

    @Override
    public double getProgress() {
        return 0;
    }

    @Override
    public Void getTaskResult() {
        return null;
    }
}
