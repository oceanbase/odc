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

import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2022/7/18 下午9:47
 * @Description: []
 */
public abstract class DBTableIndexEditor implements DBObjectEditor<DBTableIndex> {

    @Override
    public String generateCreateObjectDDL(@NotNull DBTableIndex index) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("CREATE ");
        appendIndexModifiers(index, sqlBuilder);
        sqlBuilder.append(" INDEX ").identifier(index.getName());
        appendIndexType(index, sqlBuilder);
        sqlBuilder.append(" ON ").append(getFullyQualifiedTableName(index))
                .append(" (");
        appendIndexColumns(index, sqlBuilder);
        sqlBuilder.append(")");
        appendIndexOptions(index, sqlBuilder);
        if (CollectionUtils.isNotEmpty(index.getColumnGroups())) {
            sqlBuilder.append(" WITH COLUMN GROUP(")
                    .append(index.getColumnGroups().stream().map(DBColumnGroupElement::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        return sqlBuilder.toString().trim() + ";\n";
    }

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBTableIndex index) {
        SqlBuilder sqlBuilder = sqlBuilder();
        appendIndexModifiers(index, sqlBuilder);
        sqlBuilder.append(" INDEX ").identifier(index.name());
        appendIndexType(index, sqlBuilder);
        sqlBuilder.append(" (");
        appendIndexColumns(index, sqlBuilder);
        sqlBuilder.append(")");
        appendIndexOptions(index, sqlBuilder);
        if (CollectionUtils.isNotEmpty(index.getColumnGroups())) {
            sqlBuilder.append(" WITH COLUMN GROUP(")
                    .append(index.getColumnGroups().stream().map(DBColumnGroupElement::toString)
                            .collect(Collectors.joining(",")))
                    .append(")");
        }
        return sqlBuilder.toString().trim();
    }

    protected void appendIndexColumns(DBTableIndex index, SqlBuilder sqlBuilder) {
        List<String> columnNames = index.getColumnNames();
        if (Objects.isNull(columnNames)) {
            return;
        }
        boolean isFirstColumn = true;
        for (String columnName : columnNames) {
            if (!isFirstColumn) {
                sqlBuilder.append(", ");
            }
            isFirstColumn = false;
            sqlBuilder.identifier(columnName);
            appendIndexColumnModifiers(index, sqlBuilder);
        }
    }

    protected abstract void appendIndexColumnModifiers(DBTableIndex index, SqlBuilder sqlBuilder);

    protected void appendIndexType(DBTableIndex index, SqlBuilder sqlBuilder) {}

    protected void appendIndexModifiers(DBTableIndex index, SqlBuilder sqlBuilder) {
        if (index.getType() == DBIndexType.UNIQUE) {
            sqlBuilder.append("UNIQUE");
        }
    }

    protected abstract void appendIndexOptions(DBTableIndex index, SqlBuilder sqlBuilder);

    @Override
    public String generateUpdateObjectDDL(@NotNull DBTableIndex oldIndex, @NotNull DBTableIndex newIndex) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (!Objects.equals(oldIndex, newIndex)) {
            String drop = generateDropObjectDDL(oldIndex);
            sqlBuilder.append(drop)
                    .append(generateCreateObjectDDL(newIndex));
            return sqlBuilder.toString();
        }
        if (!StringUtils.equals(oldIndex.getName(), newIndex.getName())) {
            sqlBuilder.append(generateRenameObjectDDL(oldIndex, newIndex)).append(";").line();
        }
        sqlBuilder.append(generateUpdateVisibility(oldIndex, newIndex));
        return sqlBuilder.toString();
    }

    private String generateUpdateVisibility(@NotNull DBTableIndex oldIndex, @NotNull DBTableIndex newIndex) {
        if (Objects.equals(oldIndex.getVisible(), newIndex.getVisible())) {
            return "";
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        String visibility = Boolean.FALSE.equals(newIndex.getVisible()) ? "INVISIBLE" : "VISIBLE";
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(newIndex)).append(" ALTER INDEX ")
                .identifier(newIndex.getName()).space().append(visibility).append(";").line();
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBTableIndex> oldIndexes,
            Collection<DBTableIndex> newIndexes) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (CollectionUtils.isEmpty(oldIndexes)) {
            if (CollectionUtils.isNotEmpty(newIndexes)) {
                newIndexes.forEach(column -> sqlBuilder.append(generateCreateObjectDDL(column)));
            }
            return sqlBuilder.toString();
        }
        if (CollectionUtils.isEmpty(newIndexes)) {
            if (CollectionUtils.isNotEmpty(oldIndexes)) {
                oldIndexes.forEach(column -> sqlBuilder.append(generateDropObjectDDL(column)));
            }
            return sqlBuilder.toString();
        }
        Map<Integer, DBTableIndex> position2OldIndex = new HashMap<>();
        Map<Integer, DBTableIndex> position2NewIndex = new HashMap<>();

        oldIndexes.forEach(oldIndex -> position2OldIndex.put(oldIndex.getOrdinalPosition(), oldIndex));
        newIndexes.forEach(newIndex -> {
            if (Objects.nonNull(newIndex.getOrdinalPosition())) {
                position2NewIndex.put(newIndex.getOrdinalPosition(), newIndex);
            }
        });
        for (DBTableIndex newIndex : newIndexes) {
            // ordinaryPosition is NULL means this is a new index
            if (Objects.isNull(newIndex.getOrdinalPosition())) {
                sqlBuilder.append(generateCreateObjectDDL(newIndex));
            } else if (position2OldIndex.containsKey(newIndex.getOrdinalPosition())) {
                // this is an existing index
                sqlBuilder.append(generateUpdateObjectDDL(position2OldIndex.get(newIndex.getOrdinalPosition()),
                        newIndex));
            }
        }
        for (DBTableIndex oldIndex : oldIndexes) {
            // means this index should be dropped
            if (!position2NewIndex.containsKey(oldIndex.getOrdinalPosition())) {
                sqlBuilder.append(generateDropObjectDDL(oldIndex));
            }
        }
        return sqlBuilder.toString();
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBTableIndex dbObject) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(dbObject))
                .append(" DROP INDEX ").identifier(dbObject.getName());
        return sqlBuilder.toString().trim() + ";\n";
    }

    protected abstract SqlBuilder sqlBuilder();

    protected String getFullyQualifiedTableName(@NotNull DBTableIndex index) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(index.getSchemaName())) {
            sqlBuilder.identifier(index.getSchemaName()).append(".");
        }
        if (StringUtils.isNotEmpty(index.getTableName())) {
            sqlBuilder.identifier(index.getTableName());
        }
        return sqlBuilder.toString();
    }

}
