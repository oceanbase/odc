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

import com.oceanbase.odc.service.task.caller.JobException;

/**
 * @author yaobin
 * @date 2023-11-23
 * @since 4.2.4
 */
public interface JobScheduler {

    /**
     * schedule job which can be set trigger config
     *
     * @param jd define a job
     * @throws JobException throw JobException if schedule job failed
     */
    void scheduleJob(JobDefinition jd) throws JobException;

    /**
     * schedule a job right now, trigger config on job definition will be ignored
     *
     * @param jd define a job
     * @throws JobException throw JobException if schedule job failed
     */
    void scheduleJobNow(JobDefinition jd) throws JobException;

    /**
     * cancel job
     *
     * @param ji define a job
     * @throws JobException throw JobException if cancel job failed
     */
    void cancelJob(JobIdentity ji) throws JobException;
}
