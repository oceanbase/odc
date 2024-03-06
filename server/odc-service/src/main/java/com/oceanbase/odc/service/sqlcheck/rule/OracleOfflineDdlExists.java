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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.parser.DropStatement;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.truncate.TruncateTable;

import lombok.NonNull;

/**
 * {@link OracleOfflineDdlExists}
 *
 * @author yh263208
 * @date 2024-03-05 21:12
 * @since ODC_release_4.2.4
 * @ref https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000000252800
 */
public class OracleOfflineDdlExists extends MySQLOfflineDdlExists {

    public OracleOfflineDdlExists(@NonNull JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.OFFLINE_SCHEMA_CHANGE_EXISTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            CreateTable createTable = getTable(alterTable.getSchema(), alterTable.getTableName(), context);
            return alterTable.getAlterTableActions().stream().flatMap(action -> {
                List<CheckViolation> violations = new ArrayList<>();
                violations.addAll(changeColumnToAutoIncrement(statement, createTable, action));
                violations.addAll(changeColumnToPK(statement, action));
                violations.addAll(dropColumn(statement, action));
                violations.addAll(addOrDropPK(statement, action));
                violations.addAll(modifyPartition(statement, action));
                violations.addAll(dropPartition(statement, action));
                violations.addAll(truncatePartition(statement, action));
                return violations.stream();
            }).collect(Collectors.toList());
        } else if (statement instanceof TruncateTable) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    statement, getType(), new Object[] {}));
        } else if (statement instanceof DropStatement) {
            DropStatement dropStatement = (DropStatement) statement;
            if ("TABLE".equals(dropStatement.getObjectType())) {
                return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                        statement, getType(), new Object[] {}));
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Collections.singletonList(DialectType.OB_ORACLE);
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

}
