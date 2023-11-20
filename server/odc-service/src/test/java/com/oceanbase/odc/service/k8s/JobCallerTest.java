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

package com.oceanbase.odc.service.k8s;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.core.task.context.JobContext;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.K8sJobCaller;
import com.oceanbase.odc.service.task.caller.PodTemplateConfig;
import com.oceanbase.odc.service.task.listener.JobCallerListener;

/**
 * @author yaobin
 * @date 2023-11-17
 * @since 4.2.4
 */
@Ignore("manual test this case because k8s cluster is not public environment")
public class JobCallerTest extends BaseJobTest {

    @Test
    public void test_startJob() throws JobException {
        Long exceptedTaskId = 1L;
        String imageName = "perl:5.34.0";
        List<String> cmd = Arrays.asList("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)");

        PodTemplateConfig podConfig = new PodTemplateConfig();
        podConfig.setImage(imageName);
        podConfig.setCommand(cmd);
        podConfig.setNamespace("default");

        JobCaller jobCaller = new K8sJobCaller(getK8sClient(), podConfig);
        jobCaller.getEventPublish().addEventListener(new JobCallerListener(){
            @Override
            protected void startSucceed(Long taskId) {
                Assert.assertEquals(exceptedTaskId, taskId);
            }
        });

        JobContext context = new JobContext();
        context.setTaskId(exceptedTaskId);
        jobCaller.start(context);
    }
}
