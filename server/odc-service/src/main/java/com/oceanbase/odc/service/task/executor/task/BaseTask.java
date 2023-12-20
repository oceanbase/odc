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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask implements Task {

    private static final int REPORT_TASK_INFO_INTERVAL_SECONDS = 5;
    private static final int REPORT_RESULT_RETRY_TIMES = 10;
    private static final int REPORT_RESULT_RETRY_INTERVAL_SECONDS = 10;
    private static final int DEFAULT_TASK_TIMEOUT_MILLI_SECONDS = 48 * 60 * 60 * 1000;

    private JobContext context;
    private Map<String, String> jobData;
    private TaskReporter reporter;

    private volatile TaskStatus status = TaskStatus.PREPARING;
    private volatile boolean canceled = false;
    private volatile boolean finished = false;
    private volatile long startTimeMilliSeconds;

    @Override
    public void start(JobContext context) {
        this.startTimeMilliSeconds = System.currentTimeMillis();
        this.context = context;
        try {
            this.jobData = Collections.unmodifiableMap(getJobContext().getJobData());
            this.reporter = new TaskReporter(context.getHostUrls());
            onInit();
            initTaskMonitor();
            onStart();
        } catch (Exception e) {
            log.info("Task failed, id: {}, details: {}", context.getJobIdentity().getId(), e);
            updateStatus(TaskStatus.FAILED);
            onFail(e);
        } finally {
            doFinal();
        }
    }

    @Override
    public void stop() {
        if (isFinished()) {
            log.warn("Task is already finished and cannot be canceled, id: {}",
                    getJobContext().getJobIdentity().getId());
            return;
        }
        canceled = true;
        onStop();
        updateStatus(TaskStatus.CANCELED);
        log.info("Task canceled, id: {}", getJobContext().getJobIdentity().getId());
    }

    @Override
    public boolean isFinished() {
        return finished;
    }


    @Override
    public TaskStatus getTaskStatus() {
        return status;
    }

    @Override
    public JobContext getJobContext() {
        return context;
    }

    protected void updateStatus(TaskStatus status) {
        this.status = status;
    }

    protected Map<String, String> getJobData() {
        return this.jobData;
    }

    protected boolean isCanceled() {
        return canceled;
    }

    private boolean isTimeout() {
        String milliSecStr = getJobData().get(JobDataMapConstants.TIMEOUT_MILLI_SECONDS);
        long milliSec = milliSecStr != null ? Long.parseLong(milliSecStr) : DEFAULT_TASK_TIMEOUT_MILLI_SECONDS;
        return System.currentTimeMillis() - startTimeMilliSeconds > milliSec;
    }

    private void initTaskMonitor() {
        ThreadFactory threadFactory =
                new TaskThreadFactory(("Task-Monitor-" + getJobContext().getJobIdentity().getId()));
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduledExecutor.scheduleAtFixedRate(() -> {
            // check task is timeout or not
            if (isTimeout()) {
                // when task execution is timeout then stop it.
                stop();
            }
            if (isFinished() || getTaskStatus().isTerminated()) {
                scheduledExecutor.shutdown();
            }
            try {
                reportTaskResult();
            } catch (Exception e) {
                log.warn("Update task info failed, id: {}", getJobContext().getJobIdentity().getId(), e);
            }
        }, 1, REPORT_TASK_INFO_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Task monitor init success");
    }

    private void doFinal() {
        // Report final result
        // onUpdate();
        log.info("Task id: {}, finished with status: {}, start to report final result",
                getJobContext().getJobIdentity().getId(), getTaskStatus());
        DefaultTaskResult finalResult = buildCurrentResult();
        reportTaskResultWithRetry(finalResult, REPORT_RESULT_RETRY_TIMES, REPORT_RESULT_RETRY_INTERVAL_SECONDS);

        // TODO: May solve log file here
        //String logPath = JobUtils.getLogPath();


        // Report finish signal to task server
        finalResult.setFinished(true);
        reportTaskResultWithRetry(finalResult, REPORT_RESULT_RETRY_TIMES, REPORT_RESULT_RETRY_INTERVAL_SECONDS);
        this.finished = true;
    }

    private void reportTaskResult() {
        double progress = getProgress();
        // onUpdate();
        if (getTaskStatus() == TaskStatus.DONE) {
            progress = 1.0;
        }
        reporter.report(buildCurrentResult());
        log.info("Report task info, id: {}, status: {}, progress: {}%, result: {}",
                getJobContext().getJobIdentity().getId(),
                getTaskStatus(), String.format("%.2f", progress * 100), getTaskResult());
    }

    private void reportTaskResultWithRetry(TaskResult result, int retries, int intervalSeconds) {
        int retryTimes = 0;
        while (retryTimes < retries) {
            try {
                retryTimes++;
                boolean success = reporter.report(result);
                if (success) {
                    log.info("Report task result successfully");
                    break;
                } else {
                    log.warn("Report task result failed, will retry after {} seconds, remaining retries: {}",
                            intervalSeconds, retries - retryTimes);
                    Thread.sleep(intervalSeconds * 1000L);
                }
            } catch (Exception e) {
                log.warn("Report task result failed, taskId: {}", getJobContext().getJobIdentity().getId(), e);
            }
        }
    }

    private DefaultTaskResult buildCurrentResult() {
        DefaultTaskResult result = new DefaultTaskResult();
        result.setResultJson(JsonUtils.toJson(getTaskResult()));
        result.setTaskStatus(getTaskStatus());
        result.setProgress(getProgress());
        result.setFinished(false);
        result.setJobIdentity(getJobContext().getJobIdentity());
        ExecutorInfo ei = new ExecutorInfo();
        ei.setHost(SystemUtils.getLocalIpAddress());
        ei.setPort(JobUtils.getPort());
        ei.setHostName(SystemUtils.getHostName());
        ei.setPid(SystemUtils.getPid());
        ei.setJvmStartTime(SystemUtils.getJVMStartTime());
        result.setExecutorInfo(ei);
        return result;
    }


    /**
     * Deal with task init method
     */
    protected abstract void onInit();


    /**
     * Deal with task run logic here
     */
    protected abstract void onStart();



    /**
     * Deal with task run logic here
     */
    protected abstract void onStop();

    /**
     * Deal with task stop logic here
     *
     * @param e exception
     */
    protected abstract void onFail(Exception e);

}
