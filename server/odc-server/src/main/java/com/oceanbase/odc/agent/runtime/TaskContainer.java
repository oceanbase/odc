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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.ExceptionListener;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.TaskContext;
import com.oceanbase.odc.service.task.caller.JobContext;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
final class TaskContainer<RESULT> implements ExceptionListener {
    // if task has been closed
    private final AtomicBoolean closed = new AtomicBoolean(false);
    // status maintained by task container
    private volatile TaskStatus status = TaskStatus.PREPARING;
    // task event listener for task event notify
    @Getter
    protected final TaskMonitor taskMonitor;
    // task context for task to init
    protected final TaskContext taskContext;
    // task instance holding
    @Getter
    private final Task<RESULT> task;
    // only save latest exception if any
    // it will be cleaned if been fetched
    protected AtomicReference<Throwable> latestException = new AtomicReference<>();
    // check if fina work has done
    protected AtomicBoolean finalWorkDone = new AtomicBoolean(false);


    public TaskContainer(JobContext jobContext, CloudObjectStorageService cloudObjectStorageService,
            TaskReporter taskReporter, // assignable for test
            Task<RESULT> task) {
        this.taskContext = createTaskContext(jobContext, cloudObjectStorageService);
        this.task = task;
        this.taskMonitor = new TaskMonitor(this, taskReporter, cloudObjectStorageService);
    }

    protected TaskContext createTaskContext(JobContext jobContext,
            CloudObjectStorageService cloudObjectStorageService) {
        ExceptionListener exceptionListener = this;
        return new TaskContext() {
            @Override
            public ExceptionListener getExceptionListener() {
                return exceptionListener;
            }

            @Override
            public JobContext getJobContext() {
                return jobContext;
            }

            @Override
            public CloudObjectStorageService getSharedStorage() {
                return cloudObjectStorageService;
            }
        };
    }

    /**
     * 1. init task 2. run task 3. maintain status
     */
    public void runTask() {
        try {
            task.init(taskContext);
            updateStatus(TaskStatus.RUNNING);
            taskMonitor.monitor();
            if (task.start()) {
                updateStatus(TaskStatus.DONE);
            } else {
                updateStatus(TaskStatus.FAILED);
            }
        } catch (Throwable e) {
            log.warn("Task failed, id={}.", getJobId(), e);
            updateStatus(TaskStatus.FAILED);
            onException(e);
        } finally {
            closeTask();
        }
    }

    public boolean stopTask() {
        try {
            if (getStatus().isTerminated()) {
                log.warn("Task is already finished and cannot be canceled, id={}, status={}.", getJobId(), getStatus());
            } else {
                task.stop();
                // doRefresh cannot execute if update status to 'canceled'.
                updateStatus(TaskStatus.CANCELED);
            }
            return true;
        } catch (Throwable e) {
            log.warn("Stop task failed, id={}", getJobId(), e);
            return false;
        } finally {
            closeTask();
        }
    }

    public boolean modifyTask(Map<String, String> jobParameters) {
        if (Objects.isNull(jobParameters) || jobParameters.isEmpty()) {
            log.warn("Job parameter cannot be null, id={}", getJobId());
            return false;
        }
        if (getStatus().isTerminated()) {
            log.warn("Task is already finished, cannot modify parameters, id={}", getJobId());
            return false;
        }
        task.modify(jobParameters);
        return true;
    }


    private void closeTask() {
        if (closed.compareAndSet(false, true)) {
            try {
                task.close();
            } catch (Throwable e) {
                // do nothing
            }
            log.info("Task completed, id={}, status={}.", getJobId(), getStatus());
        }
    }

    public synchronized void closeTaskContainer() {
        if (!finalWorkDone.compareAndSet(false, true)) {
            log.info("final work has done");
            return;
        }
        try {
            taskMonitor.finalWork();
        } catch (Throwable e) {
            log.info("do final work failed", e);
        }
    }

    public TaskStatus getStatus() {
        return status;
    }


    private void updateStatus(TaskStatus status) {
        TaskStatus prevStatus = this.status;
        if (!this.status.isTerminated()) {
            this.status = status;
            log.info("Update task status, id={}, from prev = {} to  status={}.", getJobId(), prevStatus, status);
        } else {
            log.info("Status has terminated, , id={}, status={}. ignore transfer tp status = {}", getJobId(),
                    prevStatus, status);
        }
    }

    protected Map<String, String> getJobParameters() {
        return task.getJobContext().getJobParameters();
    }

    private Long getJobId() {
        return task.getJobContext().getJobIdentity().getId();
    }

    public Throwable getError() {
        Throwable e = latestException.getAndSet(null);
        log.info("retrieve exception = {}", null == e ? null : e.getMessage());
        return e;
    }

    public void onException(Throwable e) {
        log.info("found exception", e);
        this.latestException.set(e);
    }
}
