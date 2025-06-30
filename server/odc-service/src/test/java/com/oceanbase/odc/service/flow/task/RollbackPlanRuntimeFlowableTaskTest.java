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
package com.oceanbase.odc.service.flow.task;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.task.RollbackPlanRuntimeFlowableTask.FlowCanceledWatcher;

/**
 * @author longpeng.zlp
 * @date 2025/5/28 17:32
 */
public class RollbackPlanRuntimeFlowableTaskTest {
    @Test
    public void testFlowCanceledWatcher() throws InterruptedException {
        FlowInstanceService flowInstanceService = Mockito.mock(FlowInstanceService.class);
        Mockito.when(flowInstanceService.getStatus(ArgumentMatchers.any())).thenReturn(Collections.singletonMap(1024L,
                FlowStatus.EXECUTING));
        RollbackPlanRuntimeFlowableTask.FlowCanceledWatcher flowCanceledWatcher =
                new FlowCanceledWatcher(flowInstanceService, 1024L, 500L);
        long invokeEndTime = System.currentTimeMillis() + 1000;
        int executeCount = 0;
        while (System.currentTimeMillis() < invokeEndTime) {
            flowCanceledWatcher.isCanceled();
            executeCount++;
            Thread.sleep(10);
        }
        Assert.assertTrue(executeCount > 50);
        Mockito.verify(flowInstanceService, Mockito.atMost(4));
    }
}
