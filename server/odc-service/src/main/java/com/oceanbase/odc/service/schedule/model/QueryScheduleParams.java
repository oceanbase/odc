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
import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2022/11/22 15:04
 * @Descripition:
 */

@Data
@Builder
public class QueryScheduleParams {

    private Set<Long> dataSourceIds;
    private String dataSourceName;
    private String id;
    private String name;
    private List<ScheduleStatus> statuses;
    private ScheduleType type;
    private Date startTime;
    private Date endTime;
    private String creator;
    private Set<Long> creatorIds;
    private Long projectId;
    private String projectUniqueIdentifier;
    private Set<Long> projectIds;
    private Long organizationId;
    private String databaseName;
    private String tenantId;
    private String clusterId;
    private String triggerStrategy;

}
