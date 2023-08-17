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
package com.oceanbase.odc.service.datasecurity.extractor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author gaoda.xy
 * @date 2023/6/12 15:23
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class DBColumn {
    private String databaseName;
    private String tableName;
    private String columnName;

    public static DBColumn from(LogicalColumn logicalColumn) {
        DBColumn column = new DBColumn();
        column.setDatabaseName(logicalColumn.getDatabaseName());
        column.setTableName(logicalColumn.getTableName());
        column.setColumnName(logicalColumn.getName());
        return column;
    }
}
