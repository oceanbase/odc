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
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.core.sql.execute.task.SqlExecuteCallable;
import com.oceanbase.odc.core.sql.execute.task.SqlExecuteTaskManager;

import lombok.NonNull;

/**
 * Agent task manager, used for various operations of the agent task manager
 *
 * @author yh263208
 * @date 2021-11-11 20:39
 * @since ODC_release_3.2.2
 */
public class TimeLimitedTaskManagerWrapper implements SqlExecuteTaskManager {

    private final SqlExecuteTaskManager taskManager;
    private final ExecuteMonitorTaskManager monitorTaskManager;

    public TimeLimitedTaskManagerWrapper(@NonNull SqlExecuteTaskManager taskManager,
            @NonNull ExecuteMonitorTaskManager monitorTaskManager) {
        this.taskManager = taskManager;
        this.monitorTaskManager = monitorTaskManager;
    }

    @Override
    public Future<?> submit(@NonNull Runnable runnable) {
        return this.taskManager.submit(runnable);
    }

    @Override
    public Future<?> submit(@NonNull Runnable runnable, long timeout, TimeUnit timeUnit) {
        return this.monitorTaskManager.submit(runnable, timeout, timeUnit, taskManager);
    }

    @Override
    public <T> Future<T> submit(@NonNull Callable<T> callable) {
        return this.taskManager.submit(callable);
    }

    @Override
    public <T> Future<T> submit(@NonNull Callable<T> callable, long timeout, TimeUnit timeUnit) {
        return this.monitorTaskManager.submit(callable, timeout, timeUnit, taskManager);
    }

    @Override
    public <T> Future<T> submit(@NonNull SqlExecuteCallable<T> callable) {
        return this.taskManager.submit(callable);
    }

    @Override
    public <T> Future<T> submit(@NonNull SqlExecuteCallable<T> callable, long timeout, TimeUnit timeUnit) {
        return this.monitorTaskManager.submit(callable, timeout, timeUnit, taskManager);
    }

    @Override
    public void close() throws Exception {
        this.taskManager.close();
    }

}

