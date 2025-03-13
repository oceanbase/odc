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

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.common.trace.TraceDecorator;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;
import com.oceanbase.odc.service.task.util.TaskResultWrap;

/**
 * @author longpeng.zlp
 * @date 2025/1/7 14:25
 */
public class PullTaskResultJobV2Test extends DaemonV2TestBase {
    private PullTaskResultJobV2 pullTaskResultJobV2;
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private TaskExecutorClient taskExecutorClient;
    private TaskResult taskResult;

    @Before
    public void init() throws JobException {
        super.init();
        pullTaskResultJobV2 = new PullTaskResultJobV2();
        threadPoolTaskExecutor = initThreadPoolExecutor();
        taskExecutorClient = Mockito.mock(TaskExecutorClient.class);
        Mockito.when(configuration.getTaskExecutorClient()).thenReturn(taskExecutorClient);
        Mockito.when(taskFrameworkService.getTaskResultPullerExecutor()).thenReturn(threadPoolTaskExecutor);
        jobEntity.setExecutorEndpoint("endpoint");
        taskResult = new TaskResult();
        Mockito.when(taskExecutorClient.getResult(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new TaskResultWrap(taskResult, true, null));
    }

    @After
    public void clean() {
        if (null != threadPoolTaskExecutor) {
            threadPoolTaskExecutor.destroy();
        }
    }

    private ThreadPoolTaskExecutor initThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Math.max(SystemUtils.availableProcessors() * 4, 16);
        int maxPoolSize = Math.max(SystemUtils.availableProcessors() * 16, 128);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setThreadNamePrefix("task-framework-result-puller-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(new TraceDecorator<>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    @Test
    public void testTryPullTaskResultWithoutEntity() throws JobException {
        jobEntity.setExecutorEndpoint(null);
        TaskResultWrap taskResultWrap = pullTaskResultJobV2.tryPullTaskResult(taskExecutorClient, jobEntity);
        Assert.assertNotNull(taskResultWrap.getE());
        Assert.assertTrue(StringUtils.containsIgnoreCase(taskResultWrap.getE().getMessage(), "executor endpoint"));
    }

    @Test
    public void testGetJobHeartbeatDuringTime() {
        long currentMillionSeconds = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(currentMillionSeconds);
        Date date = new Date(currentMillionSeconds - 10000);
        jobEntity.setCreateTime(date);
        jobEntity.setLastHeartTime(null);
        Assert.assertEquals(pullTaskResultJobV2.getLastUpdateTimeInMillDurationInMillSeconds(jobEntity, instant),
                10000);
        jobEntity.setLastHeartTime(new Date(currentMillionSeconds - 500));
        Assert.assertEquals(pullTaskResultJobV2.getLastUpdateTimeInMillDurationInMillSeconds(jobEntity, instant), 500);
    }

    @Test
    public void testGetJobsForPullResult() {
        long currentMillionSeconds = System.currentTimeMillis();
        Date date = new Date(currentMillionSeconds - 10000);
        jobEntity.setCreateTime(date);
        jobEntity.setLastHeartTime(null);
        JobEntity entity2 = new JobEntity();
        entity2.setId(1025L);
        entity2.setCreateTime(new Date(currentMillionSeconds - 20000));
        entity2.setLastHeartTime(null);
        Page<JobEntity> tmp = Mockito.mock(Page.class);
        Mockito.when(tmp.stream()).thenReturn(Arrays.asList(jobEntity, entity2).stream());
        Mockito.when(taskFrameworkService.findNeedPullResultJobs(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(tmp);
        List<JobEntity> runningJobs = pullTaskResultJobV2.getRunningJobs(configuration);
        Assert.assertEquals(runningJobs.size(), 2);
        Assert.assertEquals(runningJobs.get(0).getId().longValue(), 1025);
        Assert.assertEquals(runningJobs.get(1).getId().longValue(), 1024);
    }

    @Test
    public void testTaskResultNotReady() {
        long currentMillionSeconds = System.currentTimeMillis();
        Date date = new Date(currentMillionSeconds - 10000);
        jobEntity.setCreateTime(date);
        jobEntity.setLastHeartTime(null);
        jobEntity.setStatus(JobStatus.RUNNING);
        List<JobEntity> jobEntities = Arrays.asList(jobEntity);
        taskResult.setStatus(TaskStatus.PREPARING);
        pullTaskResultJobV2.doRefreshJob(configuration, jobEntities);
        Mockito.verify(taskFrameworkService, Mockito.times(1)).updateHeartbeatWithExpectStatus(ArgumentMatchers.any(),
                ArgumentMatchers.any());
        Mockito.verify(taskFrameworkService, Mockito.never()).propagateTaskResult(ArgumentMatchers.any(),
                ArgumentMatchers.any());
    }

    @Test
    public void testTaskResultTimeout() throws JobException {
        long currentMillionSeconds = System.currentTimeMillis();
        Date date = new Date(currentMillionSeconds - 1000000000);
        jobEntity.setCreateTime(date);
        jobEntity.setLastHeartTime(null);
        jobEntity.setStatus(JobStatus.RUNNING);
        List<JobEntity> jobEntities = Arrays.asList(jobEntity);
        Mockito.when(taskExecutorClient.getResult(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new TaskResultWrap(null, false, null));
        pullTaskResultJobV2.doRefreshJob(configuration, jobEntities);
        Mockito.verify(taskFrameworkService, Mockito.never()).updateHeartbeatWithExpectStatus(ArgumentMatchers.any(),
                ArgumentMatchers.any());
        ArgumentCaptor<JobStatus> captor = ArgumentCaptor.forClass(JobStatus.class);
        Mockito.verify(taskFrameworkService).updateStatusByIdOldStatus(ArgumentMatchers.any(), ArgumentMatchers.any(),
                captor.capture());
        Assert.assertEquals(captor.getValue(), JobStatus.TIMEOUT);
    }

    @Test
    public void testTaskResultWithoutLogMeta() throws JobException {
        long currentMillionSeconds = System.currentTimeMillis();
        Date date = new Date(currentMillionSeconds - 1000000000);
        jobEntity.setCreateTime(date);
        jobEntity.setLastHeartTime(null);
        jobEntity.setStatus(JobStatus.RUNNING);
        List<JobEntity> jobEntities = Arrays.asList(jobEntity);
        taskResult.setStatus(TaskStatus.DONE);
        pullTaskResultJobV2.doRefreshJob(configuration, jobEntities);
        Mockito.verify(taskFrameworkService, Mockito.times(1)).propagateTaskResult(ArgumentMatchers.any(),
                ArgumentMatchers.any());
        Mockito.verify(taskFrameworkService, Mockito.never()).publishEvent(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testTaskResultWithLogMeta() throws JobException {
        long currentMillionSeconds = System.currentTimeMillis();
        Date date = new Date(currentMillionSeconds - 1000000000);
        jobEntity.setCreateTime(date);
        jobEntity.setLastHeartTime(null);
        jobEntity.setStatus(JobStatus.DO_CANCELING);
        List<JobEntity> jobEntities = Arrays.asList(jobEntity);
        taskResult.setStatus(TaskStatus.DONE);
        Map<String, String> logMeta = new HashMap<>();
        logMeta.put("k", "v");
        taskResult.setLogMetadata(logMeta);
        pullTaskResultJobV2.doRefreshJob(configuration, jobEntities);
        ArgumentCaptor<JobStatus> captor = ArgumentCaptor.forClass(JobStatus.class);
        Mockito.verify(taskFrameworkService).publishEvent(ArgumentMatchers.any(), ArgumentMatchers.any(),
                captor.capture());
        Assert.assertEquals(captor.getValue(), JobStatus.CANCELED);
    }
}
