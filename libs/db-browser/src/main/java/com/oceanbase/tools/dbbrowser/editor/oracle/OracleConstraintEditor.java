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

import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.model.DBConstraintDeferability;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @Author: Lebie
 * @Date: 2022/8/1 下午9:24
 * @Description: []
 */
public class OracleConstraintEditor extends DBTableConstraintEditor {

    @Override
    protected SqlBuilder sqlBuilder() {
        return new OracleSqlBuilder();
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBTableConstraint oldConstraint,
            @NotNull DBTableConstraint newConstraint) {
        SqlBuilder sqlBuilder = sqlBuilder();
        String drop = generateDropObjectDDL(oldConstraint);
        sqlBuilder.append(drop).append(";").line()
                .append(generateCreateObjectDDL(newConstraint)).append(";").line();
        return sqlBuilder.toString();
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBTableConstraint constraint) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(constraint))
                .append(" DROP CONSTRAINT ").identifier(constraint.getName());
        return sqlBuilder.toString();
    }

    @Override
    protected void appendConstraintOptions(DBTableConstraint constraint, SqlBuilder sqlBuilder) {
        super.appendConstraintOptions(constraint, sqlBuilder);
        if (Objects.nonNull(constraint.getDeferability())) {
            if (constraint.getDeferability() == DBConstraintDeferability.INITIALLY_DEFERRED) {
                sqlBuilder.append(" DEFERRABLE INITIALLY DEFERRED");
            } else if (constraint.getDeferability() == DBConstraintDeferability.INITIALLY_IMMEDIATE) {
                sqlBuilder.append(" DEFERRABLE INITIALLY IMMEDIATE");
            }
        }
        if (Objects.nonNull(constraint.getEnabled()) && !constraint.getEnabled()
                && constraint.getType() == DBConstraintType.FOREIGN_KEY) {
            sqlBuilder.append(" DISABLE ");
        }
    }

    @Override
    protected String generateEnableOrDisableConstraintDDL(@NotNull DBTableConstraint newConstraint) {
        if (newConstraint.getType() != DBConstraintType.FOREIGN_KEY) {
            return "";
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(newConstraint));
        if (newConstraint.getEnabled()) {
            sqlBuilder.append(" ENABLE CONSTRAINT ");
        } else {
            sqlBuilder.append(" DISABLE CONSTRAINT ");
        }
        sqlBuilder.identifier(newConstraint.getName()).append(";").line();
        return sqlBuilder.toString();
    }

}
