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
import java.util.Set;

import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.common.model.InnerUser;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/8/14 18:44
 * @Descripition:
 */

@Data
public class ScheduleOverviewHist {

    private Long id;

    private ScheduleType type;

    private ScheduleStatus status;

    private InnerUser creator;

    private Set<InnerUser> candidateApprovers;

    private Date createTime;

    private Long approveInstanceId;

    private boolean approvable;

    @Internationalizable
    private String description;

    private Project project;

}
