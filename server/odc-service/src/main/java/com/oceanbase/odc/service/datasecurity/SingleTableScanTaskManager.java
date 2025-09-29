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
package com.oceanbase.odc.service.datasecurity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Single-table scan task manager
 */
@Slf4j
@Component
public class SingleTableScanTaskManager {

    private final Map<String, SingleTableScanTask> tasks = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("scanSensitiveColumnExecutor")
    private ThreadPoolTaskExecutor executor;

    public String startTask(String taskId, Runnable scanTask) {
        SingleTableScanTask task = new SingleTableScanTask(taskId);
        tasks.put(taskId, task);
        executor.submit(() -> {
            try {
                task.setStatus(TaskStatus.RUNNING);
                scanTask.run();
                task.setStatus(TaskStatus.COMPLETED);
            } catch (Exception e) {
                log.error("Single table scan task failed, taskId={}", taskId, e);
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
            }
        });

        return taskId;
    }

    public String startTask(Runnable scanTask) {
        String taskId = UUID.randomUUID().toString();
        return startTask(taskId, scanTask);
    }

    public SingleTableScanTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public void setTaskResult(String taskId, List<SensitiveColumn> result) {
        SingleTableScanTask task = tasks.get(taskId);
        if (task != null) {
            task.setResult(result);
        }
    }

    public void setTaskError(String taskId, String errorMessage) {
        SingleTableScanTask task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
        }
    }

    public void cleanupTask(String taskId) {
        tasks.remove(taskId);
    }

    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    @Data
    public static class SingleTableScanTask {
        private final String taskId;
        private TaskStatus status = TaskStatus.PENDING;
        private List<SensitiveColumn> result;
        private String errorMessage;
    }
}
