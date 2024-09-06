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

package com.oceanbase.odc.service.task.caller;

import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * operate odc job for different deployment environment, eg: k8s, master-worker or same jvm process
 * 
 * @author yaobin
 * @date 2023-11-15
 * @since 4.2.4
 */
public interface JobCaller {

    /**
     * start a odc job
     * 
     * @param context job context
     * @throws JobException throws JobException when start job failed
     */
    void start(JobContext context) throws JobException;

    /**
     * stop a odc job
     * 
     * @param ji job identity
     * @throws JobException throws JobException when stop job failed
     */
    void stop(JobIdentity ji) throws JobException;

    /**
     * modify job parameters
     *
     * @param ji job identity
     * @throws JobException throws JobException when stop job failed
     */
    void modify(JobIdentity ji, String jobParametersJson) throws JobException;

    /**
     * complete the job, process should be quit resource can be released as well
     * 
     * @param ji
     * @throws JobException
     */
    void finish(JobIdentity ji) throws JobException;

    /**
     * if job can be finished
     * 
     * @param ji
     * @return false is job/resource in unknown state
     */
    boolean canBeFinish(JobIdentity ji);
}
