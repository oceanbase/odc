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

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.editor.DBObjectEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2022/7/19 下午10:00
 * @Description: []
 */
public class MySQLTableEditor extends DBTableEditor {

    public MySQLTableEditor(DBObjectEditor<DBTableIndex> indexEditor,
            DBObjectEditor<DBTableColumn> columnEditor,
            DBObjectEditor<DBTableConstraint> constraintEditor, DBObjectEditor<DBTablePartition> partitionEditor) {
        super(indexEditor, columnEditor, constraintEditor, partitionEditor);
    }

    @Override
    protected void appendColumnComment(DBTable table, SqlBuilder sqlBuilder) {}

    @Override
    protected void appendTableComment(DBTable table, SqlBuilder sqlBuilder) {}

    @Override
    protected boolean createIndexWhenCreatingTable() {
        return true;
    }

    @Override
    protected void appendTableOptions(DBTable table, SqlBuilder sqlBuilder) {
        if (Objects.isNull(table.getTableOptions())) {
            return;
        }
        DBTableOptions options = table.getTableOptions();
        if (Objects.nonNull(options.getAutoIncrementInitialValue())) {
            sqlBuilder.append("AUTO_INCREMENT = ")
                    .append(String.valueOf(options.getAutoIncrementInitialValue())).space();
        }
        if (StringUtils.isNotBlank(options.getCharsetName())) {
            sqlBuilder.append("DEFAULT CHARSET = ").append(options.getCharsetName()).space();
        }
        if (StringUtils.isNotBlank(options.getCollationName())) {
            sqlBuilder.append("COLLATE = ").append(options.getCollationName()).space();
        }
        if (StringUtils.isNotBlank(options.getCompressionOption())) {
            sqlBuilder.append("COMPRESSION = ").append(options.getCompressionOption()).space();
        }
        appendMoreTableOptions(table, sqlBuilder);
        if (StringUtils.isNotEmpty(options.getComment())) {
            sqlBuilder.append("COMMENT = ").value(options.getComment()).space();
        }
    }

    protected void appendMoreTableOptions(DBTable table, SqlBuilder sqlBuilder) {}

    @Override
    public void generateUpdateTableOptionDDL(@NonNull DBTable oldTable, @NonNull DBTable newTable,
            @NonNull SqlBuilder sqlBuilder) {
        if (Objects.isNull(oldTable.getTableOptions()) || Objects.isNull(newTable.getTableOptions())) {
            return;
        }
        if (!StringUtils.equals(oldTable.getTableOptions().getComment(), newTable.getTableOptions().getComment())) {
            sqlBuilder.append("ALTER TABLE ")
                    .append(getFullyQualifiedTableName(newTable))
                    .append(" COMMENT = ")
                    .value(newTable.getTableOptions().getComment())
                    .append(";\n");
        }
        if (!StringUtils.equals(oldTable.getTableOptions().getCharsetName(),
                newTable.getTableOptions().getCharsetName())) {
            sqlBuilder.append("ALTER TABLE ")
                    .append(getFullyQualifiedTableName(newTable))
                    .append(" CHARACTER SET = ")
                    .append(newTable.getTableOptions().getCharsetName())
                    .append(";\n");
        }
        if (!StringUtils.equals(oldTable.getTableOptions().getCollationName(),
                newTable.getTableOptions().getCollationName())) {
            sqlBuilder.append("ALTER TABLE ")
                    .append(getFullyQualifiedTableName(newTable))
                    .append(" COLLATE = ")
                    .append(newTable.getTableOptions().getCollationName())
                    .append(";\n");
        }
    }

    @Override
    protected SqlBuilder sqlBuilder() {
        return new MySQLSqlBuilder();
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBTable oldTable,
            @NotNull DBTable newTable) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ")
                .identifier(oldTable.getName())
                .append(" RENAME TO ")
                .identifier(newTable.getName());
        return sqlBuilder.toString();
    }

}
