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
package com.oceanbase.odc.service.task.executor.task;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.executor.server.TaskMonitor;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author longpeng.zlp
 * @date 2024/10/11 14:11
 */
public class BaseTaskTest {
    private DefaultJobContext jobContext;

    @Before
    public void init() {
        jobContext = new DefaultJobContext();
        jobContext.setJobParameters(new HashMap<>());
        jobContext.setJobProperties(new HashMap<>());
        JobIdentity jobIdentity = new JobIdentity();
        jobIdentity.setId(1L);
        jobContext.setJobIdentity(jobIdentity);
        jobContext.setJobClass(DummyBaseTask.class.getName());
    }


    @Test
    public void testExceptionListenerNormal() {
        try (MockedStatic<SystemUtils> mockSystemUtil = Mockito.mockStatic(SystemUtils.class)) {
            mockSystemUtil.when(() -> {
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT);
            }).thenReturn("9099");
            DummyBaseTask dummyBaseTask = new DummyBaseTask(false);
            dummyBaseTask.start(new TaskContext() {
                @Override
                public ExceptionListener getExceptionListener() {
                    return dummyBaseTask;
                }

                @Override
                public JobContext getJobContext() {
                    return jobContext;
                }
            });
            DefaultTaskResult taskResult = DefaultTaskResultBuilder.build(dummyBaseTask);
            DefaultTaskResultBuilder.assignErrorMessage(taskResult, dummyBaseTask);
            Assert.assertNull(taskResult.getErrorMessage());
        }
    }

    @Test
    public void testExceptionListenerWithException() {
        try (MockedStatic<SystemUtils> mockSystemUtil = Mockito.mockStatic(SystemUtils.class)) {
            mockSystemUtil.when(() -> {
                SystemUtils.getEnvOrProperty(JobEnvKeyConstants.ODC_EXECUTOR_PORT);
            }).thenReturn("9099");
            DummyBaseTask dummyBaseTask = new DummyBaseTask(true);
            dummyBaseTask.start(new TaskContext() {
                @Override
                public ExceptionListener getExceptionListener() {
                    return dummyBaseTask;
                }

                @Override
                public JobContext getJobContext() {
                    return jobContext;
                }
            });
            DefaultTaskResult taskResult = DefaultTaskResultBuilder.build(dummyBaseTask);
            DefaultTaskResultBuilder.assignErrorMessage(taskResult, dummyBaseTask);
            Assert.assertEquals(taskResult.getErrorMessage(), "exception should be thrown");
        }
    }

    private static final class DummyBaseTask extends BaseTask<String> {
        private final boolean shouldThrowException;

        private DummyBaseTask(boolean shouldThrowException) {
            this.shouldThrowException = shouldThrowException;
        }

        @Override
        protected void doInit(JobContext context) throws Exception {}

        @Override
        protected boolean doStart(JobContext context, TaskContext taskContext) throws Exception {
            if (shouldThrowException) {
                throw new IllegalStateException("exception should be thrown");
            }
            return true;
        }

        protected TaskMonitor createTaskMonitor() {
            return Mockito.mock(TaskMonitor.class);
        }

        @Override
        protected void doStop() throws Exception {}

        @Override
        protected void doClose() throws Exception {}

        @Override
        public double getProgress() {
            return 100;
        }

        @Override
        public String getTaskResult() {
            return "res";
        }
    }
}
