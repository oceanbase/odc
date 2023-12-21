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
package com.oceanbase.odc.service.schedule.model;

import org.quartz.JobKey;
import org.quartz.TriggerKey;

import com.oceanbase.odc.service.task.schedule.JobIdentity;

/**
 * @author yaobin
 * @date 2023-06-13
 * @since 4.2.0
 * @Descripition: Defines the key generation rules to ensure global uniqueness in this class.
 */
public class QuartzKeyGenerator {

    public static TriggerKey generateTriggerKey(Long scheduleId, JobType jobType) {
        return new TriggerKey(scheduleId + "", jobType.name());
    }

    public static JobKey generateJobKey(Long scheduleId, JobType jobType) {
        return new JobKey(scheduleId + "", jobType.name());
    }

    public static TriggerKey generateTriggerKey(Long jobId) {
        return new TriggerKey(jobId+"");
    }

    public static JobKey generateJobKey(Long jobId) {
        return new JobKey(jobId+"");
    }

}
