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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.K8sJobCaller;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.listener.JobCallerListener;

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

        JobCaller jobCaller = new K8sJobCaller(getK8sJobClient(), podConfig);
        jobCaller.getEventPublisher().addEventListener(new JobCallerListener() {
            @Override
            protected void startSucceed(Long taskId) {
                Assert.assertEquals(exceptedTaskId, taskId);
            }

            @Override
            protected void stopSucceed(Long taskId) {
                Assert.assertEquals(exceptedTaskId, taskId);
            }
        });

        DefaultJobContext context = new DefaultJobContext();
        context.setTaskId(exceptedTaskId);
        jobCaller.start(context);
        jobCaller.stop(exceptedTaskId);
    }

    @Test
    public void test_startJob_failed() throws JobException {
        Long exceptedTaskId = System.currentTimeMillis();
        PodConfig podConfig = new PodConfig();
        podConfig.setImage(getImageName());
        podConfig.setCommand(getCmd());
        podConfig.setNamespace("default");

        JobCaller jobCaller = new K8sJobCaller(getK8sJobClient(), podConfig);
        jobCaller.getEventPublisher().addEventListener(new JobCallerListener() {
            @Override
            protected void startFailed(Long taskId, Exception ex) {
                log.info(ex.getMessage());
                Assert.assertTrue(ex.getMessage().contains("AlreadyExists"));
            }
        });

        DefaultJobContext context = new DefaultJobContext();
        context.setTaskId(exceptedTaskId);
        jobCaller.start(context);
        // double start same task
        try {
            jobCaller.start(context);
        } catch (Exception ex) {
            // ignore
        } finally {
            jobCaller.stop(exceptedTaskId);
        }

    }
}
