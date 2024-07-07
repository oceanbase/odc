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

package com.oceanbase.odc.service.task.executor.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.executor.task.BaseTask;
import com.oceanbase.odc.service.task.executor.task.Task;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.extern.slf4j.Slf4j;

/**
 * A thread pool task executor.
 * 
 * @author gaoda.xy
 * @date 2023/11/24 11:22
 */
@Slf4j
public class ThreadPoolTaskExecutor implements TaskExecutor {

    private static final TaskExecutor TASK_EXECUTOR = new ThreadPoolTaskExecutor();
    private final Map<JobIdentity, BaseTask<?>> tasks = new HashMap<>();
    private final Map<JobIdentity, Future<?>> futures = new HashMap<>();
    private final ExecutorService executor;

    private ThreadPoolTaskExecutor() {
        this.executor = Executors.newFixedThreadPool(2,
                new TraceDecoratorThreadFactory(new TaskThreadFactory("Task-Executor")));
    }

    public static TaskExecutor getInstance() {
        return TASK_EXECUTOR;
    }

    @Override
    synchronized public void execute(BaseTask<?> task, JobContext jc) {
        JobIdentity jobIdentity = jc.getJobIdentity();
        log.info("Start to execute task, jobIdentity={}.", jobIdentity.getId());

        if (tasks.containsKey(jobIdentity)) {
            throw new IllegalArgumentException("Task already exists, jobIdentity=" + jobIdentity.getId());
        }
        Future<?> future = executor.submit(() -> {
            try {
                task.start(jc);
            } catch (Exception e) {
                log.error("Task start failed, jobIdentity={}.", jobIdentity.getId(), e);
            }
        });
        futures.put(jobIdentity, future);
        tasks.put(jobIdentity, task);
    }

    @Override
    public boolean cancel(JobIdentity ji) {
        Task<?> task = getTask(ji);
        Future<Boolean> stopFuture = executor.submit(task::stop);
        boolean result = false;
        try {
            // wait 10 seconds for stop task accomplished
            result = stopFuture.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("Stop task is be interrupted, taskId={}.", ji.getId(), e);
        } catch (ExecutionException e) {
            log.warn("Stop task execution exception, taskId={}.", ji.getId(), e);
        } catch (TimeoutException e) {
            log.warn("Stop task time out, taskId={} .", ji.getId(), e);
        }
        if (!result) {
            // if task is terminated, this method should return true,
            // current status is CANCELING must push to CANCELED
            result = task.getStatus().isTerminated();
        }
        ExecutorUtils.gracefulShutdown(executor, "Task-Executor", result ? 1 : 5);
        log.info("Task be canceled succeed, taskId={}, status={}, result={}.",
                ji.getId(), task.getStatus(), result);
        return true;
    }

    @Override
    public BaseTask<?> getTask(JobIdentity ji) {
        BaseTask<?> task = tasks.get(ji);
        PreConditions.notNull(task, "task", "Task not found, jobIdentity=" + ji.getId());
        return task;
    }
}
