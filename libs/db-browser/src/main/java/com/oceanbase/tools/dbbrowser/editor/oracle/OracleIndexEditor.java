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

import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @Author: Lebie
 * @Date: 2022/7/19 下午6:59
 * @Description: []
 */
public class OracleIndexEditor extends DBTableIndexEditor {

    @Override
    public String generateRenameObjectDDL(@NotNull DBTableIndex oldIndex,
            @NotNull DBTableIndex newIndex) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER INDEX ").identifier(oldIndex.getName())
                .append(" RENAME TO ").identifier(newIndex.getName());
        return sqlBuilder.toString();
    }

    @Override
    protected void appendIndexColumnModifiers(DBTableIndex index, SqlBuilder sqlBuilder) {}

    @Override
    protected void appendIndexOptions(DBTableIndex index, SqlBuilder sqlBuilder) {
        if (Objects.nonNull(index.getGlobal())) {
            sqlBuilder.append(index.getGlobal() ? " GLOBAL " : " LOCAL ");
        }
        if (Objects.nonNull(index.getVisible()) && !index.getVisible()) {
            sqlBuilder.append(" INVISIBLE ");
        }
    }

    @Override
    public String generateDropObjectDDL(@NotNull DBTableIndex index) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("DROP INDEX ").identifier(index.getName());
        return sqlBuilder.toString();
    }

    @Override
    protected SqlBuilder sqlBuilder() {
        return new OracleSqlBuilder();
    }

    @Override
    public boolean editable() {
        return true;
    }

}
