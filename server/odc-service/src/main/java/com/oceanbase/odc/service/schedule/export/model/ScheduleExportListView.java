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
package com.oceanbase.odc.service.schedule.export.model;

import java.util.Date;

import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.Data;

@Data
public class ScheduleExportListView {
    private Long id;
    private ScheduleType scheduleType;
    private Long databaseId;
    private Database database;
    @Internationalizable
    private String description;
    private Long creatorId;
    private InnerUser creator;
    private ScheduleStatus scheduleStatus;
    private Date createTime;
}
