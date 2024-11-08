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

/**
 * Task interface. Each task should implement this interface
 * 
 * @author gaoda.xy
 * @date 2023/11/22 15:46
 */
public interface Task<RESULT> {

    /**
     * init task in runtime
     * 
     * @param taskContext
     * @throws Exception
     */
    void init(TaskContext taskContext) throws Exception;

    /**
     * Start current task. This method will be called by TaskExecutor for fire a task
     */
    boolean start() throws Exception;

    /**
     * Stop current task. This method will be called TaskExecutor for stop a task
     */
    void stop() throws Exception;

    /**
     * close and clean resource of task
     */
    void close() throws Exception;

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
     * Get task result
     */
    RESULT getTaskResult();

}
