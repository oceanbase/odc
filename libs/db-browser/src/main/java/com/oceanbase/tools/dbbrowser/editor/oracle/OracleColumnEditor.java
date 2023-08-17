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
package com.oceanbase.tools.dbbrowser.editor.oracle;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2022/7/25 上午1:40
 * @Description: []
 */
public class OracleColumnEditor extends DBTableColumnEditor {

    @Override
    protected boolean appendColumnKeyWord() {
        return false;
    }

    @Override
    protected SqlBuilder sqlBuilder() {
        return new OracleSqlBuilder();
    }

    @Override
    protected List<DBColumnModifier> getSupportColumnModifiers() {
        return Arrays.asList(
                new OracleDataColumnModifier(),
                new ExtraInfoModifier(),
                new NullNotNullModifier(),
                new DefaultOptionModifier());
    }

    @Override
    protected String generateCreateDefinitionForUpdateDDL(DBTableColumn oldColumn, DBTableColumn newColumn) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.identifier(newColumn.getName());
        List<DBColumnModifier> columnModifiersForUpdate = getSupportColumnModifiers();
        if (Objects.equals(newColumn.getNullable(), oldColumn.getNullable())) {
            columnModifiersForUpdate = columnModifiersForUpdate.stream()
                    .filter(m -> !m.getClass().equals(NullNotNullModifier.class))
                    .collect(Collectors.toList());
        }
        columnModifiersForUpdate.forEach(modifier -> modifier.appendModifier(newColumn, sqlBuilder));
        return sqlBuilder.toString().trim();
    }

    protected static class ExtraInfoModifier implements DBColumnModifier {

        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            if (Objects.nonNull(column.getVirtual()) && column.getVirtual()) {
                if (StringUtils.isEmpty(column.getGenExpression())) {
                    column.setGenExpression(StringUtils.EMPTY);
                }
                sqlBuilder.append(" AS (").append(column.getGenExpression()).append(")").append(" VIRTUAL ");
            }
        }
    }

    protected static class OracleDataColumnModifier implements DBColumnModifier {

        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            String typeName = column.getTypeName();
            Long precision = column.getPrecision();
            Integer scale = column.getScale();
            Integer yearPrecision = column.getYearPrecision();
            Integer dayPrecision = column.getDayPrecision();
            Integer secondPrecision = column.getSecondPrecision();
            if ("INTERVAL YEAR TO MONTH".equalsIgnoreCase(typeName)
                    || "INTERVAL DAY TO SECOND".equalsIgnoreCase(typeName)) {
                if ("INTERVAL YEAR TO MONTH".equalsIgnoreCase(typeName)) {
                    if (Objects.nonNull(yearPrecision)) {
                        sqlBuilder.append(" INTERVAL YEAR(").append(String.valueOf(yearPrecision)).append(") TO MONTH");
                    } else {
                        sqlBuilder.append(" INTERVAL YEAR TO MONTH");
                    }
                }
                if ("INTERVAL DAY TO SECOND".equalsIgnoreCase(typeName)) {
                    if (Objects.isNull(secondPrecision)) {
                        if (Objects.nonNull(dayPrecision)) {
                            sqlBuilder.append(" INTERVAL DAY(").append(String.valueOf(dayPrecision))
                                    .append(") TO SECOND");
                        } else {
                            sqlBuilder.append(" INTERVAL DAY TO SECOND");
                        }
                    } else {
                        if (Objects.nonNull(dayPrecision)) {
                            sqlBuilder.append(" INTERVAL DAY(").append(String.valueOf(dayPrecision))
                                    .append(") TO SECOND(")
                                    .append(String.valueOf(secondPrecision)).append(")");
                        } else {
                            sqlBuilder.append(" INTERVAL DAY TO SECOND(").append(String.valueOf(secondPrecision))
                                    .append(")");
                        }
                    }
                }
            } else if (StringUtils.startsWith(typeName, "TIMESTAMP")) {
                if (Objects.nonNull(secondPrecision)) {
                    String typeNameWithPrecision =
                            typeName.replaceFirst("TIMESTAMP", "TIMESTAMP" + "(" + secondPrecision + ")");
                    sqlBuilder.space().append(typeNameWithPrecision).space();
                } else {
                    sqlBuilder.space().append(typeName).space();
                }
            } else if ("NUMBER".equalsIgnoreCase(typeName)) {
                if (Objects.isNull(precision) && Objects.nonNull(scale)) {
                    sqlBuilder.space().append(typeName).append("(*, ").append(String.valueOf(scale)).append(")");
                } else {
                    new DataTypeModifier().appendModifier(column, sqlBuilder);
                }
            } else {
                new DataTypeModifier().appendModifier(column, sqlBuilder);
            }
        }
    }

    protected static class DefaultOptionModifier implements DBColumnModifier {
        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            /**
             * should not append default value to virtual column
             */
            if (Objects.nonNull(column.getVirtual()) && column.getVirtual()) {
                return;
            }
            String defaultValue = StringUtils.isEmpty(column.getDefaultValue()) ? "NULL" : column.getDefaultValue();
            sqlBuilder.append(" DEFAULT ").append(defaultValue);
        }
    }

    @Override
    public String generateUpdateObjectDDL(@NotNull DBTableColumn oldColumn,
            @NotNull DBTableColumn newColumn) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (!StringUtils.equals(oldColumn.getName(), newColumn.getName())) {
            sqlBuilder.append(generateRenameObjectDDL(oldColumn, newColumn)).append(";\n");
        }
        if (!StringUtils.equals(oldColumn.getComment(), newColumn.getComment())) {
            generateColumnComment(newColumn, sqlBuilder);
            oldColumn.setComment(null);
            newColumn.setComment(null);
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
    protected void generateColumnComment(DBTableColumn column, SqlBuilder sqlBuilder) {
        if (Objects.nonNull(column.getComment())) {
            sqlBuilder.append("COMMENT ON COLUMN ").append(getFullyQualifiedTableName(column)).append(".")
                    .identifier(column.getName())
                    .append(" IS ").value(column.getComment()).append(";").line();
        }
    }
}
