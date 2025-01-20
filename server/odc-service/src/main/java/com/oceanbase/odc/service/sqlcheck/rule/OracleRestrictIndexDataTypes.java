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
package com.oceanbase.odc.service.sqlcheck.rule;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * {@link OracleRestrictIndexDataTypes}
 *
 * @author yh263208
 * @date 2023-06-28 17:50
 * @since ODC_release_4.2.0
 */
public class OracleRestrictIndexDataTypes extends BaseRestrictIndexDataTypes {

    public OracleRestrictIndexDataTypes(JdbcOperations jdbcOperations, @NonNull Set<String> allowedTypeNames) {
        super(jdbcOperations, allowedTypeNames);
    }

    @Override
    protected String unquoteIdentifier(String identifier) {
        return SqlCheckUtil.unquoteOracleIdentifier(identifier);
    }

    @Override
    protected CreateTable getTableFromRemote(JdbcOperations jdbcOperations, String schema, String tableName) {
        String sql = "SHOW CREATE TABLE " + (schema == null ? tableName : (schema + "." + tableName));
        try {
            String ddl = jdbcOperations.queryForObject(sql, (rs, rowNum) -> rs.getString(2));
            if (ddl == null) {
                return null;
            }
            Statement statement = new OBOracleSQLParser().parse(new StringReader(ddl));
            return statement instanceof CreateTable ? (CreateTable) statement : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Collections.singletonList(DialectType.OB_ORACLE);
    }

}
