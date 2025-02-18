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
package com.oceanbase.odc.service.task.resource.manager;

import java.util.Arrays;
import java.util.Date;

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
import com.oceanbase.odc.metadb.task.SupervisorEndpointEntity;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.resource.ResourceAllocateState;
import com.oceanbase.odc.service.task.resource.ResourceUsageState;
import com.oceanbase.odc.service.task.supervisor.SupervisorEndpointState;
import com.oceanbase.odc.service.task.supervisor.proxy.RemoteTaskSupervisorProxy;

/**
 * @author longpeng.zlp
 * @date 2025/1/8 10:53
 */
public class ResourceAllocatorTest {
    private ResourceAllocateInfoEntity resourceAllocateInfoEntity;
    private ResourceAllocateInfoRepository repository;
    private ResourceAllocator resourceAllocator;
    private SupervisorEndpointRepositoryWrap supervisorEndpointRepositoryWrap;
    private ResourceAllocateInfoRepositoryWrap resourceAllocateInfoRepositoryWrap;
    private RemoteTaskSupervisorProxy remoteTaskSupervisorProxy;
    private ResourceManageStrategy resourceManageStrategy;
    private TaskFrameworkProperties taskFrameworkProperties;
    private SupervisorEndpointEntity supervisorEndpointEntity;

    @Before
    public void init() {
        supervisorEndpointRepositoryWrap = Mockito.mock(SupervisorEndpointRepositoryWrap.class);
        resourceAllocateInfoRepositoryWrap = Mockito.mock(ResourceAllocateInfoRepositoryWrap.class);
        remoteTaskSupervisorProxy = Mockito.mock(RemoteTaskSupervisorProxy.class);
        resourceManageStrategy = Mockito.mock(ResourceManageStrategy.class);
        taskFrameworkProperties = Mockito.mock(TaskFrameworkProperties.class);
        Mockito.when(taskFrameworkProperties.getResourceAllocateTimeOutSeconds()).thenReturn(10);
        resourceAllocator = new ResourceAllocator(supervisorEndpointRepositoryWrap, resourceAllocateInfoRepositoryWrap,
                remoteTaskSupervisorProxy, resourceManageStrategy, taskFrameworkProperties);
        supervisorEndpointEntity = new SupervisorEndpointEntity();
        supervisorEndpointEntity.setId(128L);
        supervisorEndpointEntity.setResourceID(1024L);
        supervisorEndpointEntity.setStatus(SupervisorEndpointState.AVAILABLE.name());
        supervisorEndpointEntity.setHost(SystemUtils.getLocalIpAddress());
        supervisorEndpointEntity.setPort(9999);
        resourceAllocateInfoEntity = new ResourceAllocateInfoEntity();
        resourceAllocateInfoEntity.setSupervisorEndpointId(1024L);
        resourceAllocateInfoEntity.setEndpoint(JsonUtils.toJson(supervisorEndpointEntity.getEndpoint()));
        Mockito.when(remoteTaskSupervisorProxy.isSupervisorAlive(ArgumentMatchers.any())).thenReturn(true);
        Mockito.when(
                resourceManageStrategy.isEndpointHaveEnoughResource(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(true);
    }

    @Test
    public void testResourceAllocatePreparingTimeout() {
        resourceAllocateInfoEntity.setUpdateTime(new Date(System.currentTimeMillis() - 1000000));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.PREPARING.name());
        resourceAllocator.allocate(resourceAllocateInfoEntity);
        Mockito.verify(resourceAllocateInfoRepositoryWrap, Mockito.times(1))
                .failedAllocateForId(ArgumentMatchers.any());
    }

    @Test
    public void testResourceAllocatePreparingAllocateSuccess() {
        resourceAllocateInfoEntity.setUpdateTime(new Date(System.currentTimeMillis()));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.PREPARING.name());
        resourceAllocateInfoEntity.setResourceUsageState(ResourceUsageState.PREPARING.name());
        Mockito.when(supervisorEndpointRepositoryWrap.collectAvailableSupervisorEndpoint(ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(
                        Arrays.asList(supervisorEndpointEntity));
        resourceAllocator.allocate(resourceAllocateInfoEntity);
        Mockito.verify(resourceAllocateInfoRepositoryWrap, Mockito.times(1)).allocateForJob(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testResourceAllocatePreparingAllocateNewResource() throws Exception {
        resourceAllocateInfoEntity.setUpdateTime(new Date(System.currentTimeMillis()));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.PREPARING.name());
        resourceAllocateInfoEntity.setResourceUsageState(ResourceUsageState.PREPARING.name());
        Mockito.when(supervisorEndpointRepositoryWrap.collectAvailableSupervisorEndpoint(ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(
                        Arrays.asList());
        Mockito.when(resourceManageStrategy.handleNoResourceAvailable(ArgumentMatchers.any()))
                .thenReturn(supervisorEndpointEntity);
        resourceAllocator.allocate(resourceAllocateInfoEntity);
        Mockito.verify(resourceAllocateInfoRepositoryWrap, Mockito.times(1))
                .prepareResourceForJob(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testResourceAllocateCreatingResourceTimeout() throws Exception {
        supervisorEndpointEntity.setId(1024L);
        resourceAllocateInfoEntity.setUpdateTime(new Date(System.currentTimeMillis() - 10000000));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.CREATING_RESOURCE.name());
        resourceAllocateInfoEntity.setResourceUsageState(ResourceUsageState.PREPARING.name());
        resourceAllocateInfoEntity.setSupervisorEndpointId(supervisorEndpointEntity.getId());
        resourceAllocator.allocate(resourceAllocateInfoEntity);
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(supervisorEndpointRepositoryWrap).releaseLoad(captor.capture());
        Assert.assertEquals(captor.getValue(), supervisorEndpointEntity.getId());
    }

    @Test
    public void testResourceAllocateCreatingResourceReady() throws Exception {
        resourceAllocateInfoEntity.setUpdateTime(new Date(System.currentTimeMillis()));
        resourceAllocateInfoEntity.setResourceAllocateState(ResourceAllocateState.CREATING_RESOURCE.name());
        resourceAllocateInfoEntity.setResourceUsageState(ResourceUsageState.PREPARING.name());
        Mockito.when(resourceManageStrategy.detectIfEndpointIsAvailable(ArgumentMatchers.any()))
                .thenReturn(supervisorEndpointEntity);
        resourceAllocator.allocate(resourceAllocateInfoEntity);
        Mockito.verify(resourceAllocateInfoRepositoryWrap, Mockito.times(1)).allocateForJob(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }
}
