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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.core.task.TaskManager;

import lombok.NonNull;

/**
 * {@link SqlExecuteTaskManager}
 *
 * @author yh263208
 * @date 2022-10-13 10:39
 * @since ODC_release_3.5.0
 * @see TaskManager
 */
public interface SqlExecuteTaskManager extends TaskManager {
    /**
     * Submit a {@link Callable} task
     *
     * @param callable {@link Callable} task
     * @return task id
     */
    <T> Future<T> submit(@NonNull SqlExecuteCallable<T> callable);

    /**
     * Submit a task {@link Callable} with timeout settings
     *
     * @param callable submitted {@link Callable}
     * @param timeout timeout for this task
     * @param timeUnit time unit for timeout settings
     * @return task Id
     */
    <T> Future<T> submit(@NonNull SqlExecuteCallable<T> callable, long timeout, TimeUnit timeUnit);
}

