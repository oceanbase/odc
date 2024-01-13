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
package com.oceanbase.tools.dbbrowser.editor.mysql;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2022/7/24 下午9:07
 * @Description: []
 */
public class MySQLColumnEditor extends DBTableColumnEditor {

    @Override
    protected SqlBuilder sqlBuilder() {
        return new MySQLSqlBuilder();
    }

    @Override
    protected boolean appendColumnKeyWord() {
        return true;
    }

    @Override
    protected List<DBColumnModifier> getSupportColumnModifiers() {
        return Arrays.asList(new DataTypeModifier(),
                new NullNotNullModifier(),
                new DefaultOptionModifier(),
                new ExtraInfoModifier(),
                new CharsetModifier(),
                new CollationModifier(),
                new CommentModifier());
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBTableColumn oldColumn,
            @NotNull DBTableColumn newColumn) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(oldColumn))
                .append(" CHANGE COLUMN ").identifier(oldColumn.getName()).space()
                .append(generateCreateDefinitionForUpdateDDL(oldColumn, newColumn));
        return sqlBuilder.toString();
    }

    @Override
    protected void generateColumnComment(DBTableColumn column, SqlBuilder sqlBuilder) {}

    protected static class CharsetModifier implements DBColumnModifier {

        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            if (DataTypeUtil.isStringType(column.getTypeName()) && StringUtils.isNotBlank(column.getCharsetName())) {
                sqlBuilder.append(" CHARACTER SET ").append(column.getCharsetName());
            }
        }
    }

    protected static class CollationModifier implements DBColumnModifier {

        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            if (DataTypeUtil.isStringType(column.getTypeName()) && StringUtils.isNotBlank(column.getCollationName())) {
                sqlBuilder.append(" COLLATE ").append(column.getCollationName());
            }
        }
    }

    protected static class ExtraInfoModifier implements DBColumnModifier {

        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            if (Objects.nonNull(column.getVirtual()) && column.getVirtual()) {
                if (StringUtils.isEmpty(column.getGenExpression())) {
                    column.setGenExpression(StringUtils.EMPTY);
                }
                sqlBuilder.append(" GENERATED ALWAYS AS (").append(column.getGenExpression()).append(")");
                if (Objects.nonNull(column.getStored()) && column.getStored()) {
                    sqlBuilder.append(" STORED ");
                } else {
                    sqlBuilder.append(" VIRTUAL ");
                }
            }
            if (Objects.nonNull(column.getAutoIncrement())) {
                sqlBuilder.append(column.getAutoIncrement() ? " AUTO_INCREMENT " : "");
            }
        }
    }

    protected static class CommentModifier implements DBColumnModifier {

        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            if (StringUtils.isNotBlank(column.getComment())) {
                sqlBuilder.append(" COMMENT ").value(column.getComment());
            }
        }
    }

    protected class DefaultOptionModifier implements DBColumnModifier {
        @Override
        public void appendModifier(DBTableColumn column, SqlBuilder sqlBuilder) {
            String defaultValue = column.getDefaultValue();
            if (StringUtils.isNotEmpty(defaultValue) && (Objects.isNull(column.getVirtual()) || !column.getVirtual())) {
                sqlBuilder.append(" DEFAULT ");
                if (isDefaultValueBuiltInFunction(column)) {
                    // 默认值是 内置函数，则不需要用引号转义，直接拼接即可
                    sqlBuilder.append(defaultValue);
                } else if (defaultValue.equals("(empty_string)")) {
                    // 如果默认值是(empty_string),设置default 为 ''
                    sqlBuilder.append("''");
                } else {
                    /**
                     * 不是内置函数作为默认值的话，都可以用单引号包围，不是字符串类型的，也会被转义成对应的类型值。比如： col1 int(11) DEFAULT '2' 会被内核自动转换为 col1 int(11)
                     * DEFAULT 2
                     */
                    sqlBuilder.append("'").append(defaultValue.replace("'", "''")).append("'");
                }
            }
        }
    }

    /**
     * Check whether the data_default contain built in function. Any of the synonyms for
     * CURRENT_TIMESTAMP have the same meaning as CURRENT_TIMESTAMP. These are CURRENT_TIMESTAMP(),
     * NOW(), LOCALTIME, LOCALTIME(), LOCALTIMESTAMP, and LOCALTIMESTAMP().
     */
    private boolean isDefaultValueBuiltInFunction(DBTableColumn column) {
        return StringUtils.isEmpty(column.getDefaultValue())
                || (!DataTypeUtil.isStringType(column.getTypeName())
                        && column.getDefaultValue().trim().toUpperCase(Locale.getDefault())
                                .startsWith("CURRENT_TIMESTAMP"));
    }
}
