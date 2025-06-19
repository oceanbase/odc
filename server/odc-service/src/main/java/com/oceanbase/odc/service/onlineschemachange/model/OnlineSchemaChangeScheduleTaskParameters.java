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
package com.oceanbase.odc.service.onlineschemachange.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.onlineschemachange.ddl.ReplaceResult;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-09
 * @since 4.2.0
 */
@Data
public class OnlineSchemaChangeScheduleTaskParameters {

    @NotNull
    private Long connectionId;

    /**
     * Main Account ID of Public Cloud
     */
    private String uid;

    @NotBlank
    private String databaseName;

    /**
     * Raw origin table name without schema name
     */
    @NotBlank
    private String originTableName;

    /**
     * Raw new table name without schema name
     */
    @NotBlank
    private String newTableName;


    private String originTableNameUnwrapped;

    private String newTableNameUnwrapped;


    /**
     * Raw renamed table name without schema name
     */
    @NotBlank
    private String renamedTableName;
    private String renamedTableNameUnwrapped;

    @NotBlank
    private String originTableCreateDdl;

    private String newTableCreateDdl;

    private String newTableCreateDdlForDisplay;

    private LinkType linkType = LinkType.OMS;

    private String omsProjectId;

    private String omsDataSourceId;

    private String workerInstanceId;

    private DialectType dialectType;

    /**
     * For ODC internal usage
     */
    private List<String> sqlsToBeExecuted = new ArrayList<>();

    private ReplaceResult replaceResult;

    private RateLimiterConfig rateLimitConfig = new RateLimiterConfig();

    /**
     * state change for state machine
     */
    private String state;

    private String extraInfo;

    // column should be replicated when replicate data from origin table to ghost table
    // only set when column is dropped
    private List<String> filterColumns;

    public String getOriginTableNameWithSchema() {
        return tableNameWithSchema(originTableName);
    }

    public String getNewTableNameWithSchema() {
        return tableNameWithSchema(newTableName);
    }

    public String getRenamedTableNameWithSchema() {
        return tableNameWithSchema(renamedTableName);
    }

    private String tableNameWithSchema(String tableName) {
        return StringUtils.isBlank(databaseName) ? tableName : (databaseName + "." + tableName);
    }
}
