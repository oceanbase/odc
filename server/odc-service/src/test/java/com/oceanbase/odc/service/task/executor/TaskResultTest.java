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
package com.oceanbase.odc.service.task.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author longpeng.zlp
 * @date 2024/11/11 15:09
 */
public class TaskResultTest {
    @Test
    public void testTaskResult() {
        Map<String, String> logMetadata1 = new HashMap() {
            {
                put("key1", "value1");
                put("key2", "value2");
            }
        };

        Map<String, String> logMetadata2 = new TreeMap() {
            {
                put("key1", null);
            }
        };

        TaskResult result1 = createTaskResult(TaskStatus.RUNNING, 0.877, logMetadata1, "res1");
        Assert.assertFalse(result1.isProgressChanged(result1));
        Assert.assertTrue(result1.isProgressChanged(null));
        // log meta changed
        TaskResult result2 = createTaskResult(TaskStatus.RUNNING, 0.877, logMetadata2, "res1");
        Assert.assertTrue(result1.isProgressChanged(result2));
        // statue changed
        TaskResult result3 = createTaskResult(TaskStatus.DONE, 0.877, logMetadata1, "res1");
        Assert.assertTrue(result1.isProgressChanged(result3));
        // result json changed
        TaskResult result4 = createTaskResult(TaskStatus.RUNNING, 0.877, logMetadata1, null);
        Assert.assertTrue(result1.isProgressChanged(result4));
        // result progress changed
        TaskResult result5 = createTaskResult(TaskStatus.RUNNING, 0.8774, logMetadata1, "res1");
        Assert.assertTrue(result1.isProgressChanged(result5));
    }

    private TaskResult createTaskResult(TaskStatus taskStatus, double progress, Map<String, String> logMetadata,
            String resultJson) {
        TaskResult taskResult = new TaskResult();
        taskResult.setJobIdentity(JobIdentity.of(1L));
        taskResult.setResultJson(resultJson);
        taskResult.setProgress(progress);
        taskResult.setStatus(taskStatus);
        taskResult.setLogMetadata(logMetadata);
        return taskResult;
    }
}
