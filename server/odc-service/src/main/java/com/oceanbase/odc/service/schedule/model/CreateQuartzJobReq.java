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

import org.quartz.JobDataMap;

import com.oceanbase.odc.service.quartz.model.MisfireStrategy;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2023/6/27 15:34
 * @Descripition:
 */

@Data
public class CreateQuartzJobReq {

    // It represents the owner of the job.
    private Long scheduleId;

    private JobType type;

    private Boolean allowConcurrent = false;

    private MisfireStrategy misfireStrategy = MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING;

    private TriggerConfig triggerConfig = null;

    // To store task parameters.
    private JobDataMap jobDataMap = new JobDataMap();

}
