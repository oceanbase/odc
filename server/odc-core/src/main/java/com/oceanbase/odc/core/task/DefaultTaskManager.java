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
package com.oceanbase.odc.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.event.LocalEventPublisher;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultTaskManager extends AbstractEventListener<TaskCompleteEvent> implements TaskManager {

    private static final int CORE_POOL_SIZE = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
    private final ThreadPoolExecutor asyncExecutor;
    private final AtomicLong liveTaskCounter = new AtomicLong(0);
    private final EventPublisher publisher = new LocalEventPublisher();
    private volatile boolean closed;

    public DefaultTaskManager(@NonNull ThreadPoolExecutor asyncExecutor) {
        this.publisher.addEventListener(this);
        this.asyncExecutor = asyncExecutor;
    }

    public DefaultTaskManager(@NonNull String taskManagerName) {
        this(new ThreadPoolExecutor(CORE_POOL_SIZE, CORE_POOL_SIZE, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new TaskThreadFactory(taskManagerName),
                new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    @Override
    public Future<?> submit(@NonNull Runnable runnable) {
        if (this.closed) {
            throw new IllegalStateException("TaskManager is closed");
        }
        RunnableDelegateTask delegateTask = new RunnableDelegateTask(publisher, runnable);
        this.liveTaskCounter.incrementAndGet();
        return this.asyncExecutor.submit(delegateTask);
    }

    @Override
    public Future<?> submit(@NonNull Runnable runnable, long timeout, TimeUnit timeUnit) {
        if (this.closed) {
            throw new IllegalStateException("TaskManager is closed");
        }
        throw new UnsupportedOperationException("Feature is not supported");
    }

    @Override
    public <T> Future<T> submit(@NonNull Callable<T> callable) {
        if (this.closed) {
            throw new IllegalStateException("TaskManager is closed");
        }
        CallableDelegateTask<T> delegateTask = new CallableDelegateTask<>(this.publisher, callable);
        this.liveTaskCounter.incrementAndGet();
        return this.asyncExecutor.submit(delegateTask);
    }

    @Override
    public <T> Future<T> submit(@NonNull Callable<T> callable, long timeout, TimeUnit timeUnit) {
        if (this.closed) {
            throw new IllegalStateException("TaskManager is closed");
        }
        throw new UnsupportedOperationException("Feature is not supported");
    }

    @Override
    public void close() {
        this.closed = true;
        if (liveTaskCounter.get() != 0) {
            log.warn("Task manager is about to close, and live tasks are found, taskCount={}", liveTaskCounter.get());
        }
        this.publisher.removeAllListeners();
        this.asyncExecutor.shutdownNow();
        log.info("Task manager is closed");
    }

    protected void onTaskFailed(BaseDelegateTask destTask, Exception e) {
        if (log.isDebugEnabled()) {
            log.warn("Task was failed and removed from the task manager", e);
        }
    }

    protected void onTaskSucceed(BaseDelegateTask destTask) {
        if (log.isDebugEnabled()) {
            log.debug("Task was succeed and removed from the task manager");
        }
    }

    protected long getLiveTaskCount() {
        return liveTaskCounter.get();
    }

    @Override
    public void onEvent(TaskCompleteEvent event) {
        liveTaskCounter.decrementAndGet();
        Object source = event.getSource();
        if (!(source instanceof BaseDelegateTask)) {
            return;
        }
        BaseDelegateTask delegateTask = (BaseDelegateTask) source;
        Exception e = event.getException();
        try {
            if (e == null) {
                onTaskSucceed(delegateTask);
            } else {
                onTaskFailed(delegateTask, e);
            }
        } catch (Exception exception) {
            log.warn("Failed to call method", exception);
        }
    }

}
