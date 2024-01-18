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

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
@Slf4j
public class TaskMonitor implements Closeable {

    private final TaskReporter reporter;
    private final Task<?> task;
    private volatile long startTimeMilliSeconds;
    private ScheduledExecutorService reportScheduledExecutor;
    private ScheduledExecutorService heartScheduledExecutor;

    public TaskMonitor(Task<?> task, TaskReporter reporter) {
        this.task = task;
        this.reporter = reporter;
    }

    public void monitor() {
        this.startTimeMilliSeconds = System.currentTimeMillis();

        ThreadFactory threadFactory =
                new TaskThreadFactory(("Task-Monitor-Job-" + getJobId()));
        this.reportScheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        reportScheduledExecutor.scheduleAtFixedRate(() -> {
            if (isTimeout()) {
                getTask().stop();
            }
            try {
                reportTaskResult();
            } catch (Throwable e) {
                log.warn("Update task info failed, id: {}", getJobId(), e);
            }
        }, 1, JobConstants.REPORT_TASK_INFO_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Task monitor init success");

        heartScheduledExecutor = Executors.newSingleThreadScheduledExecutor(
                new TaskThreadFactory(("Task-Heart-Job-" + getJobId())));

        heartScheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                getReporter().report(JobUrlConstants.TASK_HEART, buildHeartRequest());
            } catch (Throwable e) {
                log.warn("Update heart info failed, id: {}", getJobId(), e);
            }
        }, 1, JobConstants.REPORT_TASK_HEART_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Task heart init success");
    }

    @Override
    public void close() {
        close(reportScheduledExecutor);
        close(heartScheduledExecutor);
    }

    private void close(ExecutorService executorService) {
        try {
            if (executorService != null) {
                executorService.shutdownNow();
            }
        } catch (Throwable ex) {
            // shutdown quietly
        }
    }

    private void reportTaskResult() {
        DefaultTaskResult taskResult = DefaultTaskResultBuilder.build(getTask());
        if (taskResult.getStatus().isTerminated()) {
            log.info("job {} status {} is terminate, monitor report be ignored.",
                    taskResult.getJobIdentity().getId(), taskResult.getStatus());
            return;
        }
        DefaultTaskResult copiedResult = ObjectUtil.deepCopy(taskResult, DefaultTaskResult.class);
        getReporter().report(JobUrlConstants.TASK_RESULT_UPLOAD, copiedResult);
        log.info("Report task info, id: {}, status: {}, progress: {}%, result: {}", getJobId(),
                copiedResult.getStatus(), String.format("%.2f", copiedResult.getProgress()), getTask().getTaskResult());
    }

    private boolean isTimeout() {
        String milliSecStr =
                getTask().getJobContext().getJobParameters()
                        .get(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS);

        if (milliSecStr != null) {
            return System.currentTimeMillis() - startTimeMilliSeconds > Long.parseLong(milliSecStr);
        } else {
            return false;
        }
    }

    private HeartRequest buildHeartRequest() {
        HeartRequest request = new HeartRequest();
        request.setJobIdentity(getTask().getJobContext().getJobIdentity());
        request.setExecutorEndpoint(JobUtils.getExecutorPoint());
        return request;
    }

    private Task<?> getTask() {
        return task;
    }

    private TaskReporter getReporter() {
        return reporter;
    }

    private Long getJobId() {
        return getTask().getJobContext().getJobIdentity().getId();
    }
}
