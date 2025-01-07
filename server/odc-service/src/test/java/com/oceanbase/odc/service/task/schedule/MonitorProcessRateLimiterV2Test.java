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
package com.oceanbase.odc.service.task.schedule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

/**
 * @author longpeng.zlp
 * @date 2025/1/6 15:59
 */
public class MonitorProcessRateLimiterV2Test {
    private TaskFrameworkProperties taskFrameworkProperties;
    private TaskFrameworkService taskFrameworkService;
    private MonitorProcessRateLimiterV2 monitorProcessRateLimiterV2;

    @Before
    public void init() {
        taskFrameworkProperties = Mockito.mock(TaskFrameworkProperties.class);
        Mockito.when(taskFrameworkProperties.getJobProcessMinMemorySizeInMB()).thenReturn(1L);
        taskFrameworkService = Mockito.mock(TaskFrameworkService.class);
        monitorProcessRateLimiterV2 = new MonitorProcessRateLimiterV2(() -> taskFrameworkProperties,
                taskFrameworkService, 5);
    }

    @Test
    public void testMonitorProcessRateLimiterV2AcquireSuccess() {
        Mockito.when(taskFrameworkService.countRunningJobs(TaskRunMode.PROCESS)).thenReturn(1L);
        Assert.assertTrue(monitorProcessRateLimiterV2.doTryAcquire());
    }

    @Test
    public void testMonitorProcessRateLimiterV2AcquireFailed() {
        Mockito.when(taskFrameworkService.countRunningJobs(TaskRunMode.PROCESS)).thenReturn(10L);
        Assert.assertFalse(monitorProcessRateLimiterV2.doTryAcquire());
    }
}
