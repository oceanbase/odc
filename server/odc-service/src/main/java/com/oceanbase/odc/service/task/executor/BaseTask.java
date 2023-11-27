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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.caller.JobContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask implements Task {

    protected final JobContext context;

    protected TaskStatus status = TaskStatus.PREPARING;

    protected FlowTaskResult result;

    protected boolean stopped = false;

    protected boolean finished = false;

    protected double progress = 0;

    public BaseTask(JobContext context) {
        this.context = context;
    }

    @Override
    public void start() {
        log.info("Start task, id: {}", context.getJobIdentity().getId());
        try {
            initTaskMonitor();
            doStart();
        } catch (Exception e) {
            log.error("Task run failed, id: {}", context.getJobIdentity().getId(), e);
            onFailure(e);
        } finally {
            uploadResults();
        }
    }

    @Override
    public void stop() {
        log.info("Stop task, id: {}", context.getJobIdentity().getId());
        try {
            doStop();
        } catch (Exception e) {
            log.error("Task stop failed, id: {}", context.getJobIdentity().getId(), e);
            onFailure(e);
        } finally {
            uploadResults();
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public boolean isFinished() {
        return finished;
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

    protected abstract void doStart();

    protected abstract void doStop();

    protected abstract void onFailure(Exception e);

    protected abstract void onUpdate();

    private void initTaskMonitor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Task-" + context.getJobIdentity().getId() + "-Monitor-%d")
                .build();
        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1, threadFactory);
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                updateStatusAndProgress();
            } catch (Exception e) {
                log.warn("Update task progress failed, id: {}", context.getJobIdentity().getId(), e);
            }
            if (isStopped() || isFinished()) {
                scheduledExecutor.shutdown();
            }
        }, 1, 5, TimeUnit.SECONDS);
        log.info("Task monitor init success");
    }

    private void updateStatusAndProgress() {
        onUpdate();
        log.info("Task status: {}, progress: {}%", this.status, String.format("%.2f", this.progress * 100));
    }

    private void uploadResults() {
        log.info("Task result: {}", this.result);
    }

}
