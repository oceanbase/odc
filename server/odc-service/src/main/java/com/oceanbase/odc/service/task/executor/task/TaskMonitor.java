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

import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
@Slf4j
public class TaskMonitor {

    private volatile Task<?> task;
    private final TaskReporter reporter;
    private volatile long startTimeMilliSeconds;


    public TaskMonitor(Task<?> task, TaskReporter reporter) {
        this.task = task;
        this.reporter = reporter;
    }

    public void monitor() {
        this.startTimeMilliSeconds = System.currentTimeMillis();

        ThreadFactory threadFactory =
                new TaskThreadFactory(("Task-Monitor-" + getJobId()));
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduledExecutor.scheduleAtFixedRate(() -> {

            if (isTimeout()) {
                getTask().stop();
            }
            if (getTask().getStatus().isTerminated()) {
                scheduledExecutor.shutdown();
            } else {
                try {
                    reportTaskResult();
                } catch (Throwable e) {
                    log.warn("Update task info failed, id: {}", getJobId(), e);
                }
            }
        }, 1, JobConstants.REPORT_TASK_INFO_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Task monitor init success");

        ScheduledExecutorService heartScheduledExecutor = Executors.newSingleThreadScheduledExecutor(
                new TaskThreadFactory(("Task-Heart-" + getJobId())));

        heartScheduledExecutor.scheduleAtFixedRate(() -> {
            if (getTask().getStatus().isTerminated()) {
                heartScheduledExecutor.shutdown();
            } else {
                try {
                    getReporter().report(JobUrlConstants.TASK_HEART, buildHeartRequest());
                } catch (Throwable e) {
                    log.warn("Update heart info failed, id: {}", getJobId(), e);
                }
            }
        }, 1, JobConstants.REPORT_TASK_HEART_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Task heart init success");
    }

    private void reportTaskResult() {
        double progress = getTask().getProgress();
        if (new BigDecimal(progress).compareTo(new BigDecimal("100.0")) > 0) {
            log.warn("progress value {} is illegal, bigger than 100.0", progress);
            return;
        }
        // onUpdate();
        if (getTask().getStatus() == JobStatus.DONE) {
            progress = 100.0;
        }
        getReporter().report(JobUrlConstants.TASK_RESULT_UPLOAD, DefaultTaskResultBuilder.build(getTask()));
        log.info("Report task info, id: {}, status: {}, progress: {}%, result: {}",
                getJobId(),
                getTask().getStatus(), String.format("%.2f", progress), getTask().getTaskResult());
    }

    private boolean isTimeout() {
        String milliSecStr =
                getTask().getJobContext().getJobParameters().get(JobDataMapConstants.TASK_EXECUTION_TIMEOUT_MILLIS);

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
