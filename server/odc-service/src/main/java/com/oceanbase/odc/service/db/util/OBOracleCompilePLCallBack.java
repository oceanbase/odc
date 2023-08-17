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
package com.oceanbase.odc.service.db.util;

import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.PLObjectErrMsgUtils;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

public class OBOracleCompilePLCallBack implements StatementCallback<String> {

    private final DBPLObjectIdentity plIdentity;
    private final JdbcOperations jdbcOperations;

    public OBOracleCompilePLCallBack(@NonNull DBPLObjectIdentity plIdentity,
            @NonNull JdbcOperations jdbcOperations) {
        this.plIdentity = plIdentity;
        Validate.notEmpty(plIdentity.getName(), "PL name can not be blank");
        Validate.notEmpty(plIdentity.getSchemaName(), "PL schema name can not be blank");
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public String doInStatement(Statement stmt) throws SQLException, DataAccessException {
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append("ALTER ")
                .append(plIdentity.getType().getName()).space()
                .identifier(plIdentity.getName()).append(" COMPILE");
        stmt.execute(sqlBuilder.toString());
        if (stmt.getWarnings() == null) {
            return null;
        }
        String msg = PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations, plIdentity.getSchemaName(),
                plIdentity.getType().getName(), plIdentity.getName());
        if (StringUtils.isNotBlank(msg)) {
            return msg;
        }
        return stmt.getWarnings().getMessage();
    }

}
