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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskParameters;

import lombok.Data;

@Data
public class SqlPlanParameters implements ScheduleTaskParameters {

    private Long databaseId;

    @JsonProperty(access = Access.READ_ONLY)
    private Database databaseInfo;

    private String sqlContent;

    private List<String> sqlObjectIds;

    private String delimiter = ";";

    /**
     * SQL execution timeout
     */
    private Long timeoutMillis = 172800000L;

    /**
     * limit the number of query results
     */
    private Integer queryLimit = 1000;

    /**
     * number of retry attempts when SQL execution fails
     */
    private Integer retryTimes = 0;

    /**
     * SQL execution error handling strategy
     */
    private TaskErrorStrategy errorStrategy;

}
