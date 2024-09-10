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
package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionSubGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionSubGroupUnit;

/**
 * @Author: Lebie
 * @Date: 2024/9/2 18:55
 * @Description: []
 */
public final class OBExecutionGroup extends ExecutionGroup<SqlExecuteReq, SqlExecutionResultWrapper> {
    public OBExecutionGroup(List<ExecutionSubGroupUnit<SqlExecuteReq, SqlExecutionResultWrapper>> executionUnits) {
        super(executionUnits);
    }

    @Override
    protected List<ExecutionSubGroup<SqlExecuteReq, SqlExecutionResultWrapper>> listSubGroups(
            List<ExecutionSubGroupUnit<SqlExecuteReq, SqlExecutionResultWrapper>> executionUnits) {
        if (executionUnits.isEmpty()) {
            return Collections.emptyList();
        }
        return executionUnits.stream()
                .collect(Collectors.groupingBy(unit -> unit.getInput().getConnectionConfig().getTenantName()))
                .values().stream().map(ExecutionSubGroup::new).collect(Collectors.toList());
    }
}
