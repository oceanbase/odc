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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroup;

import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 16:44
 * @Description: []
 */
public abstract class AbstractSqlExecutionGroup implements ExecutionGroup<SqlExecutionUnit> {
    @Getter
    protected final String id;
    @Getter
    protected final List<SqlExecutionUnit>        executionUnits;
    @Getter
    protected List<List<SqlExecutionUnit>>  subgroups;
    private final Map<String, SqlExecutionUnit> id2ExecutionUnit;

    public AbstractSqlExecutionGroup(@NotNull String id, @NotEmpty List<SqlExecutionUnit> executionUnits) {
        this.id = id;
        this.executionUnits = executionUnits;
        this.id2ExecutionUnit = executionUnits.stream().collect(Collectors.toMap(SqlExecutionUnit::getId, unit -> unit));
    }

    @Override
    public List<SqlExecutionUnit> listExecutionUnits() {
        return this.executionUnits;
    }

    @Override
    public SqlExecutionUnit getExecutionUnit(@NotNull String id) {
        if (!id2ExecutionUnit.containsKey(id)) {
            throw new UnexpectedException("Execution unit not found, id=" + id);
        }
        return id2ExecutionUnit.get(id);
    }


    @Override
    public int getConcurrency() {
        return Math.max(100, listSubGroups().size());
    }
}
