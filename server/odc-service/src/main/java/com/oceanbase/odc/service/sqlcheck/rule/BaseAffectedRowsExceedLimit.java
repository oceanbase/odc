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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

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
         *    The following is the result set returned by the sql plan for the new version ob, whose version number is 4.3.4
         *
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
         *
         *  The following is the result set returned by the sql plan for the new version ob, whose version number is 3.2.4.6
         *
         *  obclient(root@mysql)[zijia]> explain  delete from ids;
         * +------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
         * | Query Plan                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
         * +------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
         * | ====================================
         * |ID|OPERATOR   |NAME|EST. ROWS|COST|
         * ------------------------------------
         * |0 |DELETE     |    |11       |57  |
         * |1 | TABLE SCAN|ids |11       |46  |
         * ====================================
         *
         * Outputs & filters:
         * -------------------------------------
         *   0 - output(nil), filter(nil), table_columns([{ids: ({ids: (ids.__pk_increment, ids.id)})}])
         *   1 - output([ids.__pk_increment], [ids.id]), filter(nil),
         *       access([ids.__pk_increment], [ids.id]), partitions(p0)
         *  |
         * +------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
         * 1 row in set (0.002 sec)
         *
         * Differences between the two versions of ob mysql are as follows:
         * 1. The number of rows returned in the result set is different. The new version returns multiple rows, and the old version returns single row.
         * 2. The name of the estimated number of affected rows are different. The name of new version is “EST.ROWS", and that of the old version is “EST. ROWS”
         *
         * </pre>
         *
         *
         */
        String explainSql = "EXPLAIN " + originalSql;
        List<String> queryResults = jdbcOperations.query(explainSql, (rs, rowNum) -> rs.getString("Query Plan"));
        return getOBAndOracleAffectRowsFromResult(queryResults, this::containsAffectRowsColumnForOB,
                this::isAffectRowsColumnForOB);
    }

    protected long getOBAndOracleAffectRowsFromResult(List<String> queryResults,
            Predicate<String> containsAffectRowsColumn,
            Predicate<String> isAffectRowsColumn) {
        if (queryResults.size() == 1) {
            queryResults = Arrays.asList(queryResults.get(0).split("\\r?\\n"));
        }
        long estRowsValue = -1;
        int estRowsIndex = -1;

        for (int rowNum = 0; rowNum < queryResults.size(); rowNum++) {
            String resultRow = queryResults.get(rowNum).trim();
            if (estRowsIndex == -1 && containsAffectRowsColumn.test(resultRow)) {
                estRowsIndex = getEstRowsIndex(resultRow, isAffectRowsColumn);
                continue;
            }

            if (estRowsIndex != -1) {
                estRowsValue = getEstRowsValue(resultRow, estRowsIndex);
                if (estRowsValue != -1) {
                    return estRowsValue;
                }
            }
        }
        return estRowsValue;
    }

    private int getEstRowsIndex(String headerRow, Predicate<String> isAffectRowsColumn) {
        String[] columns = headerRow.split("\\|");
        for (int i = 0; i < columns.length; i++) {
            if (isAffectRowsColumn.test(columns[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private long getEstRowsValue(String resultRow, int columnIndex) {
        String[] values = resultRow.split("\\|");
        if (values.length > columnIndex) {
            String value = values[columnIndex].trim();
            long estRowsValue = parseLong(value);
            if (estRowsValue != 0) {
                return estRowsValue;
            }
        }
        return -1;
    }


    private boolean containsAffectRowsColumnForOB(String row) {
        return row.contains("EST.ROWS") || row.contains("EST. ROWS");
    }

    private boolean isAffectRowsColumnForOB(String column) {
        return column.trim().equals("EST.ROWS") || column.trim().equals("EST. ROWS");
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public abstract long getStatementAffectedRows(Statement statement) throws Exception;

}
