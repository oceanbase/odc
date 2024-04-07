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

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2024/3/22 15:22
 * @Description: []
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataNode {
    private static final String DELIMITER = ".";

    private ConnectionConfig dataSourceConfig;

    private String schemaName;

    private String tableName;

    private DBTable table;

    public String getFullName() {
        return schemaName + DELIMITER + tableName;
    }

    public String getStructureSignature() {
        List<DBTableColumn> columns = table.getColumns();
        List<DBTableIndex> indexes = table.getIndexes();
        List<DBTableConstraint> constraints = table.getConstraints();
        DBTablePartition partition = table.getPartition();
    }

    private String getColumnsSignature(List<DBTableColumn> columns) {
        columns.stream().sorted(Comparator.comparing(DBTableColumn::getName)).map(column -> {
            return String.join("|", column.getName(), column.getTypeName(), column.get)
        }).collect(Collectors.joining(","));
    }
}
