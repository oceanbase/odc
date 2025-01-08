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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.listener.JobTerminateEvent;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;

/**
 * @author longpeng.zlp
 * @date 2025/1/6 16:17
 */
public class StartPreparingJobV2Test extends DaemonV2TestBase {
    protected StartPreparingJobV2 startPreparingJobV2;


    @Before
    public void init() throws JobException {
        super.init();
        Mockito.when(supervisorAgentAllocator.checkAllocateSupervisorEndpointState(ArgumentMatchers.any())).thenReturn(
                Optional.of(supervisorEndpoint));
        ExecutorEndpoint executorEndpoint = new ExecutorEndpoint("agent", supervisorEndpoint.getHost(),
                supervisorEndpoint.getPort(), 8888, "identifier");
        Mockito.when(taskSupervisorJobCaller.startTask(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenReturn(executorEndpoint);
        startPreparingJobV2 = new StartPreparingJobV2();
    }

    @Test
    public void testProcessPreparingJob() {
        jobEntity.setStatus(JobStatus.PREPARING);
        jobEntity.setCreateTime(new Date(System.currentTimeMillis()));
        Mockito.when(taskFrameworkService.find(Lists.newArrayList(JobStatus.PREPARING), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows())).thenReturn(page);
        Mockito.when(taskFrameworkService.findWithPessimisticLock(ArgumentMatchers.any())).thenReturn(jobEntity);
        startPreparingJobV2.processPreparingJob(configuration);
        // check allocate resource parameters
        ArgumentCaptor<JobContext> jobContextCapture = ArgumentCaptor.forClass(JobContext.class);
        ArgumentCaptor<String> applierNameCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ResourceLocation> locationCapture = ArgumentCaptor.forClass(ResourceLocation.class);
        Mockito.verify(supervisorAgentAllocator).submitAllocateSupervisorEndpointRequest(applierNameCapture.capture(),
                jobContextCapture.capture(), locationCapture.capture());
        Assert.assertEquals(jobContextCapture.getValue().getJobIdentity().getId().longValue(), 1024);
        Assert.assertEquals(applierNameCapture.getValue(), "K8S");
        Assert.assertEquals(locationCapture.getValue(), new ResourceLocation("test", "test"));
    }

    @Test
    public void testProcessPreparingJobStatusChanged() {
        jobEntity.setStatus(JobStatus.PREPARING);
        jobEntity.setCreateTime(new Date(System.currentTimeMillis()));
        JobEntity modified = new JobEntity();
        modified.setStatus(JobStatus.RUNNING);
        Mockito.when(taskFrameworkService.find(Lists.newArrayList(JobStatus.PREPARING), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows())).thenReturn(page);
        Mockito.when(taskFrameworkService.findWithPessimisticLock(ArgumentMatchers.any())).thenReturn(modified);
        startPreparingJobV2.processPreparingJob(configuration);
        // check allocate resource parameters
        Mockito.verify(supervisorAgentAllocator, Mockito.never()).submitAllocateSupervisorEndpointRequest(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testProcessPreparingJobTimeout() {
        jobEntity.setStatus(JobStatus.PREPARING);
        jobEntity.setCreateTime(new Date(System.currentTimeMillis() - 1000000));
        Mockito.when(taskFrameworkService.find(Lists.newArrayList(JobStatus.PREPARING), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows())).thenReturn(page);
        startPreparingJobV2.processPreparingJob(configuration);
        ArgumentCaptor<JobStatus> statusCapture = ArgumentCaptor.forClass(JobStatus.class);
        Mockito.verify(taskFrameworkService).updateStatusByIdOldStatus(ArgumentMatchers.any(),
                ArgumentMatchers.any(), statusCapture.capture());
        Assert.assertEquals(statusCapture.getValue(), JobStatus.TIMEOUT);
    }

    @Test
    public void testProcessStartingJob() throws JobException {
        jobEntity.setStatus(JobStatus.PREPARING);
        jobEntity.setCreateTime(new Date(System.currentTimeMillis()));
        Mockito.when(taskFrameworkService.find(Lists.newArrayList(JobStatus.PREPARING_RESR), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows())).thenReturn(page);
        startPreparingJobV2.processReadyStartJob(configuration);
        // check start job parameters
        ArgumentCaptor<JobContext> jobContextCapture = ArgumentCaptor.forClass(JobContext.class);
        Mockito.verify(taskSupervisorJobCaller).startTask(ArgumentMatchers.any(),
                jobContextCapture.capture(), ArgumentMatchers.any());
        Assert.assertEquals(jobContextCapture.getValue().getJobIdentity().getId().longValue(), 1024);
    }

    @Test
    public void testProcessStartingJobTimeout() throws JobException {
        jobEntity.setStatus(JobStatus.PREPARING);
        jobEntity.setCreateTime(new Date(System.currentTimeMillis() - 1000000));
        Mockito.when(taskFrameworkService.find(Lists.newArrayList(JobStatus.PREPARING_RESR), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows())).thenReturn(page);
        startPreparingJobV2.processReadyStartJob(configuration);
        ArgumentCaptor<JobStatus> statusCapture = ArgumentCaptor.forClass(JobStatus.class);
        Mockito.verify(taskFrameworkService).updateStatusByIdOldStatus(ArgumentMatchers.any(),
                ArgumentMatchers.any(), statusCapture.capture());
        Assert.assertEquals(statusCapture.getValue(), JobStatus.TIMEOUT);
    }

    @Test
    public void testProcessStartingJobFailed() throws JobException {
        jobEntity.setStatus(JobStatus.PREPARING);
        jobEntity.setCreateTime(new Date(System.currentTimeMillis()));
        Mockito.when(taskFrameworkService.find(Lists.newArrayList(JobStatus.PREPARING_RESR), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows())).thenReturn(page);
        Mockito.when(taskSupervisorJobCaller.startTask(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any())).thenThrow(new RuntimeException("failed"));
        startPreparingJobV2.processReadyStartJob(configuration);
        // check terminate event send
        ArgumentCaptor<JobTerminateEvent> eventArgumentCaptor = ArgumentCaptor.forClass(JobTerminateEvent.class);
        Mockito.verify(eventPublisher).publishEvent(eventArgumentCaptor.capture());
        Assert.assertEquals(eventArgumentCaptor.getValue().getStatus(), JobStatus.FAILED);
        Assert.assertEquals(eventArgumentCaptor.getValue().getJi().getId().longValue(), 1024);

    }


    @Test
    public void testJobExpired() {
        jobEntity.setCreateTime(new Date(System.currentTimeMillis() - 1000000));
        Assert.assertTrue(startPreparingJobV2.checkJobIsExpired(jobEntity));
    }

    @Test
    public void testJobNotExpired1() {
        jobEntity.setCreateTime(new Date(System.currentTimeMillis()));
        Assert.assertFalse(startPreparingJobV2.checkJobIsExpired(jobEntity));
    }

    @Test
    public void testJobNotExpired2() {
        Map<String, String> properties = new HashMap<>();
        jobEntity.setJobProperties(properties);
        jobEntity.setCreateTime(new Date(System.currentTimeMillis() - 100000));
        Assert.assertFalse(startPreparingJobV2.checkJobIsExpired(jobEntity));
    }
}
