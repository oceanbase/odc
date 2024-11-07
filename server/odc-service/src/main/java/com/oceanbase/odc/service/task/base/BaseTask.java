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
package com.oceanbase.odc.service.task.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.TaskContext;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask<RESULT> implements Task<RESULT> {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    protected TaskContext context;
    private DefaultJobContext jobContext;
    private volatile TaskStatus status = TaskStatus.PREPARING;


    @Override
    public void start(TaskContext taskContext) {
        this.context = taskContext;
        jobContext = copyJobContext(taskContext);
        log.info("Start task, id={}.", jobContext.getJobIdentity().getId());
        log.info("Init task parameters success, id={}.", jobContext.getJobIdentity().getId());

        try {
            doInit(jobContext);
            updateStatus(TaskStatus.RUNNING);
            context.getTaskEventListener().onTaskStart(this);
            if (doStart(jobContext, taskContext)) {
                updateStatus(TaskStatus.DONE);
            } else {
                updateStatus(TaskStatus.FAILED);
            }
        } catch (Throwable e) {
            log.warn("Task failed, id={}.", getJobId(), e);
            updateStatus(TaskStatus.FAILED);
            taskContext.getExceptionListener().onException(e);
        } finally {
            close();
        }
    }

    @Override
    public boolean stop() {
        try {
            if (getStatus().isTerminated()) {
                log.warn("Task is already finished and cannot be canceled, id={}, status={}.", getJobId(), getStatus());
            } else {
                doStop();
                // doRefresh cannot execute if update status to 'canceled'.
                updateStatus(TaskStatus.CANCELED);
            }
            return true;
        } catch (Throwable e) {
            log.warn("Stop task failed, id={}", getJobId(), e);
            return false;
        } finally {
            close();
        }
    }

    @Override
    public boolean modify(Map<String, String> jobParameters) {
        if (Objects.isNull(jobParameters) || jobParameters.isEmpty()) {
            log.warn("Job parameter cannot be null, id={}", getJobId());
            return false;
        }
        if (getStatus().isTerminated()) {
            log.warn("Task is already finished, cannot modify parameters, id={}", getJobId());
            return false;
        }
        jobContext.setJobParameters(Collections.unmodifiableMap(jobParameters));
        try {
            afterModifiedJobParameters();
        } catch (Exception e) {
            log.warn("Do after modified job parameters failed", e);
        }
        return true;
    }


    private void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                doClose();
            } catch (Throwable e) {
                // do nothing
            }
            log.info("Task completed, id={}, status={}.", getJobId(), getStatus());
            if (null != context) {
                context.getTaskEventListener().onTaskFinalize(this);
            }
        }
    }

    @Override
    public TaskStatus getStatus() {
        return status;
    }

    @Override
    public JobContext getJobContext() {
        return jobContext;
    }

    protected void updateStatus(TaskStatus status) {
        log.info("Update task status, id={}, status={}.", getJobId(), status);
        this.status = status;
    }

    protected Map<String, String> getJobParameters() {
        return jobContext.getJobParameters();
    }

    private Long getJobId() {
        return getJobContext().getJobIdentity().getId();
    }

    protected abstract void doInit(JobContext context) throws Exception;

    /**
     * start a task return succeed or failed after completed.
     *
     * @return return true if execute succeed, else return false
     */
    protected abstract boolean doStart(JobContext context, TaskContext taskContext) throws Exception;

    protected abstract void doStop() throws Exception;

    /**
     * task can release relational resource in this method
     */
    protected abstract void doClose() throws Exception;

    protected void afterModifiedJobParameters() throws Exception {
        // do nothing
    }

    // deep copy job context
    protected DefaultJobContext copyJobContext(TaskContext taskContext) {
        DefaultJobContext ret = new DefaultJobContext();
        JobContext src = taskContext.getJobContext();
        ret.setJobIdentity(src.getJobIdentity());
        if (null != src.getJobProperties()) {
            ret.setJobProperties(new HashMap<>(src.getJobProperties()));
        }
        if (null != src.getJobParameters()) {
            ret.setJobParameters(Collections.unmodifiableMap(new HashMap<>(src.getJobParameters())));
        }
        ret.setJobClass(src.getJobClass());
        if (null != src.getHostUrls()) {
            ret.setHostUrls(new ArrayList<>(src.getHostUrls()));
        }
        return ret;
    }
}
