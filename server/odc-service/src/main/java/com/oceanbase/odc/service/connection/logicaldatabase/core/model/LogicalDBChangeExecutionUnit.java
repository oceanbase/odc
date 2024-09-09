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
package com.oceanbase.odc.service.connection.logicaldatabase.core.model;

import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionStatus;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 19:46
 * @Description: []
 */
@Data
public class LogicalDBChangeExecutionUnit {
    private Long id;
    private Long executionOrder;
    private Long scheduleTaskId;
    private String executionId;
    private String sql;
    private Long logicalDatabaseId;
    private Long physicalDatabaseId;
    private SqlExecutionResultWrapper executionResult;
    private ExecutionStatus status;
}
