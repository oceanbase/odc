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

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBForeignKeyModifyRule;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @Author: Lebie
 * @Date: 2022/7/20 上午12:20
 * @Description: []
 */
public abstract class DBTableConstraintEditor implements DBObjectEditor<DBTableConstraint> {

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public String generateCreateObjectDDL(@NotNull DBTableConstraint constraint) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(constraint));
        if (constraint.getType() == DBConstraintType.PRIMARY_KEY && constraint.getName().isEmpty()) {
            sqlBuilder.append(" ADD PRIMARY KEY ");
        } else {
            sqlBuilder.append(" ADD CONSTRAINT ")
                    .identifierIf(constraint.getName(), StringUtils.isNotEmpty(constraint.getName())).space();
            appendConstraintType(constraint, sqlBuilder);
        }
        appendConstraintColumns(constraint, sqlBuilder);
        appendConstraintOptions(constraint, sqlBuilder);
        return sqlBuilder.toString().trim() + ";\n";
    }

    protected void appendConstraintOptions(DBTableConstraint constraint, SqlBuilder sqlBuilder) {
        if (Objects.nonNull(constraint.getMatchType())) {
            sqlBuilder.append(" MATCH ").append(constraint.getMatchType().name());
        }
        if (Objects.nonNull(constraint.getOnDeleteRule())) {
            /**
             * if no action, we should not append ON DELETE statement
             */
            if (constraint.getOnDeleteRule() != DBForeignKeyModifyRule.NO_ACTION) {
                sqlBuilder.append(" ON DELETE ").append(constraint.getOnDeleteRule().getValue());
            }
        }
    }

    protected void appendConstraintColumns(DBTableConstraint constraint, SqlBuilder sqlBuilder) {
        if (constraint.getType() == DBConstraintType.CHECK) {
            sqlBuilder.append("(").append(constraint.getCheckClause()).append(")").space();
            return;
        }
        List<String> columnNames = constraint.getColumnNames();
        if (Objects.isNull(columnNames)) {
            return;
        }
        sqlBuilder.append("(");
        boolean isFirstColumn = true;
        for (String columnName : columnNames) {
            if (!isFirstColumn) {
                sqlBuilder.append(", ");
            }
            isFirstColumn = false;
            sqlBuilder.identifier(columnName);
        }
        sqlBuilder.append(")");
        if (constraint.getType() == DBConstraintType.FOREIGN_KEY) {
            sqlBuilder.append(" REFERENCES ").identifier(constraint.getReferenceSchemaName()).append(".")
                    .identifier(constraint.getReferenceTableName()).append(
                            " (");
            isFirstColumn = true;
            for (String refColumnName : constraint.getReferenceColumnNames()) {
                if (!isFirstColumn) {
                    sqlBuilder.append(", ");
                }
                isFirstColumn = false;
                sqlBuilder.identifier(refColumnName);
            }
            sqlBuilder.append(")");
        }
    }

    protected void appendConstraintType(DBTableConstraint constraint, SqlBuilder sqlBuilder) {
        sqlBuilder.append(constraint.getType().getValue()).space();
    }

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBTableConstraint constraint) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (Objects.isNull(constraint.getName())) {
            constraint.setName(StringUtils.EMPTY);
        }
        sqlBuilder.append("CONSTRAINT ")
                .identifierIf(constraint.getName(), StringUtils.isNotEmpty(constraint.getName())).space();
        appendConstraintType(constraint, sqlBuilder);
        appendConstraintColumns(constraint, sqlBuilder);
        appendConstraintOptions(constraint, sqlBuilder);
        return sqlBuilder.toString().trim();
    }

    @Override
    public String generateUpdateObjectDDL(@NotNull DBTableConstraint oldConstraint,
            @NotNull DBTableConstraint newConstraint) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (!Objects.equals(oldConstraint, newConstraint)) {
            String drop = generateDropObjectDDL(oldConstraint);
            sqlBuilder.append(drop)
                    .append(generateCreateObjectDDL(newConstraint));
            return sqlBuilder.toString();
        }
        if (!Objects.equals(oldConstraint.getEnabled(), newConstraint.getEnabled())) {
            sqlBuilder.append(generateEnableOrDisableConstraintDDL(newConstraint));
        }
        if (!StringUtils.equals(oldConstraint.getName(), newConstraint.getName())) {
            sqlBuilder.append(generateRenameObjectDDL(oldConstraint, newConstraint));
        }
        return sqlBuilder.toString();
    }

    protected String generateEnableOrDisableConstraintDDL(@NotNull DBTableConstraint newConstraint) {
        return "";
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBTableConstraint> oldConstraints,
            Collection<DBTableConstraint> newConstraints) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (CollectionUtils.isEmpty(oldConstraints)) {
            if (CollectionUtils.isNotEmpty(newConstraints)) {
                newConstraints
                        .forEach(constraint -> sqlBuilder.append(generateCreateObjectDDL(constraint)));
            }
            return sqlBuilder.toString();
        }
        if (CollectionUtils.isEmpty(newConstraints)) {
            if (CollectionUtils.isNotEmpty(oldConstraints)) {
                oldConstraints
                        .forEach(constraint -> sqlBuilder.append(generateDropObjectDDL(constraint)));
            }

            return sqlBuilder.toString();
        }
        Map<Integer, DBTableConstraint> position2OldConstraint = new HashMap<>();
        Map<Integer, DBTableConstraint> position2NewConstraint = new HashMap<>();

        oldConstraints.forEach(
                oldConstraint -> position2OldConstraint.put(oldConstraint.getOrdinalPosition(), oldConstraint));
        newConstraints.forEach(newConstraint -> {
            if (Objects.nonNull(newConstraint.getOrdinalPosition())) {
                position2NewConstraint.put(newConstraint.getOrdinalPosition(), newConstraint);
            }
        });
        for (DBTableConstraint newConstraint : newConstraints) {
            // ordinaryPosition is NULL means this is a new constraint
            if (Objects.isNull(newConstraint.getOrdinalPosition())) {
                sqlBuilder.append(generateCreateObjectDDL(newConstraint));
            } else if (position2OldConstraint.containsKey(newConstraint.getOrdinalPosition())) {
                // this is an existing constraint
                sqlBuilder.append(
                        generateUpdateObjectDDL(position2OldConstraint.get(newConstraint.getOrdinalPosition()),
                                newConstraint));
            }
        }
        for (DBTableConstraint oldConstraint : oldConstraints) {
            // means this constraint should be dropped
            if (!position2NewConstraint.containsKey(oldConstraint.getOrdinalPosition())) {
                sqlBuilder.append(generateDropObjectDDL(oldConstraint));
            }
        }
        return sqlBuilder.toString();
    }

    protected abstract SqlBuilder sqlBuilder();

    protected String getFullyQualifiedTableName(@NotNull DBTableConstraint constraint) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(constraint.getSchemaName())) {
            sqlBuilder.identifier(constraint.getSchemaName()).append(".");
        }
        if (StringUtils.isNotEmpty(constraint.getTableName())) {
            sqlBuilder.identifier(constraint.getTableName());
        }
        return sqlBuilder.toString();
    }

}
