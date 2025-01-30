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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;

/**
 * @author longpeng.zlp
 * @date 2025/1/6 18:57
 */
public class DoStopJobV2Test extends DaemonV2TestBase {
    protected DoStopJobV2 doStopJobV2;
    protected String executorEndpoint;

    @Before
    public void init() throws JobException {
        super.init();
        doStopJobV2 = new DoStopJobV2();
        executorEndpoint = "http://host:9999";
    }

    @Test
    public void testDoStopJobStatusChanged() throws JobException {
        jobEntity.setExecutorEndpoint(executorEndpoint);
        jobEntity.setStatus(JobStatus.CANCELING);
        JobEntity modified = new JobEntity();
        modified.setStatus(JobStatus.FAILED);
        Mockito.when(taskFrameworkService.findWithPessimisticLock(ArgumentMatchers.any())).thenReturn(modified);
        doStopJobV2.sendStopToTask(configuration, configuration.getTaskFrameworkService(), jobEntity);
        Mockito.verify(taskFrameworkService, Mockito.never()).updateStatusByIdOldStatus(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testSendStopToCancelingJob() throws JobException {
        doSendStop(JobStatus.CANCELING);
        ArgumentCaptor<JobStatus> statusCapture = ArgumentCaptor.forClass(JobStatus.class);
        Mockito.verify(taskFrameworkService).updateStatusByIdOldStatus(ArgumentMatchers.any(),
                ArgumentMatchers.any(), statusCapture.capture());
        Assert.assertEquals(statusCapture.getValue(), JobStatus.DO_CANCELING);
    }

    @Test
    public void testSendStopToTimeoutJob() throws JobException {
        doSendStop(JobStatus.TIMEOUT);
        ArgumentCaptor<JobStatus> statusCapture = ArgumentCaptor.forClass(JobStatus.class);
        Mockito.verify(taskFrameworkService).updateStatusDescriptionByIdOldStatus(ArgumentMatchers.any(),
                ArgumentMatchers.any(), statusCapture.capture(), ArgumentMatchers.any());
        Assert.assertEquals(statusCapture.getValue(), JobStatus.FAILED);
    }

    private void doSendStop(JobStatus jobStatus) throws JobException {
        jobEntity.setExecutorEndpoint(executorEndpoint);
        jobEntity.setStatus(jobStatus);
        Mockito.when(taskFrameworkService.findWithPessimisticLock(ArgumentMatchers.any())).thenReturn(jobEntity);
        doStopJobV2.sendStopToTask(configuration, configuration.getTaskFrameworkService(), jobEntity);
        ArgumentCaptor<ExecutorEndpoint> executorEndpointArgumentCaptor =
                ArgumentCaptor.forClass(ExecutorEndpoint.class);
        Mockito.verify(taskSupervisorJobCaller).stopTaskDirectly(executorEndpointArgumentCaptor.capture(),
                ArgumentMatchers.any());
        Assert.assertEquals(executorEndpointArgumentCaptor.getValue().getHost(), "host");
        Assert.assertEquals(executorEndpointArgumentCaptor.getValue().getExecutorPort().intValue(), 9999);
        Assert.assertEquals(executorEndpointArgumentCaptor.getValue().getSupervisorPort().intValue(), 9989);
    }
}
