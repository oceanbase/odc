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
package com.oceanbase.tools.dbbrowser.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.editor.DBObjectEditor;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

/**
 * @Author: Lebie
 * @Date: 2022/8/23 下午3:36
 * @Description: []
 */
public class DBObjectEditorUtils {

    public static void generateShadowTableColumnListUpdateDDL(List<DBTableColumn> oldColumns,
            List<DBTableColumn> newColumns,
            DBObjectEditor<DBTableColumn> editor, SqlBuilder sqlBuilder) {
        if (CollectionUtils.isEmpty(oldColumns)) {
            if (CollectionUtils.isNotEmpty(newColumns)) {
                newColumns.forEach(column -> sqlBuilder.append(editor.generateCreateObjectDDL(column)));
            }
            return;
        }
        if (CollectionUtils.isEmpty(newColumns)) {
            if (CollectionUtils.isNotEmpty(oldColumns)) {
                oldColumns.forEach(column -> sqlBuilder.append(editor.generateDropObjectDDL(column)));
            }
            return;
        }
        Map<String, DBTableColumn> name2OldColumn = new HashMap<>();
        Map<String, DBTableColumn> name2NewColumn = new HashMap<>();

        oldColumns.forEach(oldColumn -> name2OldColumn.put(oldColumn.getName(), oldColumn));
        newColumns.forEach(newColumn -> {
            if (Objects.nonNull(newColumn.getName())) {
                name2NewColumn.put(newColumn.getName(), newColumn);
            }
        });
        for (DBTableColumn newColumn : newColumns) {
            if (!name2OldColumn.containsKey(newColumn.getName())) {
                sqlBuilder.append(editor.generateCreateObjectDDL(newColumn));
            } else {
                // this is an existing column
                sqlBuilder.append(editor.generateUpdateObjectDDL(name2OldColumn.get(newColumn.getName()),
                        newColumn));
            }
        }
        for (DBTableColumn oldColumn : oldColumns) {
            // means this column should be dropped
            if (!name2NewColumn.containsKey(oldColumn.getName())) {
                sqlBuilder.append(editor.generateDropObjectDDL(oldColumn));
            }
        }
    }

    public static void generateShadowIndexListUpdateDDL(List<DBTableIndex> oldIndexes, List<DBTableIndex> newIndexes,
            DBObjectEditor<DBTableIndex> editor, SqlBuilder sqlBuilder) {
        if (CollectionUtils.isEmpty(oldIndexes)) {
            if (CollectionUtils.isNotEmpty(newIndexes)) {
                newIndexes.forEach(column -> sqlBuilder.append(editor.generateCreateObjectDDL(column)));
            }
            return;
        }
        if (CollectionUtils.isEmpty(newIndexes)) {
            if (CollectionUtils.isNotEmpty(oldIndexes)) {
                oldIndexes.forEach(column -> sqlBuilder.append(editor.generateDropObjectDDL(column)));
            }
            return;
        }
        Map<String, DBTableIndex> name2OldIndex = new HashMap<>();
        Map<String, DBTableIndex> name2NewIndex = new HashMap<>();

        oldIndexes.forEach(oldIndex -> name2OldIndex.put(oldIndex.getName(), oldIndex));
        newIndexes.forEach(newIndex -> {
            if (Objects.nonNull(newIndex.getName())) {
                name2NewIndex.put(newIndex.getName(), newIndex);
            }
        });
        for (DBTableIndex newIndex : newIndexes) {
            if (!name2OldIndex.containsKey(newIndex.getName())) {
                sqlBuilder.append(editor.generateCreateObjectDDL(newIndex));
            } else {
                // this is an existing index
                sqlBuilder.append(editor.generateUpdateObjectDDL(name2OldIndex.get(newIndex.getName()),
                        newIndex));
            }
        }
        for (DBTableIndex oldIndex : oldIndexes) {
            // means this index should be dropped
            if (!name2NewIndex.containsKey(oldIndex.getName())) {
                sqlBuilder.append(editor.generateDropObjectDDL(oldIndex));
            }
        }
    }

