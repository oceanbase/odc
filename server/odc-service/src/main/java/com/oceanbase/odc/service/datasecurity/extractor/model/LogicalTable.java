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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/6/5 19:20
 */
@Data
public class LogicalTable {
    private String name;
    private String alias;
    private List<LogicalColumn> columnList;

    /**
     * Only used when there is more than one from tables
     */
    private List<LogicalTable> tableList;

    public List<Set<DBColumn>> getTableRelatedDBColumns() {
        List<Set<DBColumn>> tableDbColumns = new ArrayList<>();
        if (!columnList.isEmpty()) {
            for (LogicalColumn column : columnList) {
                tableDbColumns.add(getColumnRelatedDBColumns(column));
            }
        }
        return tableDbColumns;
    }

    private Set<DBColumn> getColumnRelatedDBColumns(LogicalColumn column) {
        Set<DBColumn> dbColumns = new HashSet<>();
        if (!column.getType().isTemporary()) {
            dbColumns.add(DBColumn.from(column));
        } else if (CollectionUtils.isNotEmpty(column.getFromList())) {
            for (LogicalColumn subColumn : column.getFromList()) {
                dbColumns.addAll(getColumnRelatedDBColumns(subColumn));
            }
        }
        return dbColumns;
    }

    public static LogicalTable empty() {
        LogicalTable logicalTable = new LogicalTable();
        logicalTable.setTableList(new ArrayList<>());
        logicalTable.setColumnList(new ArrayList<>());
        return logicalTable;
    }
}
