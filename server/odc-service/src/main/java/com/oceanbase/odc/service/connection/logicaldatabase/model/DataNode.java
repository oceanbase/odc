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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.common.util.HashUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

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

    @JsonIgnore
    private ConnectionConfig dataSourceConfig;

    private String schemaName;

    private String tableName;

    public DataNode(String schemaName, String tableName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public String getFullName() {
        return schemaName + DELIMITER + tableName;
    }


    @JsonIgnore
    public String getStructureSignature(DBTable table) {
        if (Objects.isNull(table)) {
            return "[ODC] NULL OBJECT";
        }
        String columnSignature = getColumnsSignature(table.getColumns());
        String indexSignature = getIndexesSignature(table.getIndexes());
        String constraintSignature = getConstraintsSignature(table.getConstraints());
        String tableOptionSignature = getTableOptionSignature(table.getTableOptions());
        return HashUtils
                .sha1(String.join("|||", columnSignature, indexSignature, constraintSignature, tableOptionSignature));
    }

    private String getTableOptionSignature(DBTableOptions tableOptions) {
        if (tableOptions == null) {
            return "[ODC] NULL OBJECT";
        }
        return String.join("|", nullSafeGet(tableOptions.getCharsetName()),
                nullSafeGet(tableOptions.getCollationName()));
    }

    private String getColumnsSignature(List<DBTableColumn> columns) {
        if (CollectionUtils.isEmpty(columns)) {
            return "[ODC] NULL LIST";
        }
        return columns.stream().sorted(Comparator.comparing(DBTableColumn::getName))
                .map(column -> String.join("|", nullSafeGet(column.getName()), nullSafeGet(column.getTypeName()),
                        nullSafeGet(column.getFullTypeName()), nullSafeGet(column.getTypeModifiers()),
                        nullSafeGet(column.getCharsetName()), nullSafeGet(column.getCollationName()),
                        nullSafeGet(column.getAutoIncrement()), nullSafeGet(column.getCharUsed()),
                        nullSafeGet(column.getDefaultValue()), nullSafeGet(column.getDayPrecision()),
                        nullSafeGet(column.getPrecision()), nullSafeGet(column.getYearPrecision()),
                        nullSafeGet(column.getScale()),
                        nullSafeGet(column.getNullable()), nullSafeGet(column.getEnumValues()),
                        nullSafeGet(column.getGenExpression()), nullSafeGet(column.getHidden()),
                        nullSafeGet(column.getMaxLength()), nullSafeGet(column.getOnUpdateCurrentTimestamp()),
                        nullSafeGet(column.getVirtual()), nullSafeGet(column.getStored()),
                        nullSafeGet(column.getSecondPrecision()), nullSafeGet(column.getZerofill()),
                        nullSafeGet(column.getExtraInfo()), nullSafeGet(column.getUnsigned())))
                .collect(Collectors.joining("||"));
    }

    private String getIndexesSignature(List<DBTableIndex> indexes) {
        if (CollectionUtils.isEmpty(indexes)) {
            return "[ODC] NULL LIST";
        }
        Collections.sort(indexes, (idx1, idx2) -> {
            String key1 = String.join("", idx1.getColumnNames()) + idx1.getType();
            String key2 = String.join("", idx2.getColumnNames()) + idx2.getType();
            return key1.compareTo(key2);
        });
        return indexes.stream().map(index -> String.join("|",
                nullSafeGet(index.getType()), nullSafeGet(index.getUnique()), nullSafeGet(index.getPrimary()),
                nullSafeGet(index.getColumnNames()),
                nullSafeGet(index.getVisible()), nullSafeGet(index.getAlgorithm()),
                nullSafeGet(index.getAdditionalInfo()),
                nullSafeGet(index.getGlobal()), nullSafeGet(index.getAvailable()), nullSafeGet(index.getCollation()),
                nullSafeGet(index.getCardinality()), nullSafeGet(index.getCompressInfo()),
                nullSafeGet(index.getParserName())))
                .collect(Collectors.joining("||"));
    }

    private String getConstraintsSignature(List<DBTableConstraint> constraints) {
        if (CollectionUtils.isEmpty(constraints)) {
            return "[ODC] NULL LIST";
        }
        Collections.sort(constraints, (con1, con2) -> {
            String key1 = String.join("", con1.getColumnNames()) + con1.getType();
            String key2 = String.join("", con2.getColumnNames()) + con2.getType();
            return key1.compareTo(key2);
        });
        return constraints.stream().map(constraint -> String.join("|",
                nullSafeGet(constraint.getType()), nullSafeGet(constraint.getColumnNames()),
                nullSafeGet(constraint.getEnabled()), nullSafeGet(constraint.getCheckClause()),
                nullSafeGet(constraint.getValidate()), nullSafeGet(constraint.getDeferability()),
                nullSafeGet(constraint.getReferenceColumnNames()), nullSafeGet(constraint.getOnDeleteRule()),
                nullSafeGet(constraint.getOnUpdateRule()), nullSafeGet(constraint.getMatchType())))
                .collect(Collectors.joining("||"));
    }

    private String nullSafeGet(Object obj) {
        if (obj instanceof Collection) {
            if (CollectionUtils.isEmpty((Collection) obj)) {
                return "[ODC] NULL LIST";
            }
            return (String) ((Collection) obj).stream().map(this::nullSafeGet).collect(Collectors.joining(","));
        }
        return obj == null ? "[ODC] NULL OBJECT" : obj.toString();
    }
}
