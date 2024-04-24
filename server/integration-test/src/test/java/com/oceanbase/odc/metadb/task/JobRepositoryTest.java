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

package com.oceanbase.odc.metadb.task;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.runtime.DatabaseChangeTask;
import com.oceanbase.odc.service.task.util.JobDateUtils;

/**
 * @author yaobin
 * @date 2023-12-21
 * @since 4.2.4
 */
public class JobRepositoryTest extends ServiceTestEnv {

    @Autowired
    private JobRepository jobRepository;

    @Test
    public void test_saveJob() {
        JobEntity je = createJobEntity();
        Assert.assertNotNull(je);
    }

    @Test
    public void test_updateReportResult() {
        JobEntity currentJob = createJobEntity();
        JobEntity jse = new JobEntity();
        jse.setResultJson("");
        jse.setStatus(JobStatus.RUNNING);
        jse.setProgressPercentage(1L);
        jse.setExecutorEndpoint("");
        jse.setLastReportTime(JobDateUtils.getCurrentDate());
        jse.setFinishedTime(JobDateUtils.getCurrentDate());
        int rows = jobRepository.updateReportResult(jse, currentJob.getId(), currentJob.getStatus());
        Assert.assertTrue(rows > 0);
    }



    private JobEntity createJobEntity() {
        JobEntity entity = new JobEntity();
        entity.setJobClass(DatabaseChangeTask.class.getCanonicalName());
        entity.setJobType("ASYNC");
        entity.setExecutionTimes(0);
        entity.setStatus(JobStatus.PREPARING);
        return jobRepository.save(entity);
    }
}
