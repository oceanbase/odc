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
package com.oceanbase.odc.service.task.processor;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.task.executor.task.TaskDescription;
import com.oceanbase.odc.service.task.processor.matcher.DLMProcessorMatcher;
import com.oceanbase.odc.service.task.processor.matcher.LogicalDBChangeProcessorMatcher;

/**
 * @author longpeng.zlp
 * @date 2024/10/11 14:59
 */
public class ProcessorMatcherTest {
    @Test
    public void testProcessorMatcher() {
        TaskDescription[] taskDescriptions = new TaskDescription[] {
                TaskDescription.DLM, TaskDescription.LOGICAL_DATABASE_CHANGE
        };
        ProcessorMatcher[] processorMatchers = new ProcessorMatcher[] {
                new DLMProcessorMatcher(),
                new LogicalDBChangeProcessorMatcher()
        };
        Assert.assertEquals(taskDescriptions.length, processorMatchers.length);
        for (int i = 0; i < taskDescriptions.length; ++i) {
            // same position interested
            Assert.assertTrue(processorMatchers[i].interested(taskDescriptions[i].getType()));
            // remains not interested
            for (int j = 0; j < taskDescriptions.length; ++j) {
                if (j == i) {
                    continue;
                }
                Assert.assertFalse(processorMatchers[i].interested(taskDescriptions[j].getType()));
            }
        }
    }
}
