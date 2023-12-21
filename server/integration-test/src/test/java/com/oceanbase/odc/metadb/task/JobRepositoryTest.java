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
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.executor.sampletask.SampleTask;

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


    private JobEntity createJobEntity() {
        JobEntity entity = new JobEntity();
        entity.setJobClass(SampleTask.class.getCanonicalName());
        entity.setJobType("SAMPLE");
        entity.setExecutionTimes(0);
        entity.setScheduleTimes(1);
        entity.setStatus(TaskStatus.PREPARING);
        entity.setFinished(1);
        return jobRepository.save(entity);
    }
}
