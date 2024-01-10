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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.util.DBObjectEditorUtils;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: Lebie
 * @Date: 2022/7/19 下午4:51
 * @Description: []
 */
public abstract class DBTableEditor implements DBObjectEditor<DBTable> {
    @Getter
    @Setter
    protected DBObjectEditor<DBTableIndex> indexEditor;

    @Getter
    @Setter
    protected DBObjectEditor<DBTableColumn> columnEditor;

    @Getter
    @Setter
    protected DBObjectEditor<DBTableConstraint> constraintEditor;

    @Getter
    @Setter
    protected DBObjectEditor<DBTablePartition> partitionEditor;

    public DBTableEditor(DBObjectEditor<DBTableIndex> indexEditor, DBObjectEditor<DBTableColumn> columnEditor,
            DBObjectEditor<DBTableConstraint> constraintEditor, DBObjectEditor<DBTablePartition> partitionEditor) {
        this.indexEditor = indexEditor;
        this.columnEditor = columnEditor;
        this.constraintEditor = constraintEditor;
        this.partitionEditor = partitionEditor;
    }

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public String generateCreateObjectDDL(@NotNull DBTable table) {
        fillSchemaNameAndTableName(table);
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("CREATE TABLE ").append(getFullyQualifiedTableName(table))
                .append(" (").line();
        boolean isFirstSentence = true;
        for (DBTableColumn column : table.getColumns()) {
            if (!isFirstSentence) {
                sqlBuilder.append(",").line();
            }
            isFirstSentence = false;
            sqlBuilder.append(columnEditor.generateCreateDefinitionDDL(column));
        }
        if (createIndexWhenCreatingTable()) {
            for (DBTableIndex index : excludePrimaryKeyIndex(table.getIndexes(), table.getConstraints())) {
                if (!isFirstSentence) {
                    sqlBuilder.append(",").line();
                }
                isFirstSentence = false;
                sqlBuilder.append(indexEditor.generateCreateDefinitionDDL(index));
            }
        }
        for (DBTableConstraint constraint : excludeUniqueConstraint(table.getIndexes(), table.getConstraints())) {
            if (!isFirstSentence) {
                sqlBuilder.append(",").line();
            }
            isFirstSentence = false;
            sqlBuilder.append(constraintEditor.generateCreateDefinitionDDL(constraint));
        }
        sqlBuilder.line().append(") ");
        appendTableOptions(table, sqlBuilder);
        if (Objects.nonNull(table.getPartition())) {
            sqlBuilder.append(partitionEditor.generateCreateDefinitionDDL(table.getPartition()));
        }
        sqlBuilder.append(";\n");
        appendTableComment(table, sqlBuilder);
        appendColumnComment(table, sqlBuilder);
        return sqlBuilder.toString();
    }

    protected abstract void appendColumnComment(DBTable table, SqlBuilder sqlBuilder);

    protected abstract void appendTableComment(DBTable table, SqlBuilder sqlBuilder);

    protected abstract boolean createIndexWhenCreatingTable();

