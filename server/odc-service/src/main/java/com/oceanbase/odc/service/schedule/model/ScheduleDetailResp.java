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

import java.util.Date;
import java.util.List;

import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2022/11/22 14:38
 * @Descripition:
 */

@Data
public class ScheduleDetailResp {

    private Long scheduleId;

    private String scheduleName;

    private ScheduleType type;

    private Long projectId;

    private InnerUser creator;

    private Date createTime;

    private Date updateTime;

    private ScheduleStatus status;

    @Internationalizable
    private String description;

    private Boolean allowConcurrent;

    private MisfireStrategy misfireStrategy;

    private List<Date> nextFireTimes;

    private ScheduleTaskParameters parameters;

    private TriggerConfig triggerConfig;

    private Project project;

}
