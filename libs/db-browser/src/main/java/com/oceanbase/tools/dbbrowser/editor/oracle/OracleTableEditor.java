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

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.tools.dbbrowser.editor.DBObjectEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2022/7/19 下午10:01
 * @Description: []
 */
public class OracleTableEditor extends DBTableEditor {

    public OracleTableEditor(DBObjectEditor<DBTableIndex> indexEditor,
            DBObjectEditor<DBTableColumn> columnEditor,
            DBObjectEditor<DBTableConstraint> constraintEditor,
            DBObjectEditor<DBTablePartition> partitionEditor) {
        super(indexEditor, columnEditor, constraintEditor, partitionEditor);
    }

    @Override
    protected boolean createIndexWhenCreatingTable() {
        return false;
    }

    @Override
    protected void appendTableOptions(DBTable table, SqlBuilder sqlBuilder) {
        if (Objects.isNull(table.getTableOptions())) {
            return;
        }
        DBTableOptions options = table.getTableOptions();
        if (StringUtils.isNotBlank(options.getCompressionOption())) {
            sqlBuilder.append("COMPRESS ").append(options.getCompressionOption()).space();
        }
        if (Objects.nonNull(options.getReplicaNum())) {
            sqlBuilder.append("REPLICA_NUM = ").append(String.valueOf(options.getReplicaNum())).space();
        }
        if (Objects.nonNull(options.getUseBloomFilter())) {
            sqlBuilder.append("USE_BLOOM_FILTER = ").append(options.getUseBloomFilter() ? "TRUE" : "FALSE").space();
        }
        if (Objects.nonNull(options.getTabletSize())) {
            sqlBuilder.append("TABLET_SIZE = ").append(String.valueOf(options.getTabletSize())).space();
        }
    }

    @Override
    public void generateUpdateTableOptionDDL(DBTable oldTable, DBTable newTable, SqlBuilder sqlBuilder) {
        if (!StringUtils.equals(oldTable.getTableOptions().getComment(), newTable.getTableOptions().getComment())) {
            appendTableComment(newTable, sqlBuilder);
        }
    }

    @Override
    public String generateCreateObjectDDL(@NotNull DBTable table) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append(super.generateCreateObjectDDL(table));
        if (CollectionUtils.isNotEmpty(excludePrimaryKeyIndex(table.getIndexes(), table.getConstraints()))) {
            for (DBTableIndex index : table.getIndexes()) {
                sqlBuilder.append(indexEditor.generateCreateObjectDDL(index)).append(";").line();
            }
        }
        return sqlBuilder.toString();
    }

    @Override
    protected void appendColumnComment(@NotNull DBTable table, @NotNull SqlBuilder sqlBuilder) {
        if (CollectionUtils.isEmpty(table.getColumns())) {
            return;
        }
        table.getColumns().forEach(column -> {
            if (Objects.nonNull(column.getComment())) {
                sqlBuilder.append("COMMENT ON COLUMN ").append(getFullyQualifiedTableName(table)).append(".")
                        .identifier(column.getName())
                        .append(" IS ").value(column.getComment()).append(";").line();
            }
        });
    }

    @Override
    protected void appendTableComment(DBTable table, SqlBuilder sqlBuilder) {
        if (Objects.nonNull(table.getTableOptions()) && Objects.nonNull(table.getTableOptions().getComment())) {
            sqlBuilder.append("COMMENT ON TABLE ").append(getFullyQualifiedTableName(table)).append(" IS ")
                    .value(table.getTableOptions().getComment()).append(";\n");
        }
    }

    @Override
    protected SqlBuilder sqlBuilder() {
        return new OracleSqlBuilder();
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBTable oldTable,
            @NotNull DBTable newTable) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("RENAME ").identifier(oldTable.getName()).append(" TO ").identifier(newTable.getName());
        return sqlBuilder.toString();
    }

}
