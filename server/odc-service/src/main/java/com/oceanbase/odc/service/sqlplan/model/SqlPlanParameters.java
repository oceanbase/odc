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
package com.oceanbase.odc.service.sqlplan.model;

import java.util.List;

import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskParameters;

import lombok.Data;

@Data
public class SqlPlanParameters implements ScheduleTaskParameters {

    private Long databaseId;

    private Database databaseInfo;// 包含数据源、数据库信息

    private String sqlContent;

    private List<String> sqlObjectNames;

    private List<String> sqlObjectIds;

    private Long timeoutMillis = 172800000L;

    private String delimiter = ";";

    private Integer queryLimit = 1000;

    private boolean modifyTimeoutIfTimeConsumingSqlExists = true;

    private Integer retryTimes = 0;

    private Long retryIntervalMillis = 180000L;

    private TaskErrorStrategy errorStrategy;

    private Integer riskLevelIndex;
}
