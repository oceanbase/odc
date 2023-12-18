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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobUtils;
import com.oceanbase.odc.service.task.model.ExecutorInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask implements Task {

    protected JobContext context;

    protected TaskStatus status = TaskStatus.PREPARING;

    protected FlowTaskResult result;

    protected TaskReporter reporter;

    protected volatile double progress = 0;

    protected volatile boolean canceled = false;

    private volatile boolean finished = false;

    private static final int REPORT_TASK_INFO_INTERVAL_SECONDS = 5;
    private static final int REPORT_RESULT_RETRY_TIMES = 10;
    private static final int REPORT_RESULT_RETRY_INTERVAL_SECONDS = 10;

    @Override
    public void start(JobContext context) {
        try {
            this.context = context;
            this.reporter = new TaskReporter(context.getHostUrls());
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
        if (finished) {
            log.warn("Task is already finished and cannot be canceled, id: {}", context.getJobIdentity().getId());
            return;
        }
        canceled = true;
        updateStatus(TaskStatus.CANCELED);
        log.info("Task canceled, id: {}", context.getJobIdentity().getId());
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public double progress() {
        return progress;
    }

    @Override
    public JobContext context() {
        return context;
    }

    @Override
    public TaskStatus status() {
        return status;
    }

    @Override
    public FlowTaskResult result() {
        return result;
    }

    /**
     * Deal with task run logic here
     */
    protected abstract void onStart();

    /**
     * Deal with task stop logic here
     * 
     * @param e exception
     */
    protected abstract void onFail(Exception e);

    /**
     * Deal with task update logic here, will be invoked by {@link BaseTask#initTaskMonitor()}
     */
    protected abstract void onUpdate();

    protected void updateStatus(TaskStatus status) {
        this.status = status;
    }

    private void initTaskMonitor() {
        ThreadFactory threadFactory = new TaskThreadFactory(("Task-Monitor-" + context.getJobIdentity().getId()));
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (finished || this.status.isTerminated()) {
                scheduledExecutor.shutdown();
            }
            try {
                reportTaskResult();
            } catch (Exception e) {
                log.warn("Update task info failed, id: {}", context.getJobIdentity().getId(), e);
            }
        }, 1, REPORT_TASK_INFO_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Task monitor init success");
    }

    private void reportTaskResult() {
        onUpdate();
        if (this.status == TaskStatus.DONE) {
            this.progress = 1.0;
        }
        reporter.report(buildCurrentResult());
        log.info("Report task info, id: {}, status: {}, progress: {}%, result: {}", context.getJobIdentity().getId(),
                status, String.format("%.2f", progress * 100), result);
    }

    private void doFinal() {
        onUpdate();
        if (this.status == TaskStatus.DONE) {
            this.progress = 1.0;
        }
        log.info("Task finished with status: {}, id: {}, start to report final result",
                context.getJobIdentity().getId(), status);
        DefaultTaskResult finalResult = buildCurrentResult();
        finalResult.setFinished(true);
        int retryTimes = 0;
        while (retryTimes < REPORT_RESULT_RETRY_TIMES) {
            try {
                retryTimes++;
                boolean success = reporter.report(finalResult);
                if (success) {
                    log.info("Report final result reported successfully");
                    break;
                } else {
                    log.warn("Report final result failed, will retry after {} seconds, remaining retries: {}",
                            REPORT_RESULT_RETRY_INTERVAL_SECONDS, REPORT_RESULT_RETRY_TIMES - retryTimes);
                    Thread.sleep(REPORT_RESULT_RETRY_INTERVAL_SECONDS * 1000);
                }
            } catch (Exception e) {
                log.warn("Report final result failed, taskId: {}", context.getJobIdentity().getId(), e);
            }
        }
        // TODO: May solve log file here
        this.finished = true;
    }

    private DefaultTaskResult buildCurrentResult() {
        DefaultTaskResult result = new DefaultTaskResult();
        result.setResultJson(JsonUtils.toJson(this.result));
        result.setTaskStatus(this.status);
        result.setProgress(this.progress);
        result.setFinished(false);
        result.setJobIdentity(this.context.getJobIdentity());
        ExecutorInfo ei = new ExecutorInfo();
        ei.setHost(SystemUtils.getLocalIpAddress());
        ei.setPort(JobUtils.getPort());
        ei.setHostName(SystemUtils.getHostName());
        ei.setPid(SystemUtils.getPid());
        ei.setJvmStartTime(SystemUtils.getJVMStartTime());
        result.setExecutorInfo(ei);
        return result;
    }

}
