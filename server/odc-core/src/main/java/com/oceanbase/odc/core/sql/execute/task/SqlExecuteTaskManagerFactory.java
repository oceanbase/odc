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

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.task.ExecuteMonitorTaskManager;
import com.oceanbase.odc.core.task.TaskManagerFactory;
import com.oceanbase.odc.core.task.TimeLimitedTaskManagerWrapper;

import lombok.NonNull;

/**
 * Factory class for generating agent task manager
 *
 * @author yh263208
 * @date 2021-11-11 20:49
 * @since ODC_release_3.2.2
 */
public class SqlExecuteTaskManagerFactory implements TaskManagerFactory<SqlExecuteTaskManager> {

    private final int maxConcurrentTaskCount;
    private final String taskManagerName;
    private final long watingTimeoutMillis;
    private final ExecuteMonitorTaskManager monitorTaskManager;

    public SqlExecuteTaskManagerFactory(@NonNull ExecuteMonitorTaskManager monitorTaskManager,
            int maxConcurrentTaskCount) {
        this(monitorTaskManager, null, maxConcurrentTaskCount, 10, TimeUnit.SECONDS);
    }

    public SqlExecuteTaskManagerFactory(@NonNull ExecuteMonitorTaskManager monitorTaskManager,
            String taskManagerName, int maxConcurrentTaskCount) {
        this(monitorTaskManager, taskManagerName, maxConcurrentTaskCount, 10, TimeUnit.SECONDS);
    }

    public SqlExecuteTaskManagerFactory(@NonNull ExecuteMonitorTaskManager monitorTaskManager,
            String taskManagerName, int maxConcurrentTaskCount, long timeout, TimeUnit timeUnit) {
        this.monitorTaskManager = monitorTaskManager;
        this.maxConcurrentTaskCount = maxConcurrentTaskCount;
        this.taskManagerName = taskManagerName;
        Validate.isTrue(timeout > 0, "Timeout Can not be negative");
        this.watingTimeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
    }

    @Override
    public SqlExecuteTaskManager generateManager() {
        DefaultSqlExecuteTaskManager taskManager = new DefaultSqlExecuteTaskManager(
                maxConcurrentTaskCount, taskManagerName, this.watingTimeoutMillis, TimeUnit.MILLISECONDS);
        return new TimeLimitedTaskManagerWrapper(taskManager, this.monitorTaskManager);
    }

}
