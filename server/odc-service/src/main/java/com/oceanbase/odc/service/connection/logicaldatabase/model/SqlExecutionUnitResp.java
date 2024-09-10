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
package com.oceanbase.odc.service.connection.logicaldatabase.model;

import java.util.List;

import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionStatus;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/9/4 12:21
 * @Description: []
 */
@Data
public class SqlExecutionUnitResp {
    private Long id;

    private Database database;

    private ConnectionConfig dataSource;

    private List<SqlExecutionResultWrapper> sqlExecuteResults;

    private Integer totalSqlCount;

    private Integer completedSqlCount;

    private ExecutionStatus status;
}