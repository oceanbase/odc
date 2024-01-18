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

package com.oceanbase.odc.service.task;

import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.K8sJobCaller;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-17
 * @since 4.2.4
 */
@Slf4j
@Ignore("manual test this case because k8s cluster is not public environment")
public class JobCallerTest extends BaseJobTest {

    @Test
    public void test_startJob() throws JobException {
        Long exceptedTaskId = System.currentTimeMillis();
        PodConfig podConfig = new PodConfig();
        podConfig.setImage(getImageName());
        podConfig.setCommand(getCmd());
        podConfig.setNamespace("default");

        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);
        JobCaller jobCaller = new K8sJobCaller(getK8sJobClient(), podConfig);

        DefaultJobContext context = new DefaultJobContext();
        context.setJobIdentity(jobIdentity);
        jobCaller.start(context);
        context.setJobIdentity(jobIdentity);
        jobCaller.stop(JobIdentity.of(exceptedTaskId));
    }


}
