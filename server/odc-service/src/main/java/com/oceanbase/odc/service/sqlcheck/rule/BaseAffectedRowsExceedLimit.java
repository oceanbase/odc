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

import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;

public abstract class BaseAffectedRowsExceedLimit implements SqlCheckRule {

    private final Long maxSqlAffectedRows;

    public BaseAffectedRowsExceedLimit(@NonNull Long maxSqlAffectedRows) {
        this.maxSqlAffectedRows = maxSqlAffectedRows < 0 ? Long.MAX_VALUE : maxSqlAffectedRows;
    }

    /**
     * Get the rule type
     */
    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_SQL_AFFECTED_ROWS;
    }

    /**
     * Execution rule check
     */
    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        try {
            long affectedRows = getStatementAffectedRows(statement);
            if (affectedRows >= 0) {
                if (affectedRows > maxSqlAffectedRows) {
                    return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                            statement, getType(), new Object[] {maxSqlAffectedRows, affectedRows}));
                }
                return Collections.emptyList();
            }
        } catch (Exception e) {
            // eat the exception
        }
        return Collections.emptyList();
    }

    /**
     * OB execute 'explain' statement
     *
     * @param originalSql target sql
     * @param jdbcOperations jdbc Object
     * @return affected rows
     */
    public long getOBAffectedRows(String originalSql, JdbcOperations jdbcOperations) {
        /**
         * obclient [SYS]> explain select * from all_users;
         * +-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
         * | Query Plan |
         * +-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
         * | ================================================ | | |ID|OPERATOR |NAME|EST.ROWS|EST.TIME(us)|
         * | | ------------------------------------------------ | | |0 |TABLE RANGE SCAN|B |18 |5 | | |
         * ================================================ | | Outputs & filters: | |
         * ------------------------------------- | | 0 - output([B.USER_NAME], [B.USER_ID],
         * [cast(B.GMT_CREATE, DATE(0, 0))], [cast('NO', VARCHAR2(3 BYTE))], [cast('N', VARCHAR2(1 BYTE))],
         * [cast('NO', VARCHAR2(3 | | BYTE))], [cast('USING_NLS_COMP', VARCHAR2(100 BYTE))], [cast('NO',
         * VARCHAR2(3 BYTE))], [cast('NO', VARCHAR2(3 BYTE))]), filter([B.TYPE = 0], [B.TENANT_ID | | =
         * EFFECTIVE_TENANT_ID()]) | | access([B.TENANT_ID], [B.USER_ID], [B.TYPE], [B.USER_NAME],
         * [B.GMT_CREATE]), partitions(p0) | | is_index_back=false, is_global_index=false,
         * filter_before_indexback[false,false], | | range_key([B.TENANT_ID], [B.USER_ID]), range(1004,MIN ;
         * 1004,MAX), | | range_cond([B.TENANT_ID = EFFECTIVE_TENANT_ID()]) |
         * +-------------------------------------------------------------------------------------------------------------------------------------------------------------------+
         * 14 rows in set (0.007 sec)
         */
        String explainSql = "EXPLAIN " + originalSql;
        List<String> queryResults = jdbcOperations.query(explainSql, (rs, rowNum) -> rs.getString("Query Plan"));
        long maxEstRowsValue = 0;
        for (int rowNum = 3; rowNum < queryResults.size(); rowNum++) {
            String resultRow = queryResults.get(rowNum);
            long estRowsValue = getEstRowsValue(resultRow);
            if (estRowsValue > maxEstRowsValue) {
                maxEstRowsValue = estRowsValue;
            }
        }
        return maxEstRowsValue;
    }

    public long getEstRowsValue(String singleRow) {
        String[] parts = singleRow.split("\\|");
        if (parts.length > 5) {
            String value = parts[4].trim();
            return parseLong(value);
        }
        return 0;
    }

    /**
     * Safely parse a long value.
     *
     * @param value string to parse
     * @return parsed long or 0 if parsing fails
     */
    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public abstract long getStatementAffectedRows(Statement statement) throws Exception;

}
