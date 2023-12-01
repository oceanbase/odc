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

import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.task.caller.JobContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask implements Task {

    protected JobContext context;

    protected TaskStatus status;

    protected FlowTaskResult result;

    protected TaskReporter reporter;

    protected volatile double progress = 0;

    protected volatile boolean canceled = false;

    private volatile boolean finished = false;

    public BaseTask(JobContext context) {
        this.context = context;
        this.status = TaskStatus.PREPARING;
        this.reporter = new TaskReporter(context.getHostProperties());
    }

    @Override
    public void start() {
        try {
            updateStatus(TaskStatus.RUNNING);
            log.info("Task started, id: {}, status: {}", context.getJobIdentity().getId(), status);
            initTaskMonitor();
            onStart();
            updateStatus(TaskStatus.DONE);
        } catch (Exception e) {
            onFail(e);
            updateStatus(TaskStatus.FAILED);
            log.warn("Task failed, id: {}, status: {}", context.getJobIdentity().getId(), status, e);
        } finally {
            reportTaskResult();
            finished();
        }
    }

    @Override
    public void stop() {
        try {
            if (finished) {
                log.warn("Task already finished, id: {}, status: {}", context.getJobIdentity().getId(), status);
                return;
            }
            canceled = true;
            updateStatus(TaskStatus.CANCELED);
            log.info("Task stopped, id: {}, status: {}", context.getJobIdentity().getId(), status);
        } catch (Exception e) {
            log.warn("Task stop failed, id: {}, status: {}", context.getJobIdentity().getId(), status, e);
        }
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

    private void initTaskMonitor() {
        ThreadFactory threadFactory = new TaskThreadFactory(("Task-Monitor-" + context.getJobIdentity().getId()));
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (finished) {
                scheduledExecutor.shutdown();
            }
            try {
                reportTaskResult();
            } catch (Exception e) {
                log.warn("Update task progress failed, id: {}", context.getJobIdentity().getId(), e);
            }
        }, 1, 5, TimeUnit.SECONDS);
        log.info("Task monitor init success");
    }

    private void reportTaskResult() {
        onUpdate();
        reporter.report(context.getJobIdentity(), status, progress, result);
        log.info("Task status: {}, progress: {}%, result: {}", status, String.format("%.2f", progress * 100), result);
    }

    private void updateStatus(TaskStatus status) {
        this.status = status;
    }

    private void finished() {
        log.info("Task finished, id: {}, status: {}", context.getJobIdentity().getId(), status);
        finished = true;
    }

}
