/*
 * Copyright (c) 2024 OceanBase.
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

import java.util.Date;

import com.oceanbase.odc.service.quartz.model.MisfireStrategy;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/6/8 17:40
 * @Descripition:
 */

@Data
public class Schedule {

    private Long id;

    private Long connectionId;

    private Long databaseId;

    private String databaseName;

    private Long organizationId;

    private Long projectId;

    private ScheduleStatus status;

    private Boolean allowConcurrent = false;

    private MisfireStrategy misfireStrategy = MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING;

    private ScheduleType scheduleType;

    private String parameters;

    private String triggerConfigJson;

    private Long creatorId;

    private String description;

    private Date createTime;

    private Date updateTime;

}
