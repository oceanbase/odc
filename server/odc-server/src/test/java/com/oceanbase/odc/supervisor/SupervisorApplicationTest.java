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
package com.oceanbase.odc.supervisor;

/**
 * @author longpeng.zlp
 * @date 2024/12/9 15:59
 */
/**
 * @author longpeng.zlp
 * @date 2024/12/9 15:59
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.dummy.DummyTask;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.supervisor.JobEventHandler;
import com.oceanbase.odc.service.task.supervisor.PortDetector;
import com.oceanbase.odc.service.task.supervisor.TaskCallerResult;
import com.oceanbase.odc.service.task.supervisor.TaskSupervisorJobCaller;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.proxy.LocalTaskSupervisorProxy;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;
import com.oceanbase.odc.supervisor.runtime.SupervisorApplication;

/**
 * @author longpeng.zlp
 * @date 2024/12/9 15:42
 */
public class SupervisorApplicationTest {
    private SupervisorEndpoint localSupervisorEndpoint;
    private SupervisorEndpoint remoteSupervisorEndpoint;
    // supervisor application
    private SupervisorApplication supervisorApplication;
    // local task supervisor
    private LocalTaskSupervisorProxy localTaskSupervisorProxy;

    private TaskSupervisorJobCaller taskSupervisorJobCaller;

    private JobContext jobContext;

    private ProcessConfig processConfig;

    @Before
    public void setUp() {
        PortDetector portDetector = PortDetector.getInstance();
        int allocatePort = portDetector.getPort();
        supervisorApplication = new SupervisorApplication(allocatePort);
        supervisorApplication.start(new String[] {});
        String ip = SystemUtils.getLocalIpAddress();
        remoteSupervisorEndpoint = new SupervisorEndpoint(ip, allocatePort);
        localSupervisorEndpoint = new SupervisorEndpoint(ip, 8989);
        localTaskSupervisorProxy =
                new LocalTaskSupervisorProxy(localSupervisorEndpoint, JobConstants.ODC_AGENT_CLASS_NAME);
        JobEventHandler jobEventHandler = Mockito.mock(JobEventHandler.class);
        taskSupervisorJobCaller =
                new TaskSupervisorJobCaller(jobEventHandler, localTaskSupervisorProxy, new TaskExecutorClient());
        jobContext = createJobContext();
        processConfig = createProcessConfig();
    }

    @After
    public void clear() {
        supervisorApplication.stop();
        supervisorApplication.waitStop();
    }

    @Test
    public void testRemoteTaskOperation() throws JobException, IOException {
        ExecutorEndpoint executorEndpoint =
                taskSupervisorJobCaller.startTask(remoteSupervisorEndpoint, jobContext, processConfig);
        waitPortAvailable(executorEndpoint);
        Assert.assertEquals(executorEndpoint.getSupervisorPort(), remoteSupervisorEndpoint.getPort());
        // local and remote can see job alive
        Assert.assertTrue(localTaskSupervisorProxy.isTaskAlive(remoteSupervisorEndpoint, executorEndpoint, jobContext));
        Assert.assertTrue(localTaskSupervisorProxy.isTaskAlive(localSupervisorEndpoint, executorEndpoint, jobContext));
        // verify supervisor alive
        Assert.assertTrue(localTaskSupervisorProxy.isSupervisorAlive(remoteSupervisorEndpoint));
        Assert.assertTrue(localTaskSupervisorProxy.isSupervisorAlive(localSupervisorEndpoint));
        // stop task, sync
        TaskCallerResult taskCallerResult =
                taskSupervisorJobCaller.destroyTask(remoteSupervisorEndpoint, executorEndpoint, jobContext);
        Assert.assertTrue(taskCallerResult.getSucceed());
        Assert.assertTrue(localTaskSupervisorProxy.isSupervisorAlive(remoteSupervisorEndpoint));
        // verify finish
        taskCallerResult = taskSupervisorJobCaller.finish(remoteSupervisorEndpoint, executorEndpoint, jobContext);
        Assert.assertTrue(taskCallerResult.getSucceed());
        // check task stopped
        Assert.assertFalse(
                localTaskSupervisorProxy.isTaskAlive(remoteSupervisorEndpoint, executorEndpoint, jobContext));
    }

