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
                new CharsetModifier(),
                new CollationModifier(),
                new DefaultOptionModifier(),
                new ExtraInfoModifier(),
                new CommentModifier(),
                new NullNotNullModifier());
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
            if ((Objects.isNull(column.getVirtual()) || !column.getVirtual())) {
                if (StringUtils.isNotEmpty(defaultValue)) {
                    sqlBuilder.append(" DEFAULT " + defaultValue);
                }
            }
        }
    }
}
