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

import org.springframework.util.CollectionUtils;

/**
 * @Author: Lebie
 * @Date: 2024/8/26 16:38
 * @Description: []
 */
public class OBExecutionGroup extends AbstractSqlExecutionGroup {
    private final List<List<SqlExecutionUnit>> subgroups;

    public OBExecutionGroup(String id, List<SqlExecutionUnit> executionUnits) {
        super(id, executionUnits);
        this.subgroups = this.executionUnits.stream()
                .collect(Collectors
                        .groupingBy(unit -> unit.getSqlUnit().getDataNode().getDataSourceConfig().getClusterName()))
                .values().stream().collect(Collectors.toList());
    }

    @Override
    public List<List<SqlExecutionUnit>> listSubGroups() {
        return this.subgroups;
    }

    @Override
    public int getSubGroupConcurrency() {
        return 0;
    }
}
