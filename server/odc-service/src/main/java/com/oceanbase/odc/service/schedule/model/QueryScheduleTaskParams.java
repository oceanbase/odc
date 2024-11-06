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

import com.oceanbase.odc.core.shared.constant.TaskStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryScheduleTaskParams {
    private String id;
    // it will be merged into scheduleIds when it is not null
    private String scheduleId;
    private String scheduleName;
    private Set<Long> dataSourceIds;
    private List<TaskStatus> statuses;
    private ScheduleType scheduleType;
    private Date startTime;
    private Date endTime;
    private String creator;
    private Set<Long> creatorIds;
    private Long projectId;
    private Set<Long> projectIds;
    private Long organizationId;
    private String databaseName;
    private String tenantId;
    private String clusterId;

    // inner use
    private Set<Long> scheduleIds;
}
