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
package com.oceanbase.odc.service.task.supervisor;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

/**
 * @author longpeng.zlp
 * @date 2025/1/6 15:07
 */
public class TaskSupervisorTest {
    private SupervisorEndpoint supervisorEndpoint = new SupervisorEndpoint(SystemUtils.getLocalIpAddress(), 9999);
    private DefaultJobContext jobContext = new DefaultJobContext();
    private ProcessConfig processConfig = new ProcessConfig();
    private TaskSupervisor taskSupervisor;

    @Before
    public void init() {
        jobContext.setJobIdentity(JobIdentity.of(1024L));
        jobContext.setJobClass(TaskSupervisor.class.getName());
        jobContext.setJobParameters(new HashMap<>());
        jobContext.setJobProperties(new HashMap<>());
        processConfig.setJvmXmsMB(1024);
        processConfig.setJvmXmxMB(1024);
        processConfig.setEnvironments(new HashMap<>());
        taskSupervisor = new TaskSupervisor(supervisorEndpoint, TaskSupervisor.class.getName());
        taskSupervisor.setJobInfoSerializer(null);
    }


    @Test
    public void testTaskSupervisorStartTask() throws JobException {
        ExecutorEndpoint executorEndpoint = taskSupervisor.startTask(jobContext, processConfig);
        Assert.assertEquals(executorEndpoint.getSupervisorPort().intValue(), 9999);
        Assert.assertEquals(executorEndpoint.getHost(), SystemUtils.getLocalIpAddress());
        Assert.assertTrue(taskSupervisor.isTaskAlive(TaskSupervisor.getExecutorIdentifier(executorEndpoint)));
        Assert.assertTrue(taskSupervisor.destroyTask(executorEndpoint, jobContext));
        Assert.assertFalse(taskSupervisor.isTaskAlive(TaskSupervisor.getExecutorIdentifier(executorEndpoint)));
    }

    @Test
    public void testGeneratePort() {
        ProcessConfig tmp = new ProcessConfig();
        tmp.setEnvironments(new HashMap<>());
        // not enabled,return -1
        Assert.assertEquals(-1, taskSupervisor.tryGenerateListenPortToEnv(tmp));
        // enable pull mode
        tmp.getEnvironments().put(JobEnvKeyConstants.REPORT_ENABLED, "false");
        int port = taskSupervisor.tryGenerateListenPortToEnv(tmp);
        Assert.assertTrue(port > 0);
        // has put it, return config value
        Assert.assertEquals(port, taskSupervisor.tryGenerateListenPortToEnv(tmp));
    }

    // test main
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            Thread.sleep(1000);
        }
    }
}
