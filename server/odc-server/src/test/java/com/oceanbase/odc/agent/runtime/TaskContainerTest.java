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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author longpeng.zlp
 * @date 2024/10/11 14:11
 */
public class TaskContainerTest {
    private DefaultJobContext jobContext;

    @Before
    public void init() {
        jobContext = new DefaultJobContext();
        jobContext.setJobParameters(new HashMap<>());
        jobContext.setJobProperties(new HashMap<>());
        JobIdentity jobIdentity = new JobIdentity();
        jobIdentity.setId(1L);
        jobContext.setJobIdentity(jobIdentity);
        jobContext.setJobClass(SimpleTask.class.getName());
    }

    @Test
    public void testExceptionListenerNormal() {
        try (MockedStatic<SystemUtils> mockSystemUtil = Mockito.mockStatic(SystemUtils.class)) {
            mockSystemUtil.when(() -> {
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT);
            }).thenReturn("9099");
            SimpleTask dummyBaseTask = new SimpleTask(false);
            TaskContainer<?> taskContainer = buildTaskContainer(jobContext, dummyBaseTask);
            taskContainer.runTask();
            taskContainer.taskMonitor.finalWork();
            TaskReporter taskReporter = taskContainer.taskMonitor.getReporter();
            ArgumentCaptor<TaskResult> argumentCaptor = ArgumentCaptor.forClass(TaskResult.class);
            Mockito.verify(taskReporter).report(ArgumentMatchers.any(), argumentCaptor.capture());
            Assert.assertNull(argumentCaptor.getValue().getErrorMessage());
        }
    }

    @Test
    public void testExceptionListenerWithException() {
        try (MockedStatic<SystemUtils> mockSystemUtil = Mockito.mockStatic(SystemUtils.class)) {
            mockSystemUtil.when(() -> {
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT);
            }).thenReturn("9099");
            SimpleTask dummyBaseTask = new SimpleTask(true);
            TaskContainer<?> taskContainer = buildTaskContainer(jobContext, dummyBaseTask);
            taskContainer.runTask();
            taskContainer.taskMonitor.finalWork();
            TaskReporter taskReporter = taskContainer.taskMonitor.getReporter();
            ArgumentCaptor<TaskResult> argumentCaptor = ArgumentCaptor.forClass(TaskResult.class);
            Mockito.verify(taskReporter).report(ArgumentMatchers.any(), argumentCaptor.capture());
            Assert.assertTrue(
                    StringUtils.contains(argumentCaptor.getValue().getErrorMessage(), "exception should be thrown"));
        }
    }

    @Test
    public void testTaskContainerOnException() {
        TaskContainer<String> taskContainer = new TaskContainer<>(jobContext, Mockito.mock(
                CloudObjectStorageService.class), Mockito.mock(TaskReporter.class), new SimpleTask(false));
        Assert.assertNull(taskContainer.getError());
        taskContainer.onException(new Throwable("error"));
        Throwable ex = taskContainer.getError();
        Assert.assertEquals(ex.getMessage(), "error");
        Assert.assertNull(taskContainer.getError());
    }

    private static TaskContainer<?> buildTaskContainer(JobContext jobContext, Task<?> task) {
        TaskReporter taskReporter = Mockito.mock(TaskReporter.class);
        Mockito.when(taskReporter.report(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);
        return new TaskContainer<>(jobContext, Mockito.mock(CloudObjectStorageService.class), taskReporter, task);
    }
}
