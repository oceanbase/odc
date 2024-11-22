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
         * <pre>
         *     obclient> explain delete from T1 where 1=1;
         * +-------------------------------------------------------+
         * | Query Plan                                            |
         * +-------------------------------------------------------+
         * | =================================================     |
         * | |ID|OPERATOR         |NAME|EST.ROWS|EST.TIME(us)|     |
         * | -------------------------------------------------     |
         * | |0 |DELETE           |    |2       |21          |     |
         * | |1 |└─TABLE FULL SCAN|t1  |2       |5           |     |
         * | =================================================     |
         * | Outputs & filters:                                    |
         * | -------------------------------------                 |
         * |   0 - output(nil), filter(nil)                        |
         * |       table_columns([{t1: ({t1: (t1.id)})}])          |
         * |   1 - output([t1.id]), filter(nil), rowset=16         |
         * |       access([t1.id]), partitions(p0)                 |
         * |       is_index_back=false, is_global_index=false,     |
         * |       range_key([t1.id]), range(MIN ; MAX)always true |
         * +-------------------------------------------------------+
         * 14 rows in set (0.00 sec)
         * </pre>
         */
        String explainSql = "EXPLAIN " + originalSql;
        List<String> queryResults = jdbcOperations.query(explainSql, (rs, rowNum) -> rs.getString("Query Plan"));
        return getOBAndOracleAffectRowsFromResult(queryResults);
    }

    public long getOBAndOracleAffectRowsFromResult(List<String> queryResults) {
        long estRowsValue = 0;
        for (int rowNum = 0; rowNum < queryResults.size(); rowNum++) {
            String resultRow = queryResults.get(rowNum);
            estRowsValue = getEstRowsValue(resultRow);
            if (estRowsValue != 0) {
                break;
            }
        }
        return estRowsValue;
    }

    private long getEstRowsValue(String singleRow) {
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
