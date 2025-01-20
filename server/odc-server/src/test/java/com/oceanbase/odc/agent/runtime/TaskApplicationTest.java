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
package com.oceanbase.odc.agent.runtime;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2023-12-14
 * @since 4.2.4
 */
public class TaskApplicationTest {
    @Test
    public void test_executeDatabaseChangeTask_run() {
        Long exceptedTaskId = System.currentTimeMillis();
        JobIdentity jobIdentity = JobIdentity.of(exceptedTaskId);
        setJobContextInSystemProperty(jobIdentity);
        startTaskApplication();
        assertRunningResult(jobIdentity);
    }


    private void setJobContextInSystemProperty(JobIdentity jobIdentity) {
        Map<String, String> envMap = buildConfig(jobIdentity);
        JobUtils.encryptEnvironments(envMap);
        envMap.forEach(System::setProperty);
    }

    private void assertRunningResult(JobIdentity ji) {

        try {
            long endTime = System.currentTimeMillis() + 20000;
            TaskExecutor taskExecutor = ThreadPoolTaskExecutor.getInstance();
            while (System.currentTimeMillis() < endTime) {
                if (!taskExecutor.taskExist(ji)) {
                    Thread.sleep(1000);
                    continue;
                }
                TaskRuntimeInfo taskRuntimeInfo = taskExecutor.getTaskRuntimeInfo(ji);
                TaskContainer<?> task = taskRuntimeInfo.getTaskContainer();
                task.taskMonitor.markLogMetaCollected();
                if (TaskStatus.DONE == task.getStatus()) {
                    return;
                } else {
                    Thread.sleep(1000);
                }
            }
            // not check pass
            Assert.assertFalse(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private Map<String, String> buildConfig(JobIdentity jobIdentity) {
        Map<String, String> ret = new HashMap<>();
        ret.put(JobEnvKeyConstants.ODC_TASK_RUN_MODE, "K8S");
        ret.put(JobEnvKeyConstants.ODC_BOOT_MODE, "TASK_EXECUTOR");
        ret.put(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID, "1");
        DefaultJobContext defaultJobContext = new DefaultJobContext();
        defaultJobContext.setJobClass(SimpleTask.class.getName());
        defaultJobContext.setJobIdentity(jobIdentity);
        ret.put(JobEnvKeyConstants.ODC_JOB_CONTEXT, JsonUtils.toJson(defaultJobContext));
        ret.put(JobEnvKeyConstants.ODC_LOG_DIRECTORY, "log");
        ret.put(JobEnvKeyConstants.ODC_EXECUTOR_PORT, "8080");
        ret.put(JobEnvKeyConstants.REPORT_ENABLED, "false");
        return ret;
    }

    private void startTaskApplication() {
        new Thread(() -> new TaskApplication().run(null)).start();
    }

}
