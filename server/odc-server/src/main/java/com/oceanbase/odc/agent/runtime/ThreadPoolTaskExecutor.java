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
package com.oceanbase.odc.agent.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.executor.TraceDecoratorThreadFactory;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.util.CloudObjectStorageServiceBuilder;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * A thread pool task executor.
 * 
 * @author gaoda.xy
 * @date 2023/11/24 11:22
 */
@Slf4j
class ThreadPoolTaskExecutor implements TaskExecutor {

    private static final TaskExecutor TASK_EXECUTOR = new ThreadPoolTaskExecutor();
    private final Map<JobIdentity, TaskRuntimeInfo> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    private ThreadPoolTaskExecutor() {
        this.executor = new ThreadPoolExecutor(2, 2, 1000, TimeUnit.SECONDS, new LinkedBlockingQueue<>(16),
                new TraceDecoratorThreadFactory(new TaskThreadFactory("Task-Executor")), new CallerRunsPolicy());
    }

    public static TaskExecutor getInstance() {
        return TASK_EXECUTOR;
    }

    @Override
    synchronized public void execute(Task<?> task, JobContext jc) {
        JobIdentity jobIdentity = jc.getJobIdentity();
        log.info("Start to execute task, jobIdentity={}.", jobIdentity.getId());

        if (tasks.containsKey(jobIdentity)) {
            throw new IllegalArgumentException("Task already exists, jobIdentity=" + jobIdentity.getId());
        }
        // init cloud objet storage service and task monitor
        CloudObjectStorageService cloudObjectStorageService = buildCloudStorageService(jc);
        TaskReporter taskReporter = new TaskReporter(jc.getHostUrls());
        TaskContainer<?> taskContainer = new TaskContainer<>(jc, cloudObjectStorageService, taskReporter, task);
        TaskRuntimeInfo taskRuntimeInfo = new TaskRuntimeInfo(taskContainer, taskContainer.getTaskMonitor());
        // first put data in map, avoid concurrent thread access caused task not found exception
        tasks.put(jobIdentity, taskRuntimeInfo);
        Future<?> future = executor.submit(() -> {
            try {
                taskContainer.runTask();
            } catch (Exception e) {
                log.error("Task start failed, jobIdentity={}.", jobIdentity.getId(), e);
                taskContainer.onException(e);
            } finally {
                taskContainer.closeTaskContainer();
            }
        });
        taskRuntimeInfo.setFuture(future);
    }

    /**
     * build task monitor
     *
     * @param jobContext
     * @return
     */
    protected CloudObjectStorageService buildCloudStorageService(JobContext jobContext) {
        Optional<ObjectStorageConfiguration> storageConfig = JobUtils.getObjectStorageConfiguration();
        CloudObjectStorageService cloudObjectStorageService = null;
        try {
            if (storageConfig.isPresent()) {
                cloudObjectStorageService = CloudObjectStorageServiceBuilder.build(storageConfig.get());
            }
        } catch (Throwable e) {
            log.warn("Init cloud object storage service failed, id={}.", jobContext.getJobIdentity().getId(), e);
        }
        return cloudObjectStorageService;
    }

    @Override
    public boolean cancel(JobIdentity ji) {
        TaskRuntimeInfo runtimeInfo = getTaskRuntimeInfo(ji);
        TaskContainer<?> task = runtimeInfo.getTaskContainer();
        Future<Boolean> stopFuture = executor.submit(task::stopTask);
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
    public TaskRuntimeInfo getTaskRuntimeInfo(JobIdentity ji) {
        TaskRuntimeInfo runtimeInfo = tasks.get(ji);
        PreConditions.notNull(runtimeInfo, "task", "Task not found, jobIdentity=" + ji.getId());
        return runtimeInfo;
    }

    @Override
    public boolean taskExist(JobIdentity ji) {
        return tasks.get(ji) != null;
    }
}
