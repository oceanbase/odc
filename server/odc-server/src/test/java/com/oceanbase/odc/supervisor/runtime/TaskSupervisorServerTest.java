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
package com.oceanbase.odc.supervisor.runtime;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.MapUtils;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.dummy.DummyTask;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.supervisor.protocol.CommandType;
import com.oceanbase.odc.service.task.supervisor.protocol.GeneralTaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.StartTaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.TaskCommand;
import com.oceanbase.odc.service.task.supervisor.protocol.TaskNetClient;
import com.oceanbase.odc.service.task.supervisor.runtime.EndpointInfo;
import com.oceanbase.odc.service.task.supervisor.runtime.TaskCommandExecutor;
import com.oceanbase.odc.service.task.supervisor.runtime.TaskSupervisorServer;

/**
 * @author longpeng.zlp
 * @date 2024/11/25 16:58
 */
public class TaskSupervisorServerTest {
    private TaskSupervisorServer taskSupervisorServer;
    private SimpleTaskCommandExecutor simpleTaskCommandExecutor;
    private TaskNetClient taskNetClient;
    private DefaultJobContext jobContext;

    @Before
    public void setUp() throws InterruptedException {
        // init job context
        jobContext = new DefaultJobContext();
        jobContext.setJobIdentity(JobIdentity.of(1024L));
        jobContext.setJobClass(DummyTask.class.getName());
        jobContext.setHostUrls(Arrays.asList("127.0.0.1:8080"));
        jobContext.setJobParameters(new HashMap<String, String>() {
            {
                put("par1", "par11");
                put("par2", "par21");
            }
        });
        jobContext.setJobProperties(new HashMap<String, String>() {
            {
                put("pro1", "pro11");
                put("pro2", "pro21");
            }
        });
        taskNetClient = new TaskNetClient();
        simpleTaskCommandExecutor = new SimpleTaskCommandExecutor();
        taskSupervisorServer = new TaskSupervisorServer(0, simpleTaskCommandExecutor);
        taskSupervisorServer.start();
        while (taskSupervisorServer.getServerPort() <= 0) {
            Thread.sleep(100);
        }
    }

    @After
    public void shutdown() throws Exception {
        taskSupervisorServer.stop();
    }

    @Test
    public void testStartCommandProcess() throws IOException {
        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setJvmXmsMB(1024);
        processConfig.setJvmXmxMB(2048);
        processConfig.setEnvironments(new HashMap<String, String>() {
            {
                put("env1", "key1");
                put("evn2", "key2");
            }
        });
        StartTaskCommand startTaskCommand = StartTaskCommand.create(jobContext, processConfig);
        String ret = taskNetClient.sendCommand(
                new SupervisorEndpoint("127.0.0.1", taskSupervisorServer.getServerPort()),
                startTaskCommand);
        Assert.assertEquals(ret, startTaskCommand.commandType().name().toLowerCase());
        StartTaskCommand receivedCommand = (StartTaskCommand) simpleTaskCommandExecutor.receivedTaskCommand;
        checkJobContextEquals(receivedCommand.getJobContext(), jobContext);
        ProcessConfig receivedProcessConfig = receivedCommand.getProcessConfig();
        Assert.assertEquals(receivedProcessConfig.getJvmXmsMB(), 1024);
        Assert.assertEquals(receivedProcessConfig.getJvmXmxMB(), 2048);
        Assert.assertTrue(MapUtils.isEqual(receivedProcessConfig.getEnvironments(), processConfig.getEnvironments(),
                String::equals));
    }

    @Test
    public void testNoneStartCommandProcess() throws IOException {
        ExecutorEndpoint endpoint = new ExecutorEndpoint("command", "127.0.0.1", 8989, 12345, "identifier");
        for (CommandType commandType : CommandType.values()) {
            if (commandType == CommandType.START) {
                continue;
            }
            GeneralTaskCommand generalTaskCommand = GeneralTaskCommand.create(jobContext, endpoint, commandType);
            String ret = taskNetClient.sendCommand(
                    new SupervisorEndpoint("127.0.0.1", taskSupervisorServer.getServerPort()),
                    generalTaskCommand);
            GeneralTaskCommand receivedCommand = (GeneralTaskCommand) simpleTaskCommandExecutor.receivedTaskCommand;
            Assert.assertEquals(ret, generalTaskCommand.commandType().name().toLowerCase());
            checkJobContextEquals(receivedCommand.getJobContext(), jobContext);
            ExecutorEndpoint receivedEndpoint = receivedCommand.getExecutorEndpoint();
            Assert.assertEquals(receivedEndpoint.getSupervisorPort(), endpoint.getSupervisorPort());
            Assert.assertEquals(receivedEndpoint.getIdentifier(), endpoint.getIdentifier());
            Assert.assertEquals(receivedEndpoint.getProtocol(), endpoint.getProtocol());
            Assert.assertEquals(receivedEndpoint.getHost(), endpoint.getHost());
            Assert.assertEquals(receivedEndpoint.getExecutorPort(), endpoint.getExecutorPort());
        }
    }

    @Test
    public void testHeartbeat() throws IOException {
        String ret = taskNetClient.heartbeat(
                new SupervisorEndpoint("127.0.0.1", taskSupervisorServer.getServerPort()));
        GeneralTaskCommand receivedCommand = (GeneralTaskCommand) simpleTaskCommandExecutor.receivedTaskCommand;
        Assert.assertNull(receivedCommand);
        Assert.assertEquals(ret, "true");
    }

    @Test
    public void testHEndpointInfo() throws IOException {
        String ret = taskNetClient.memInfo(
                new SupervisorEndpoint("127.0.0.1", taskSupervisorServer.getServerPort()));
        GeneralTaskCommand receivedCommand = (GeneralTaskCommand) simpleTaskCommandExecutor.receivedTaskCommand;
        Assert.assertNull(receivedCommand);
        EndpointInfo endpointInfo = JsonUtils.fromJson(ret, EndpointInfo.class);
        Assert.assertNotNull(endpointInfo);
    }


    private void checkJobContextEquals(JobContext src, JobContext dest) {
        Assert.assertEquals(src.getJobClass(), dest.getJobClass());
        Assert.assertTrue(MapUtils.isEqual(src.getJobParameters(), dest.getJobParameters(), String::equals));
        Assert.assertTrue(MapUtils.isEqual(src.getJobProperties(), dest.getJobProperties(), String::equals));
        Assert.assertEquals(src.getJobIdentity().getId(), dest.getJobIdentity().getId());
        Assert.assertEquals(src.getHostUrls(), dest.getHostUrls());
    }

    private static final class SimpleTaskCommandExecutor implements TaskCommandExecutor {
        private TaskCommand receivedTaskCommand;

        @Override
        public String onCommand(TaskCommand taskCommand) throws JobException {
            this.receivedTaskCommand = taskCommand;
            return taskCommand.commandType().name().toLowerCase();
        }
    }
}
