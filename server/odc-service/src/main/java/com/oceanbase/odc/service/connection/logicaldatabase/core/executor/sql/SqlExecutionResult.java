/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql;

import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionStatus;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/8/27 17:03
 * @Description: []
 */
@Data
public class SqlExecutionResult implements ExecutionResult {
    private String id;
    private ExecutionStatus status;
    private SqlExecuteResult result;

    @Override
    public boolean isCompleted() {
        return this.status == ExecutionStatus.SUCCESS || this.status == ExecutionStatus.SKIPPED;
    }
}
