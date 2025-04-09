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

    protected long getOBAndOracleAffectRowsFromResult(List<String> queryResults) {
        // 如果查询结果只有一行，则按换行符拆分（老版本ob）
        if (queryResults.size() == 1) {
            queryResults = Arrays.asList(queryResults.get(0).split("\\r?\\n"));
        }

        long estRowsValue = -1;
        int estRowsIndex = -1;

        for (int rowNum = 0; rowNum < queryResults.size(); rowNum++) {
            String resultRow = queryResults.get(rowNum).trim();

            // 先定位到 EST.ROWS 所在行
            if (estRowsIndex == -1 && containsAffectRowsColumnName(resultRow)) {
                estRowsIndex = getEstRowsIndex(resultRow);
                continue;
            }

            // 从 EST.ROWS 所在行的下一行开始尝试获取对应位置的值
            if (estRowsIndex != -1) {
                // 尝试从数据行中获取 EST.ROWS 的值
                estRowsValue = getEstRowsValue(resultRow, estRowsIndex);
                if (estRowsValue != -1) {
                    return estRowsValue;
                }
            }
        }
        return estRowsValue;
    }

    private int getEstRowsIndex(String headerRow) {
        String[] columns = headerRow.split("\\|");
        for (int i = 0; i < columns.length; i++) {
            if (isAffectRowsColumnName(columns[i].trim())) {
                return i; // 返回列索引
            }
        }
        return -1; // 如果没有找到，返回 -1
    }

    private long getEstRowsValue(String resultRow, int columnIndex) {
        String[] values = resultRow.split("\\|");
        if (values.length > columnIndex) {
            String value = values[columnIndex].trim();
            long estRowsValue = parseLong(value);
            if (estRowsValue != -1) {
                return estRowsValue; // 返回获取到的值
            }
        }
        return -1; // 如果没有找到有效的值，返回 -1
    }

    // protected long getOBAndOracleAffectRowsFromResult(List<String> queryResults) {
    // if(queryResults.size()==1) {
    // queryResults = Arrays.asList(queryResults.get(0).split("\\r?\\n"));
    // }
    // long estRowsValue = -1;
    // int position = -1;
    // boolean startGetValue=false;
    // for (int rowNum = 0; rowNum < queryResults.size(); rowNum++) {
    // String resultRow = queryResults.get(rowNum);
    // // 先定位到 EST.ROWS 所在行
    // if(!startGetValue && containsAffectRowsColumnName(resultRow)){
    // // 获取 EST.ROWS 所在列的索引位置
    // String[] columns = resultRow.split("\\|");
    // for (int i = 0; i < columns.length; i++) {
    // if (isAffectRowsColumnName(columns[i])) {
    // position = i;
    // startGetValue=true;
    // break;
    // }
    // }
    // continue;
    // }
    // // 从EST.ROWS 所在行的下一行开始尝试获取与EST.ROWS对应位置的值
    // if(startGetValue){
    // String[] values = resultRow.split("\\|");
    // if (values.length > position) {
    // String value = values[position].trim();
    // estRowsValue = parseLong(value);
    // if(estRowsValue!=-1){
    // break;
    // }
    // }
    // }
    // }
    // return estRowsValue;
    // }

    protected boolean isAffectRowsColumnName(String column) {
        return column.trim().equals("EST.ROWS") || column.trim().equals("EST. ROWS");
    }

    protected boolean containsAffectRowsColumnName(String row) {
        return row.contains("EST.ROWS") || row.contains("EST. ROWS");
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
            return -1;
        }
    }

    public abstract long getStatementAffectedRows(Statement statement) throws Exception;

}
