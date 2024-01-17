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

package com.oceanbase.odc.service.task.executor.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.task.caller.JobContext;
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
    private final Map<JobIdentity, Task<?>> tasks = new HashMap<>();
    private final Map<JobIdentity, Future<?>> futures = new HashMap<>();
    private final ExecutorService executor;

    private ThreadPoolTaskExecutor() {
        this.executor = Executors.newFixedThreadPool(2,
                new TaskThreadFactory("Task-Executor"));
    }

    public static TaskExecutor getInstance() {
        return TASK_EXECUTOR;
    }

    @Override
    public void execute(Task<?> task, JobContext jc) {
        Future<?> future = executor.submit(() -> task.start(jc));
        tasks.put(jc.getJobIdentity(), task);
        futures.put(jc.getJobIdentity(), future);
    }

    @Override
    public boolean cancel(JobIdentity ji) {
        Future<?> startFuture = futures.get(ji);
        if (startFuture.isDone()) {
            return true;
        }
        Task<?> task = tasks.get(ji);
        Future<Boolean> stopFuture = executor.submit(task::stop);
        boolean result = false;
        try {
            // wait 30 seconds for stop task accomplished
            result = stopFuture.get(30 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("Stop task {} is be interrupted.", ji.getId(), e);
        } catch (ExecutionException e) {
            log.warn("Stop task {} execution exception.", ji.getId(), e);
        } catch (TimeoutException e) {
            log.warn("Stop task {} time out.", ji.getId(), e);
        }
        return result || startFuture.cancel(true);
    }

    @Override
    public Task<?> getTask(JobIdentity ji) {
        return tasks.get(ji);
    }
}
