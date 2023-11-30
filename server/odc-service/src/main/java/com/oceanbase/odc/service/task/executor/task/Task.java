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

package com.oceanbase.odc.service.task.executor.task;

import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.executor.executor.TaskExecutor;

/**
 * Task interface. Each task should implement this interface
 * 
 * @author gaoda.xy
 * @date 2023/11/22 15:46
 */
public interface Task {

    /**
     * Start current task. This method will be called by {@link TaskExecutor} for fire a task
     */
    void start();

    /**
     * Stop current task. This method will be called by {@link TaskExecutor} for stop a task
     */
    void stop();

    /**
     * Check whether current task is finished
     * 
     * @return true if current task is finished
     */
    boolean isFinished();

    /**
     * Get task progress
     *
     * @return progress
     */
    double progress();

    /**
     * Get job context. Each task implementation should hold a job context
     *
     * @return {@link JobContext}
     */
    JobContext context();

    /**
     * Get task status
     * 
     * @return {@link TaskStatus}
     */
    TaskStatus status();

    /**
     * Get task result
     * 
     * @return {@link FlowTaskResult} (The FlowTaskResult will be changed to TaskResult in the future)
     */
    FlowTaskResult result();

}
