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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.dlm.JobMetaFactory;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.tools.migrator.core.JobReq;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.job.MigrateJob;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/12/27 13:59
 * @Descripition:
 */
@Slf4j
public class DataArchiveTask extends BaseTask {

    private MigrateJob migrateJob;

    @Override
    protected void onInit() {

    }

    @Override
    protected void onStart() {
        Map<String, String> jobData = getJobContext().getJobParameters();
        List<JobReq> jobReqs = JsonUtils.fromJson(jobData.get(JobDataMapConstants.META_DB_TASK_PARAMETER),
                new TypeReference<List<JobReq>>() {});
        JobMetaFactory jobMetaFactory = new JobMetaFactory();
        for (JobReq jobReq : jobReqs) {
            JobMeta jobMeta = null;
            try {
                jobMeta = jobMetaFactory.create(jobReq);
            } catch (Exception e) {
                log.warn("DataArchiveTask create jobMeta failed,jobId={}", jobReq.getHistoryJob().getId(), e);
            }
            migrateJob = new MigrateJob();
            migrateJob.setJobMeta(jobMeta);
            try {
                migrateJob.run();
            } catch (Exception e) {
                log.warn("DataArchiveTask run failed,jobId={}", jobMeta.getJobId(), e);
            }
        }
    }

    @Override
    protected void onStop() {
        migrateJob.getJobMeta().setToStop(true);
    }

    @Override
    protected void onFail(Exception e) {

    }

    @Override
    public double getProgress() {
        return 0;
    }

    @Override
    public Serializable getTaskResult() {
        return null;
    }
}