    protected abstract void appendTableOptions(DBTable table, SqlBuilder sqlBuilder);

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBTable table) {
        return generateCreateObjectDDL(table);
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBTable table) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("DROP TABLE ").append(getFullyQualifiedTableName(table));
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectDDL(@NotNull DBTable oldTable,
            @NotNull DBTable newTable) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (!StringUtils.equals(oldTable.getName(), newTable.getName())) {
            sqlBuilder.append(generateRenameObjectDDL(oldTable, newTable));
            sqlBuilder.append(";\n");
        }
        generateUpdateTableOptionDDL(oldTable, newTable, sqlBuilder);
        fillSchemaNameAndTableName(oldTable, newTable);
        sqlBuilder.append(columnEditor.generateUpdateObjectListDDL(oldTable.getColumns(), newTable.getColumns()));
        sqlBuilder.append(indexEditor.generateUpdateObjectListDDL(oldTable.getIndexes(), newTable.getIndexes()));
        sqlBuilder.append(
                constraintEditor.generateUpdateObjectListDDL(oldTable.getConstraints(), newTable.getConstraints()));
        sqlBuilder.append(partitionEditor.generateUpdateObjectDDL(oldTable.getPartition(), newTable.getPartition()));
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBTable> oldTables,
            Collection<DBTable> newTables) {
        throw new NotImplementedException();
    }

    public String generateUpdateObjectDDLWithoutRenaming(@NotNull DBTable oldTable,
            @NotNull DBTable newTable) {
        SqlBuilder sqlBuilder = sqlBuilder();
        generateUpdateTableOptionDDL(oldTable, newTable, sqlBuilder);
        fillSchemaNameAndTableName(oldTable, newTable);
        DBObjectEditorUtils.generateShadowTableColumnListUpdateDDL(oldTable.getColumns(), newTable.getColumns(),
                columnEditor, sqlBuilder);
        DBObjectEditorUtils.generateShadowIndexListUpdateDDL(
                excludePrimaryKeyIndex(oldTable.getIndexes(), oldTable.getConstraints()),
                excludePrimaryKeyIndex(newTable.getIndexes(), newTable.getConstraints()),
                indexEditor, sqlBuilder);
        DBObjectEditorUtils.generateShadowTableConstraintListUpdateDDL(
                excludeUniqueConstraint(oldTable.getIndexes(), oldTable.getConstraints()),
                excludeUniqueConstraint(newTable.getIndexes(), newTable.getConstraints()),
                constraintEditor, sqlBuilder);
        DBTablePartitionEditor editor = (DBTablePartitionEditor) partitionEditor;
        sqlBuilder.append(editor.generateShadowTableUpdateObjectDDL(oldTable.getPartition(), newTable.getPartition()));
        return sqlBuilder.toString();
    }

    public abstract void generateUpdateTableOptionDDL(DBTable oldTable, DBTable newTable, SqlBuilder sqlBuilder);

    protected abstract SqlBuilder sqlBuilder();

    protected String getFullyQualifiedTableName(@NotNull DBTable table) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(table.getSchemaName())) {
            sqlBuilder.identifier(table.getSchemaName()).append(".");
        }
        sqlBuilder.identifier(table.getName());
        return sqlBuilder.toString();
    }

    /**
     * 排除唯一性约束</br>
     * 创建一个唯一索引，OB 会自动创建一个同名唯一约束；因此在生成 DDL 时，如果已有唯一索引，则需要忽略掉同名唯一约束，不然生成的 DDL 会无法执行</br>
     */
    public List<DBTableConstraint> excludeUniqueConstraint(List<DBTableIndex> indexes,
            List<DBTableConstraint> constraints) {
        if (CollectionUtils.isEmpty(indexes) || CollectionUtils.isEmpty(constraints)) {
            return constraints;
        }
        Set<String> uniqueIndexNames =
                indexes.stream()
                        .filter(index -> index.getType() == DBIndexType.UNIQUE)
                        .map(DBTableIndex::getName).collect(Collectors.toSet());
        // 过滤掉唯一索引同名的唯一约束
        return constraints.stream()
                .filter(constraint -> !uniqueIndexNames.contains(constraint.getName())
                        || constraint.getType() != DBConstraintType.UNIQUE_KEY)
                .collect(Collectors.toList());
    }

    /**
     * 排除主键约束对应的唯一索引</br>
     * 创建主键约束的时候，OB 会自动创建一个同名唯一索引；因此在生成 DDL 时，如果已有主键约束，则需要忽略掉同名唯一索引，不然生成的 DDL 会无法执行</br>
     */
    public List<DBTableIndex> excludePrimaryKeyIndex(List<DBTableIndex> indexes,
            List<DBTableConstraint> constraints) {
        if (CollectionUtils.isEmpty(indexes) || CollectionUtils.isEmpty(constraints)) {
            return indexes;
        }
        Optional<String> primaryKeyNameOpt =
                constraints.stream()
                        .filter(constraint -> constraint.getType() == DBConstraintType.PRIMARY_KEY)
                        .findFirst()
                        .map(DBTableConstraint::getName);
        return indexes.stream()
                .filter(index -> !StringUtils.equals(index.getName(), primaryKeyNameOpt.orElse(Strings.EMPTY))
                        || index.getType() != DBIndexType.UNIQUE)
                .collect(Collectors.toList());
    }

    private void fillSchemaNameAndTableName(DBTable oldTable, DBTable newTable) {
        fillSchemaNameAndTableName(oldTable);
        fillSchemaNameAndTableName(newTable);
    }

    private void fillSchemaNameAndTableName(DBTable table) {
        fillColumnSchemaNameAndTableName(table.getColumns(), table.getSchemaName(), table.getName());
        fillIndexSchemaNameAndTableName(table.getIndexes(), table.getSchemaName(), table.getName());
        fillConstraintSchemaNameAndTableName(table.getConstraints(), table.getSchemaName(), table.getName());
        fillPartitionSchemaNameAndTableName(table.getPartition(), table.getSchemaName(), table.getName());
    }

    private void fillColumnSchemaNameAndTableName(List<DBTableColumn> columns, String schemaName, String tableName) {
        if (CollectionUtils.isNotEmpty(columns)) {
            columns.forEach(column -> {
                column.setSchemaName(schemaName);
                column.setTableName(tableName);
            });
        }
    }

    private void fillIndexSchemaNameAndTableName(List<DBTableIndex> indexes, String schemaName, String tableName) {
        if (CollectionUtils.isNotEmpty(indexes)) {
            indexes.forEach(index -> {
                index.setSchemaName(schemaName);
                index.setTableName(tableName);
            });
        }
    }

    private void fillConstraintSchemaNameAndTableName(List<DBTableConstraint> constraints, String schemaName,
            String tableName) {
        if (CollectionUtils.isNotEmpty(constraints)) {
            constraints.forEach(constraint -> {
                constraint.setSchemaName(schemaName);
                constraint.setTableName(tableName);
            });
        }
    }

    private void fillPartitionSchemaNameAndTableName(DBTablePartition constraint, String schemaName,
            String tableName) {
        if (Objects.nonNull(constraint)) {
            constraint.setSchemaName(schemaName);
            constraint.setTableName(tableName);
        }
    }

}
