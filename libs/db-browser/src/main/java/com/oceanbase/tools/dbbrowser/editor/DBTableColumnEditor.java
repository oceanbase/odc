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
package com.oceanbase.tools.dbbrowser.editor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2022/7/20 上午12:19
 * @Description: []
 */
public abstract class DBTableColumnEditor implements DBObjectEditor<DBTableColumn> {

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public String generateCreateObjectDDL(@NotNull DBTableColumn column) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(column))
                .append(" ADD ");
        if (appendColumnKeyWord()) {
            sqlBuilder.append("COLUMN ");
        }
        appendColumnDefinition(column, sqlBuilder);
        sqlBuilder.append(";").line();
        generateColumnComment(column, sqlBuilder);
        return sqlBuilder.toString();
    }

    protected abstract boolean appendColumnKeyWord();

    protected void appendColumnDefinition(DBTableColumn column, SqlBuilder sqlBuilder) {
        sqlBuilder.identifier(column.getName());
        getSupportColumnModifiers().forEach(modifier -> modifier.appendModifier(column, sqlBuilder));
    }

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBTableColumn column) {
        SqlBuilder sqlBuilder = sqlBuilder();
        appendColumnDefinition(column, sqlBuilder);
        return sqlBuilder.toString().trim();
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBTableColumn oldColumn,
            @NotNull DBTableColumn newColumn) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(oldColumn))
                .append(" RENAME COLUMN ").identifier(oldColumn.getName()).append(" TO ")
                .identifier(newColumn.getName());
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectDDL(@NotNull DBTableColumn oldColumn,
            @NotNull DBTableColumn newColumn) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (!StringUtils.equals(oldColumn.getName(), newColumn.getName())) {
            sqlBuilder.append(generateRenameObjectDDL(oldColumn, newColumn)).append(";\n");
        }
        if (!Objects.equals(oldColumn, newColumn)) {
            sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(oldColumn))
                    .append(" MODIFY ");
            if (appendColumnKeyWord()) {
                sqlBuilder.append("COLUMN ");
            }
            sqlBuilder.append(generateCreateDefinitionForUpdateDDL(oldColumn, newColumn)).append(";\n");
        }
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBTableColumn> oldColumns,
            Collection<DBTableColumn> newColumns) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (CollectionUtils.isEmpty(oldColumns)) {
            if (CollectionUtils.isNotEmpty(newColumns)) {
                newColumns.forEach(column -> sqlBuilder.append(generateCreateObjectDDL(column)));
            }
            return sqlBuilder.toString();
        }
        if (CollectionUtils.isEmpty(newColumns)) {
            if (CollectionUtils.isNotEmpty(oldColumns)) {
                oldColumns.forEach(column -> sqlBuilder.append(generateDropObjectDDL(column)));
            }
            return sqlBuilder.toString();
        }
        Map<Integer, DBTableColumn> position2OldColumn = new HashMap<>();
        Map<Integer, DBTableColumn> position2NewColumn = new HashMap<>();

        oldColumns.forEach(oldColumn -> position2OldColumn.put(oldColumn.getOrdinalPosition(), oldColumn));
        newColumns.forEach(newColumn -> {
            if (Objects.nonNull(newColumn.getOrdinalPosition())) {
                position2NewColumn.put(newColumn.getOrdinalPosition(), newColumn);
            }
        });
        for (DBTableColumn newColumn : newColumns) {
            // ordinaryPosition is NULL means this is a new column
            if (Objects.isNull(newColumn.getOrdinalPosition())) {
                sqlBuilder.append(generateCreateObjectDDL(newColumn));
            } else if (position2OldColumn.containsKey(newColumn.getOrdinalPosition())) {
                // this is an existing column
                sqlBuilder.append(generateUpdateObjectDDL(position2OldColumn.get(newColumn.getOrdinalPosition()),
                        newColumn));
            }
        }
        for (DBTableColumn oldColumn : oldColumns) {
            // means this column should be dropped
            if (!position2NewColumn.containsKey(oldColumn.getOrdinalPosition())) {
                sqlBuilder.append(generateDropObjectDDL(oldColumn));
            }
        }
        return sqlBuilder.toString();
    }

    protected String generateCreateDefinitionForUpdateDDL(DBTableColumn oldColumn, DBTableColumn newColumn) {
        return generateCreateDefinitionDDL(newColumn);
    }

    protected abstract void generateColumnComment(DBTableColumn column, SqlBuilder sqlBuilder);

    @Override
    public String generateDropObjectDDL(@NotNull DBTableColumn column) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(column))
                .append(" DROP COLUMN ").identifier(column.getName()).append(";\n");
        return sqlBuilder.toString();
    }

    protected abstract SqlBuilder sqlBuilder();

    protected String getFullyQualifiedTableName(@NotNull DBTableColumn column) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(column.getSchemaName())) {
            sqlBuilder.identifier(column.getSchemaName()).append(".");
        }
        if (StringUtils.isNotEmpty(column.getTableName())) {
            sqlBuilder.identifier(column.getTableName());
        }
        return sqlBuilder.toString();
    }

    public static class DataTypeModifier implements DBColumnModifier {

        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            String typeName = column.getTypeName();
            Long precision = column.getPrecision();
            Integer scale = column.getScale();
            sqlBuilder.space().append(typeName);
            if (StringUtils.equalsIgnoreCase(typeName, "enum") || StringUtils.equalsIgnoreCase(typeName, "set")) {
                if (CollectionUtils.isNotEmpty(column.getEnumValues())) {
                    List<String> quotedValues =
                            column.getEnumValues().stream()
                                    .map(StringUtils::quoteMysqlValue)
                                    .collect(Collectors.toList());
                    sqlBuilder.append("(").append(String.join(",", quotedValues)).append(")");
                }
                return;
            }
            if (Objects.isNull(scale)) {
                if (Objects.nonNull(precision)) {
                    sqlBuilder.append("(").append(String.valueOf(precision)).append(")");
                }
            } else {
                if (Objects.isNull(precision)) {
                    sqlBuilder.append("(").append(String.valueOf(scale)).append(")");
                } else {
                    sqlBuilder.append("(").append(String.valueOf(precision)).append(", ").append(String.valueOf(scale))
                            .append(")");
                }
            }
            if (DataTypeUtil.isDateType(column.getTypeName())
                    && Objects.nonNull(column.getOnUpdateCurrentTimestamp())) {
                if (column.getOnUpdateCurrentTimestamp()) {
                    sqlBuilder.append(" ON UPDATE CURRENT_TIMESTAMP");
                    if (Objects.nonNull(precision)) {
                        sqlBuilder.append("(").append(String.valueOf(precision)).append(")");
                    }
                }
            }
            if (DataTypeUtil.isIntegerType(column.getTypeName()) && Objects.nonNull(column.getUnsigned())) {
                sqlBuilder.append(column.getUnsigned() ? " UNSIGNED " : "");
            }
            if (DataTypeUtil.isIntegerType(column.getTypeName()) && Objects.nonNull(column.getZerofill())) {
                sqlBuilder.append(column.getZerofill() ? " ZEROFILL " : "");
            }
        }
    }

    protected abstract List<DBColumnModifier> getSupportColumnModifiers();

    protected interface DBColumnModifier {
        void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder);
    }

    public static class NullNotNullModifier implements DBColumnModifier {
        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            sqlBuilder.append(column.getNullable() ? " NULL " : " NOT NULL");
        }
    }

}
