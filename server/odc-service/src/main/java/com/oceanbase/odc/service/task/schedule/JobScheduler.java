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

package com.oceanbase.odc.service.task.schedule;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.service.task.exception.JobException;

/**
 * @author yaobin
 * @date 2023-11-23
 * @since 4.2.4
 */
public interface JobScheduler {


    /**
     * schedule a job right now
     *
     * @param jd define a job
     * @return job id
     */
    Long scheduleJobNow(JobDefinition jd);

    /**
     * cancel job
     *
     * @param jobId job id
     * @throws JobException throw JobException if cancel job failed
     */
    void cancelJob(Long jobId) throws JobException;

    /**
     * modify job parameters, new parameters will be delivery to executor if job is running
     *
     * @param jobId job id
     * @param jobParameters job parameters
     * @throws JobException throw JobException if job is terminated
     */
    void modifyJobParameters(Long jobId, Consumer<Map<String, String>> jobParameters) throws JobException;

    /**
     * await job to be completed
     *
     * @param jobId job id
     * @param timeout await timeout
     * @param timeUnit await time uit
     * @throws InterruptedException throws InterruptedException if await be interrupted
     */
    void await(Long jobId, Integer timeout, TimeUnit timeUnit) throws InterruptedException;

    /**
     * get job execution event publisher
     *
     * @return job execution event publisher
     */
    EventPublisher getEventPublisher();
}
