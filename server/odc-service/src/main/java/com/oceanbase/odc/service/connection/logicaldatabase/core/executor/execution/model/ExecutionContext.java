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

package com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionUnit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/8/23 17:57
 * @Description: []
 */
public final class ExecutionContext<T extends ExecutionUnit> {
    private final List<ExecutionGroup<T>>  groups;
    private final Map<String, ExecutionGroup<T>> id2Group;

    public ExecutionContext(List<ExecutionGroup<T>> groups) {
        this.groups = groups;
        this.id2Group = groups.stream().collect(Collectors.toMap(ExecutionGroup::getId, group -> group));
    }

    public List<ExecutionGroup<T>> listGroups() {
        return groups;
    }

    public ExecutionGroup<T> getGroup(@NotNull String id) {
        if (!id2Group.containsKey(id)) {
            throw new UnexpectedException("Execution group not found, id=" + id);
        }
        return id2Group.get(id);
    }

    public ExecutionGroup<T> getCurrentlyExecutingGroup() {
        throw new NotImplementedException();
    }
}
