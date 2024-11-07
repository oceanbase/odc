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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.TaskContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.executor.DefaultTaskResult;
import com.oceanbase.odc.service.task.executor.TaskReporter;

/**
 * @author longpeng.zlp
 * @date 2024/11/7 17:45
 */
public class TaskMonitorTest {
    @Test
    public void testTaskMonitorOnException() {
        TaskMonitor taskMonitor = new TaskMonitor(new MockBaseTask(), Mockito.mock(TaskReporter.class), Mockito.mock(
                CloudObjectStorageService.class));
        Assert.assertNull(taskMonitor.getError());
        taskMonitor.onException(new Throwable("error"));
        Throwable ex = taskMonitor.getError();
        Assert.assertEquals(ex.getMessage(), "error");
        Assert.assertNull(taskMonitor.getError());
    }

    @Test
    public void testTaskMonitorReportRetryFailed() {
        TaskReporter taskReporter = Mockito.mock(TaskReporter.class);
        Mockito.when(taskReporter.report(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(false);
        TaskMonitor taskMonitor = new TaskMonitor(new MockBaseTask(), taskReporter, Mockito.mock(
                CloudObjectStorageService.class));
        Assert.assertFalse(taskMonitor.reportTaskResultWithRetry(new DefaultTaskResult(), 3, 1));
    }

    @Test
    public void testTaskMonitorReportRetrySuccess() {
        TaskReporter taskReporter = Mockito.mock(TaskReporter.class);
        Mockito.when(taskReporter.report(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);
        TaskMonitor taskMonitor = new TaskMonitor(new MockBaseTask(), taskReporter, Mockito.mock(
                CloudObjectStorageService.class));
        Assert.assertTrue(taskMonitor.reportTaskResultWithRetry(new DefaultTaskResult(), 3, 1));
    }

    private static final class MockBaseTask implements Task<String> {

        @Override
        public void start(TaskContext taskContext) {}

        @Override
        public boolean stop() {
            return false;
        }

        @Override
        public boolean modify(Map<String, String> jobParameters) {
            return false;
        }

        @Override
        public double getProgress() {
            return 0;
        }

        @Override
        public JobContext getJobContext() {
            return null;
        }

        @Override
        public TaskStatus getStatus() {
            return TaskStatus.RUNNING;
        }

        @Override
        public String getTaskResult() {
            return "result";
        }
    }
}
