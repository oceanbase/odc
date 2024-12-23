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

import org.quartz.Job;
import org.quartz.JobDataMap;

import com.oceanbase.odc.service.quartz.executor.QuartzJob;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/6/9 22:19
 * @Descripition:
 */

@Data
public class ChangeQuartJobParam {

    private String jobName;

    private String jobGroup;

    private Boolean allowConcurrent = false;

    private MisfireStrategy misfireStrategy = MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING;

    private TriggerConfig triggerConfig = null;

    // To store task parameters.
    private JobDataMap jobDataMap = new JobDataMap();

    private OperationType operationType;
    private Class<? extends Job> jobClazz = QuartzJob.class;
}
