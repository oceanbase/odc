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
package com.oceanbase.odc.core.sql.execute.task;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.task.BaseDelegateTask;
import com.oceanbase.odc.core.task.DefaultTaskManager;
import com.oceanbase.odc.core.task.TaskThreadFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * SQL asynchronous execution task manager, this manager is used to manage SQL asynchronous
 * execution tasks, including task creation, execution, deletion and retry
 *
 * @author yh263208
 * @date 2021-11-11 11:37
 * @since ODC_release_3.2.2
 */
@Slf4j
public class DefaultSqlExecuteTaskManager extends DefaultTaskManager implements SqlExecuteTaskManager {

    private final int maxConcurrentTaskCount;
    private final Semaphore taskCountSemaphore;
    private final long waitingTimeoutForSubmitTask;
    private final List<Future<?>> activeFuture;

    public DefaultSqlExecuteTaskManager(int maxConcurrentTaskCount) {
        this(maxConcurrentTaskCount, "default", 10, TimeUnit.SECONDS);
    }

    public DefaultSqlExecuteTaskManager(int maxConcurrentTaskCount, String taskManagerName) {
        this(maxConcurrentTaskCount, taskManagerName, 10, TimeUnit.SECONDS);
    }

    public DefaultSqlExecuteTaskManager(int maxConcurrentTaskCount, String taskManagerName,
            long timeout, @NonNull TimeUnit timeUnit) {
        super(new ThreadPoolExecutor(
                maxConcurrentTaskCount, maxConcurrentTaskCount,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new TaskThreadFactory("sql-async-execute-" + (taskManagerName == null ? "default" : taskManagerName)),
                new ThreadPoolExecutor.AbortPolicy()));
        Validate.isTrue(maxConcurrentTaskCount > 0, "Concurrent count can not be negative");
        this.maxConcurrentTaskCount = maxConcurrentTaskCount;
        this.taskCountSemaphore = new Semaphore(maxConcurrentTaskCount);
        Validate.isTrue(timeout > 0, "Timeout can not be negative");
        this.waitingTimeoutForSubmitTask = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        this.activeFuture = new LinkedList<>();
    }

    @Override
    public Future<?> submit(@NonNull Runnable runnable) {
        return doAction(() -> DefaultSqlExecuteTaskManager.super.submit(runnable));
    }

    @Override
    public Future<?> submit(@NonNull Runnable runnable, long timeout, TimeUnit timeUnit) {
        return doAction(() -> DefaultSqlExecuteTaskManager.super.submit(runnable, timeout, timeUnit));
    }

    @Override
    public <T> Future<T> submit(@NonNull Callable<T> callable) {
        return doAction(() -> DefaultSqlExecuteTaskManager.super.submit(callable));
    }

    @Override
    public <T> Future<T> submit(@NonNull Callable<T> callable, long timeout, TimeUnit timeUnit) {
        return doAction(() -> DefaultSqlExecuteTaskManager.super.submit(callable, timeout, timeUnit));
    }

    @Override
    public <T> Future<T> submit(@NonNull SqlExecuteCallable<T> callable) {
        Future<T> taskFuture = doAction(() -> DefaultSqlExecuteTaskManager.super.submit(callable));
        return wrapFuture(taskFuture, callable);
    }

    @Override
    public <T> Future<T> submit(@NonNull SqlExecuteCallable<T> callable, long timeout, TimeUnit timeUnit) {
        Future<T> taskFuture = doAction(() -> DefaultSqlExecuteTaskManager.super.submit(callable, timeout, timeUnit));
        return wrapFuture(taskFuture, callable);
    }

    @Override
    public void close() {
        super.close();
        for (Future<?> future : activeFuture) {
            if (future.isDone() || future.isCancelled()) {
                continue;
            }
            try {
                boolean res = future.cancel(true);
                log.info("Sql execution is cancelled, result={}", res);
            } catch (Exception e) {
                log.warn("Failed to cancel sql executing", e);
            }
        }
    }

    @Override
    protected void onTaskFailed(BaseDelegateTask destTask, Exception e) {
        super.onTaskFailed(destTask, e);
        this.taskCountSemaphore.release();
        this.activeFuture.removeIf(future -> future.isDone() || future.isCancelled());
    }

    @Override
    protected void onTaskSucceed(BaseDelegateTask destTask) {
        super.onTaskSucceed(destTask);
        this.taskCountSemaphore.release();
        this.activeFuture.removeIf(future -> future.isDone() || future.isCancelled());
    }

    private <T> Future<T> doAction(Supplier<Future<T>> supplier) {
        boolean accuireResult = false;
        try {
            accuireResult = this.taskCountSemaphore.tryAcquire(this.waitingTimeoutForSubmitTask, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            log.warn("Task submission semaphore acquisition waiting to be interrupted", exception);
        }
        String errMsg = "Too many tasks submitted, max=" + this.maxConcurrentTaskCount;
        if (!accuireResult) {
            throw new BadRequestException(ErrorCodes.ConnectionOccupied, new Object[] {}, errMsg);
        }
        if (getLiveTaskCount() >= maxConcurrentTaskCount) {
            this.taskCountSemaphore.release();
            throw new BadRequestException(ErrorCodes.ConnectionOccupied, new Object[] {}, errMsg);
        }
        try {
            return supplier.get();
        } catch (Exception exception) {
            this.taskCountSemaphore.release();
            throw new IllegalStateException(exception);
        }
    }

    private <T> Future<T> wrapFuture(Future<T> target, SqlExecuteCallable<T> callable) {
        Future<T> future = new SqlExecuteContext<>(target, callable);
        activeFuture.removeIf(f -> f.isDone() || f.isCancelled());
        activeFuture.add(future);
        return future;
    }

}