    @Test
    public void testRemoteTaskOperationWithSupervisorNotAlive() throws JobException, IOException, InterruptedException {
        ExecutorEndpoint executorEndpoint =
                taskSupervisorJobCaller.startTask(remoteSupervisorEndpoint, jobContext, processConfig);
        Assert.assertEquals(executorEndpoint.getSupervisorPort(), remoteSupervisorEndpoint.getPort());
        waitPortAvailable(executorEndpoint);
        // shutdown remote supervisor
        supervisorApplication.stop();
        supervisorApplication.waitStop();
        // stop task
        TaskCallerResult taskCallerResult =
                taskSupervisorJobCaller.stopTaskDirectly(executorEndpoint, jobContext);
        // still can stop
        Assert.assertTrue(taskCallerResult.getSucceed());
        // check task stopped
        Assert.assertFalse(localTaskSupervisorProxy.isSupervisorAlive(remoteSupervisorEndpoint));
        // verify finish
        taskCallerResult = taskSupervisorJobCaller.finish(remoteSupervisorEndpoint, executorEndpoint, jobContext);
        Assert.assertFalse(taskCallerResult.getSucceed());
        // stop use local supervisor
        Assert.assertTrue(localTaskSupervisorProxy.isTaskAlive(localSupervisorEndpoint, executorEndpoint, jobContext));
        taskCallerResult = taskSupervisorJobCaller.destroyTask(localSupervisorEndpoint, executorEndpoint, jobContext);
        Assert.assertFalse(localTaskSupervisorProxy.isTaskAlive(localSupervisorEndpoint, executorEndpoint, jobContext));

    }


    @Test
    public void testRemoteTaskOperationWithTaskNotAlive() throws JobException, IOException {
        ExecutorEndpoint executorEndpoint =
                taskSupervisorJobCaller.startTask(remoteSupervisorEndpoint, jobContext, processConfig);
        waitPortAvailable(executorEndpoint);
        Assert.assertEquals(executorEndpoint.getSupervisorPort(), remoteSupervisorEndpoint.getPort());
        // shutdown remote task
        localTaskSupervisorProxy.destroyTask(localSupervisorEndpoint, executorEndpoint, jobContext);
        Assert.assertFalse(localTaskSupervisorProxy.isTaskAlive(localSupervisorEndpoint, executorEndpoint, jobContext));
        // stop task
        TaskCallerResult taskCallerResult =
                taskSupervisorJobCaller.destroyTask(remoteSupervisorEndpoint, executorEndpoint, jobContext);
        // still can stop
        Assert.assertTrue(taskCallerResult.getSucceed());
        // check task stopped
        Assert.assertTrue(localTaskSupervisorProxy.isSupervisorAlive(remoteSupervisorEndpoint));
        // verify finish
        taskCallerResult = taskSupervisorJobCaller.finish(remoteSupervisorEndpoint, executorEndpoint, jobContext);
        Assert.assertTrue(taskCallerResult.getSucceed());
    }

    private ProcessConfig createProcessConfig() {
        ProcessConfig ret = new ProcessConfig();
        ret.setJvmXmxMB(1024);
        ret.setJvmXmsMB(1024);
        Map<String, String> envMap = new HashMap<String, String>() {
            {
                put(JobEnvKeyConstants.REPORT_ENABLED, Boolean.FALSE.toString());
                put(JobEnvKeyConstants.ODC_LOG_DIRECTORY, ".");
                put(JobEnvKeyConstants.ODC_EXECUTOR_USER_ID, "1024");
                put(JobEnvKeyConstants.ODC_BOOT_MODE, "TASK_EXECUTOR");
                put(JobEnvKeyConstants.ODC_TASK_RUN_MODE, "PROCESS");
            }
        };
        JobUtils.encryptEnvironments(envMap);
        ret.setEnvironments(envMap);
        return ret;
    }


    private JobContext createJobContext() {
        DefaultJobContext jobContext = new DefaultJobContext();
        jobContext.setJobClass(DummyTask.class.getName());
        jobContext.setJobIdentity(JobIdentity.of(1024L));
        jobContext.setJobProperties(new HashMap<String, String>() {
            {
                put("prop1", "valueProp1");
            }
        });
        jobContext.setJobParameters(new HashMap<String, String>() {
            {
                put("param1", "valueParam1");
            }
        });
        return jobContext;
    }

    private static boolean isPortAvailable(ExecutorEndpoint executorEndpoint) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(executorEndpoint.getHost(), executorEndpoint.getExecutorPort()), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void waitPortAvailable(ExecutorEndpoint executorEndpoint) {
        while (!isPortAvailable(executorEndpoint)) {
            try {
                Thread.sleep(500);
            } catch (Throwable e) {
            }
        }
    }
}

