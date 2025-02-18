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
package com.oceanbase.odc.service.task.schedule.daemon.v2;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.caller.DefaultExecutorIdentifier;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.supervisor.TaskCallerResult;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;

/**
 * @author longpeng.zlp
 * @date 2025/1/7 10:06
 */
public class DoFinishJobV2Test extends DaemonV2TestBase {

    private DoFinishJobV2 doFinishJobV2;
    private DefaultExecutorIdentifier executorIdentifier;
    private ResourceAllocateInfoEntity resourceAllocateInfoEntity;

    @Before
    public void init() throws JobException {
        super.init();
        doFinishJobV2 = new DoFinishJobV2();
        executorIdentifier = DefaultExecutorIdentifier.builder()
                .host(supervisorEndpoint.getHost())
                .port(9999)
                .protocol("test")
                .namespace("-10000") // invalid pid
                .executorName("test_identifier")
                .build();
        jobEntity.setExecutorIdentifier(executorIdentifier.toString());
        resourceAllocateInfoEntity = new ResourceAllocateInfoEntity();
    }

    @Test
    public void testReleaseK8SResource() {
        doFinishJobV2.releaseK8sResource(configuration, jobEntity, taskFrameworkProperties);
        ArgumentCaptor<ResourceID> argumentCaptor = ArgumentCaptor.forClass(ResourceID.class);
        Mockito.verify(resourceManager).release(argumentCaptor.capture());
        Assert.assertEquals(argumentCaptor.getValue(),
                new ResourceID(new ResourceLocation("test", "test"), "cloudK8sPod", "-10000", "test_identifier"));
    }

    @Test
    public void testReleaseK8SResourceNotReleaseResource() {
        jobEntity.setExecutorIdentifier(null);
        doFinishJobV2.releaseK8sResource(configuration, jobEntity, taskFrameworkProperties);
        Mockito.verify(resourceManager, Mockito.never()).release(ArgumentMatchers.any());
    }

    @Test
    public void testDestroyTask() throws JobException {
        jobEntity.setExecutorIdentifier(null);
        // no task identifier
        Assert.assertTrue(doFinishJobV2.tryDestroyProcess(jobEntity));
        // task not on this machine
        executorIdentifier.setHost("lllll");
        jobEntity.setExecutorIdentifier(executorIdentifier.toString());
        Assert.assertFalse(doFinishJobV2.tryDestroyProcess(jobEntity));
        // task on this machine
        executorIdentifier.setHost(supervisorEndpoint.getHost());
        jobEntity.setExecutorIdentifier(executorIdentifier.toString());
        Assert.assertTrue(doFinishJobV2.tryDestroyProcess(jobEntity));
    }

    @Test
    public void testDestroyTaskByAgentNotAllocateResource() throws JobException {
        jobEntity.setExecutorIdentifier(null);
        doFinishJobV2.destroyTaskByAgent(configuration, resourceAllocateInfoEntity, jobEntity);
        Mockito.verify(taskSupervisorJobCaller, Mockito.never()).destroyTask(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testDestroyTaskByAgentNotStart() throws JobException {
        jobEntity.setExecutorIdentifier(null);
        resourceAllocateInfoEntity.setEndpoint(JsonUtils.toJson(supervisorEndpoint));
        doFinishJobV2.destroyTaskByAgent(configuration, resourceAllocateInfoEntity, jobEntity);
        Mockito.verify(taskSupervisorJobCaller, Mockito.never()).destroyTask(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testDestroyTaskByAgent() throws JobException {
        resourceAllocateInfoEntity.setEndpoint(JsonUtils.toJson(supervisorEndpoint));
        Mockito.when(taskSupervisorJobCaller.destroyTask(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(new TaskCallerResult(true, null));
        doFinishJobV2.destroyTaskByAgent(configuration, resourceAllocateInfoEntity, jobEntity);
        ArgumentCaptor<ExecutorEndpoint> captor = ArgumentCaptor.forClass(ExecutorEndpoint.class);
        Mockito.verify(taskSupervisorJobCaller).destroyTask(ArgumentMatchers.any(), captor.capture(),
                ArgumentMatchers.any());
        Assert.assertEquals(captor.getValue().getExecutorPort().intValue(), 9999);
    }

    @Test
    public void testTryDestroyExecutorCurrentVersion() throws JobException {
        Mockito.when(supervisorAgentAllocator.queryResourceAllocateIntoEntity(ArgumentMatchers.any())).thenReturn(
                Optional.of(resourceAllocateInfoEntity));
        Assert.assertTrue(doFinishJobV2.tryDestroyExecutor(configuration, jobEntity));
        Mockito.verify(supervisorAgentAllocator, Mockito.times(1)).deallocateSupervisorEndpoint(ArgumentMatchers.any());
        Mockito.verify(taskFrameworkProperties, Mockito.never()).getRunMode();
    }

    @Test
    public void testTryDestroyExecutorOldVersionK8s() throws JobException {
        Mockito.when(supervisorAgentAllocator.queryResourceAllocateIntoEntity(ArgumentMatchers.any())).thenReturn(
                Optional.empty());
        Assert.assertTrue(doFinishJobV2.tryDestroyExecutor(configuration, jobEntity));
        ArgumentCaptor<ResourceID> captor = ArgumentCaptor.forClass(ResourceID.class);
        Mockito.verify(resourceManager).release(captor.capture());
        Assert.assertEquals(captor.getValue(),
                new ResourceID(new ResourceLocation("test", "test"), "cloudK8sPod", "-10000", "test_identifier"));
    }
}
