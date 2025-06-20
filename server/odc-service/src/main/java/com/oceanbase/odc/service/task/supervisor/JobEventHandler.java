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
package com.oceanbase.odc.service.task.supervisor;

import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.listener.JobCallerEvent;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;

/**
 * @author longpeng.zlp
 * @date 2024/11/28 14:53
 */
public interface JobEventHandler {
    /**
     * call process should be done before job started
     * 
     * @param jobContext
     * @return
     */
    void beforeStartJob(JobContext jobContext) throws JobException;

    /**
     * calls process should be done after start job
     * 
     * @param executorIdentifier
     * @param jobContext
     */
    void afterStartJob(ExecutorEndpoint executorIdentifier, JobContext jobContext) throws JobException;

    /**
     * finish done
     * 
     * @param executorIdentifier
     * @param jobContext
     */
    void afterFinished(ExecutorEndpoint executorIdentifier, JobContext jobContext) throws JobException;

    /**
     * target machine may offline
     * 
     * @param jobContext
     */
    void finishFailed(ExecutorEndpoint executorIdentifier, JobContext jobContext);


    /**
     * listent new event
     * 
     * @param jobCallerEvent
     */
    void onNewEvent(JobCallerEvent jobCallerEvent);
}
