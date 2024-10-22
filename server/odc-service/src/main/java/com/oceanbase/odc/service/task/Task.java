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
package com.oceanbase.odc.service.task;

import java.util.Map;

import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.enums.JobStatus;

/**
 * Task interface. Each task should implement this interface
 * 
 * @author gaoda.xy
 * @date 2023/11/22 15:46
 */
public interface Task<RESULT> {

    /**
     * Start current task. This method will be called by TaskExecutor for fire a task
     */
    void start(JobContext context);

    /**
     * Stop current task. This method will be called TaskExecutor for stop a task
     */
    boolean stop();

    /**
     * Modify current task parameters
     */
    boolean modify(Map<String, String> jobParameters);

    /**
     * Get task progress, the progress should be between 0 and 1 (include 0 and 1)
     *
     * @return progress
     */
    double getProgress();

    /**
     * Get job context. Each task implementation should hold a job context
     *
     * @return {@link JobContext}
     */
    JobContext getJobContext();

    /**
     * Get task status
     * 
     * @return {@link JobStatus}
     */
    JobStatus getStatus();

    /**
     * Get task result
     */
    RESULT getTaskResult();

}
