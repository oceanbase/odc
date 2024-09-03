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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.mariadb.jdbc.HostAddress;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionSubGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionUnit;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

/**
 * @Author: Lebie
 * @Date: 2024/9/2 18:27
 * @Description: []
 */
public final class MySQLExecutionGroup extends ExecutionGroup<SqlExecuteReq, SqlExecutionResultWrapper> {

    public MySQLExecutionGroup(List<ExecutionUnit<SqlExecuteReq, SqlExecutionResultWrapper>> executionUnits) {
        super(executionUnits);
    }

    @Override
    protected List<ExecutionSubGroup<SqlExecuteReq, SqlExecutionResultWrapper>> listSubGroups(
            List<ExecutionUnit<SqlExecuteReq, SqlExecutionResultWrapper>> executionUnits) {
        if (CollectionUtils.isEmpty(executionUnits)) {
            return Collections.emptyList();
        }
        return executionUnits.stream()
                .collect(Collectors.groupingBy(unit -> new HostAddress(unit.getInput().getConnectionConfig().getHost(),
                        unit.getInput().getConnectionConfig().getPort())))
                .values().stream().map(ExecutionSubGroup::new).collect(Collectors.toList());
    }

}
