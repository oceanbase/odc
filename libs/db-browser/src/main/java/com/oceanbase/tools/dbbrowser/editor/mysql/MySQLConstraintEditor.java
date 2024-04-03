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

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @Author: Lebie
 * @Date: 2022/7/21 下午5:58
 * @Description: []
 */
public class MySQLConstraintEditor extends DBTableConstraintEditor {

    @Override
    protected SqlBuilder sqlBuilder() {
        return new MySQLSqlBuilder();
    }

    @Override
    protected void appendConstraintOptions(DBTableConstraint constraint, SqlBuilder sqlBuilder) {
        super.appendConstraintOptions(constraint, sqlBuilder);
        if (Objects.nonNull(constraint.getOnUpdateRule())) {
            sqlBuilder.append(" ON UPDATE ").append(constraint.getOnUpdateRule().getValue());
        }
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBTableConstraint constraint) {
        SqlBuilder sqlBuilder = sqlBuilder();
        DBConstraintType type = constraint.getType();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(constraint)).append(" DROP ");
        if (type == DBConstraintType.PRIMARY_KEY) {
            sqlBuilder.append("PRIMARY KEY");
        } else if (type == DBConstraintType.CHECK) {
            sqlBuilder.append("CONSTRAINT ").identifier(constraint.getName());
        } else if (type == DBConstraintType.FOREIGN_KEY) {
            sqlBuilder.append("FOREIGN KEY ").identifierIf(constraint.getName(),
                    StringUtils.isNotEmpty(constraint.getName()));
        } else {
            sqlBuilder.append("KEY ").identifier(constraint.getName());
        }
        return sqlBuilder.toString().trim() + ";\n";
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBTableConstraint oldConstraint,
            @NotNull DBTableConstraint newConstraint) {
        SqlBuilder sqlBuilder = sqlBuilder();
        // 外键不支持重命名，只能删除后重建
        if (newConstraint.getType() == DBConstraintType.FOREIGN_KEY) {
            String drop = generateDropObjectDDL(oldConstraint);
            sqlBuilder.append(drop)
                    .append(generateCreateObjectDDL(newConstraint));
        } else {
            sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(oldConstraint))
                    .append(" RENAME KEY ").identifier(oldConstraint.getName()).append(" TO ")
                    .identifier(newConstraint.getName()).append(";").line();
        }
        return sqlBuilder.toString();
    }

}