    public static void generateShadowTableConstraintListUpdateDDL(List<DBTableConstraint> oldConstraints,
            List<DBTableConstraint> newConstraints,
            DBObjectEditor<DBTableConstraint> editor, SqlBuilder sqlBuilder) {
        if (Objects.isNull(oldConstraints)) {
            oldConstraints = new ArrayList<>();
        }

        if (Objects.isNull(newConstraints)) {
            newConstraints = new ArrayList<>();
        }

        Map<DBConstraintType, List<DBTableConstraint>> type2OldConstraints =
                oldConstraints.stream().collect(Collectors.groupingBy(DBTableConstraint::getType));

        Map<DBConstraintType, List<DBTableConstraint>> type2NewConstraints =
                newConstraints.stream().collect(Collectors.groupingBy(DBTableConstraint::getType));

        generateForeignKeyDDL(type2OldConstraints.getOrDefault(DBConstraintType.FOREIGN_KEY, new ArrayList<>()),
                type2NewConstraints.getOrDefault(DBConstraintType.FOREIGN_KEY, new ArrayList<>()), editor,
                sqlBuilder);

        List<DBTableConstraint> oldConstraintsWithoutFK = new ArrayList<>();
        List<DBTableConstraint> newConstraintsWithoutFK = new ArrayList<>();

        type2OldConstraints.forEach((dbConstraintType, constraints) -> {
            if (dbConstraintType != DBConstraintType.FOREIGN_KEY) {
                oldConstraintsWithoutFK.addAll(constraints);
            }
        });

        type2NewConstraints.forEach((dbConstraintType, constraints) -> {
            if (dbConstraintType != DBConstraintType.FOREIGN_KEY) {
                newConstraintsWithoutFK.addAll(constraints);
            }
        });

        if (CollectionUtils.isEmpty(oldConstraintsWithoutFK)) {
            if (CollectionUtils.isNotEmpty(newConstraintsWithoutFK)) {
                newConstraintsWithoutFK.forEach(
                        constraint -> sqlBuilder.append(editor.generateCreateObjectDDL(constraint)));
            }
            return;
        }
        if (CollectionUtils.isEmpty(newConstraintsWithoutFK)) {
            if (CollectionUtils.isNotEmpty(oldConstraintsWithoutFK)) {
                oldConstraintsWithoutFK.forEach(
                        constraint -> sqlBuilder.append(editor.generateDropObjectDDL(constraint)));
            }
            return;
        }
        Map<String, DBTableConstraint> name2OldConstraint = new HashMap<>();
        Map<String, DBTableConstraint> name2NewConstraint = new HashMap<>();

        oldConstraintsWithoutFK.forEach(
                oldConstraint -> name2OldConstraint.put(oldConstraint.getName(), oldConstraint));
        newConstraintsWithoutFK.forEach(newConstraint -> {
            if (Objects.nonNull(newConstraint.getName())) {
                name2NewConstraint.put(newConstraint.getName(), newConstraint);
            }
        });
        for (DBTableConstraint newConstraint : newConstraintsWithoutFK) {
            if (!name2OldConstraint.containsKey(newConstraint.getName())) {
                sqlBuilder.append(editor.generateCreateObjectDDL(newConstraint));
            } else {
                // this is an existing constraint
                sqlBuilder.append(
                        editor.generateUpdateObjectDDL(name2OldConstraint.get(newConstraint.getName()),
                                newConstraint));
            }
        }
        for (DBTableConstraint oldConstraint : oldConstraintsWithoutFK) {
            // means this constraint should be dropped
            if (!name2NewConstraint.containsKey(oldConstraint.getName())) {
                sqlBuilder.append(editor.generateDropObjectDDL(oldConstraint));
            }
        }
    }

    private static void generateForeignKeyDDL(List<DBTableConstraint> oldForeignKeys,
            List<DBTableConstraint> newForeignKeys, DBObjectEditor<DBTableConstraint> editor,
            SqlBuilder sqlBuilder) {

        for (DBTableConstraint newConstraint : newForeignKeys) {
            boolean isEqual = false;
            for (DBTableConstraint oldConstraint : oldForeignKeys) {
                if (Objects.deepEquals(newConstraint, oldConstraint)) {
                    isEqual = true;
                    break;
                }
            }
            if (isEqual) {
                continue;
            }
            // 创建外键约束的时候，不指定名称创建，防止同一个 database 下跟源表的外键名重复
            newConstraint.setName(StringUtils.EMPTY);
            sqlBuilder.append(editor.generateCreateObjectDDL(newConstraint));
        }
        for (DBTableConstraint oldConstraint : oldForeignKeys) {
            boolean isEqual = false;
            for (DBTableConstraint newConstraint : newForeignKeys) {
                if (Objects.deepEquals(newConstraint, oldConstraint)) {
                    isEqual = true;
                    break;
                }
            }
            if (isEqual) {
                continue;
            }
            sqlBuilder.append(editor.generateDropObjectDDL(oldConstraint));
        }
    }

}
