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
package com.oceanbase.odc.service.task.resource;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.metadb.task.ResourceAllocateInfoRepository;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

/**
 * @author longpeng.zlp
 * @date 2025/1/8 10:13
 */
public class SupervisorAgentAllocatorTest {
    private SupervisorAgentAllocator supervisorAgentAllocator;
    private ResourceAllocateInfoRepository repository;
    private String applierName;
    private DefaultJobContext jobContext;
    private ResourceLocation resourceLocation;
    private ResourceAllocateInfoEntity resourceAllocateInfoEntity;
    private SupervisorEndpoint supervisorEndpoint;

    @Before
    public void init() {
        repository = Mockito.mock(ResourceAllocateInfoRepository.class);
        supervisorAgentAllocator = new SupervisorAgentAllocator(repository);
        applierName = "test";
        jobContext = new DefaultJobContext();
        jobContext.setJobIdentity(JobIdentity.of(1024L));
        resourceLocation = new ResourceLocation("local", "local");
        resourceAllocateInfoEntity = new ResourceAllocateInfoEntity();
        resourceAllocateInfoEntity.setTaskId(jobContext.getJobIdentity().getId());
        Mockito.when(repository.findByTaskIdNative(ArgumentMatchers.any()))
                .thenReturn(Optional.of(resourceAllocateInfoEntity));
        supervisorEndpoint = new SupervisorEndpoint(SystemUtils.getLocalIpAddress(), 9999);
    }

    @Test
    public void testResourceAllocateSubmitRequest() {
        supervisorAgentAllocator.submitAllocateSupervisorEndpointRequest(applierName, jobContext, resourceLocation);
        ArgumentCaptor<ResourceAllocateInfoEntity> captor = ArgumentCaptor.forClass(ResourceAllocateInfoEntity.class);
        Mockito.verify(repository).save(captor.capture());
        Assert.assertNull(captor.getValue().getEndpoint());
        Assert.assertEquals(captor.getValue().getResourceGroup(), "local");
        Assert.assertEquals(captor.getValue().getResourceRegion(), "local");
        Assert.assertEquals(captor.getValue().getTaskId().longValue(), jobContext.getJobIdentity().getId().longValue());
        Assert.assertEquals(captor.getValue().getResourceAllocateState(), ResourceAllocateState.PREPARING.name());
        Assert.assertEquals(captor.getValue().getResourceUsageState(), ResourceUsageState.PREPARING.name());
    }

    @Test
    public void testCheckAllocateSupervisorEndpointStateReady() {
        resourceAllocateInfoEntity.setEndpoint(JsonUtils.toJson(supervisorEndpoint));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.AVAILABLE.name());
        Optional<SupervisorEndpoint> endpoint =
                supervisorAgentAllocator.checkAllocateSupervisorEndpointState(jobContext);
        Assert.assertTrue(endpoint.isPresent());
        Assert.assertEquals(endpoint.get(), supervisorEndpoint);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(repository).updateResourceUsageStateByTaskId(captor.capture(), ArgumentMatchers.any());
        Assert.assertEquals(captor.getValue(), ResourceUsageState.USING.name());
    }

    @Test
    public void testCheckAllocateSupervisorEndpointStateNotReady1() {
        resourceAllocateInfoEntity.setEndpoint(JsonUtils.toJson(supervisorEndpoint));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.PREPARING.name());
        Optional<SupervisorEndpoint> endpoint =
                supervisorAgentAllocator.checkAllocateSupervisorEndpointState(jobContext);
        Assert.assertFalse(endpoint.isPresent());
        Mockito.verify(repository, Mockito.never()).updateResourceUsageStateByTaskId(ArgumentMatchers.any(),
                ArgumentMatchers.any());
    }

    @Test
    public void testCheckAllocateSupervisorEndpointStateNotReady2() {
        resourceAllocateInfoEntity.setEndpoint(JsonUtils.toJson(supervisorEndpoint));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.CREATING_RESOURCE.name());
        Optional<SupervisorEndpoint> endpoint =
                supervisorAgentAllocator.checkAllocateSupervisorEndpointState(jobContext);
        Assert.assertFalse(endpoint.isPresent());
        Mockito.verify(repository, Mockito.never()).updateResourceUsageStateByTaskId(ArgumentMatchers.any(),
                ArgumentMatchers.any());
    }

    @Test(expected = RuntimeException.class)
    public void testCheckAllocateSupervisorEndpointStateInvalid() {
        resourceAllocateInfoEntity.setEndpoint(JsonUtils.toJson(supervisorEndpoint));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.FAILED.name());
        supervisorAgentAllocator.checkAllocateSupervisorEndpointState(jobContext);
    }
}
