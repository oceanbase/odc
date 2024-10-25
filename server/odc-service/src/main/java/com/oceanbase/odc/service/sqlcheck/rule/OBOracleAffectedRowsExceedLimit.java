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
import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.update.Update;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:{@link OracleAffectedRowsExceedLimit}
 * 
 * @author: zijia.cj
 * @date: 2024/10/24 13:06
 * @since: 4.3.3
 */
@Slf4j
public class OBOracleAffectedRowsExceedLimit extends BaseAffectedRowsExceedLimit {
    private final JdbcOperations jdbcOperations;
    private final DialectType dialectType;

    public OBOracleAffectedRowsExceedLimit(@NonNull Long maxSqlAffectedRows, DialectType dialectType,
            JdbcOperations jdbcOperations) {
        super(maxSqlAffectedRows);
        this.jdbcOperations = jdbcOperations;
        this.dialectType = dialectType;
    }

    /**
     * Get supported database types
     */
    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_ORACLE);
    }

    /**
     * Base method implemented by Oracle types
     */
    @Override
    public long getStatementAffectedRows(Statement statement) {
        long affectedRows = 0;
        if (statement instanceof Update || statement instanceof Delete || statement instanceof Insert) {
            String explainSql = "EXPLAIN " + statement.getText();
            try {
                if (this.jdbcOperations == null) {
                    log.warn("JdbcOperations is null, please check your connection");
                    return -1;
                } else {
                    switch (this.dialectType) {
                        case OB_ORACLE:
                            affectedRows = getOBOracleAffectedRows(explainSql, this.jdbcOperations);
                            break;
                        default:
                            log.warn("Unsupported dialect type: {}", this.dialectType);
                            break;
                    }
                }
            } catch (Exception e) {
                log.warn("Error in calling getAffectedRows method", e);
                affectedRows = -1;
            }
        }
        return affectedRows;
    }

    /**
     * OBOracle execute 'explain' statement
     *
     * @param explainSql target sql
     * @param jdbc jdbc Object
     * @return affected rows
     */
    private long getOBOracleAffectedRows(String explainSql, JdbcOperations jdbc) {
        /**
         * obclient> explain delete T1 where 1=1;
         * +----------------------------------------------------------------------+ | Query Plan |
         * +----------------------------------------------------------------------+ |
         * ===================================================== | | |ID|OPERATOR
         * |NAME|EST.ROWS|EST.TIME(us)| | | ----------------------------------------------------- | | |0
         * |EXCHANGE IN REMOTE | |10 |82 | | | |1 |+-EXCHANGE OUT REMOTE| |10 |75 | | | |2 | +-DELETE | |10
         * |60 | | | |3 | +-TABLE FULL SCAN|T1 |10 |3 | | |
         * ===================================================== | | Outputs & filters: | |
         * ------------------------------------- | | 0 - output(nil), filter(nil) | | 1 - output(nil),
         * filter(nil) | | 2 - output(nil), filter(nil) | | table_columns([{T1: ({T1: (T1.__pk_increment,
         * T1.NAME)})}]) | | 3 - output([T1.__pk_increment], [T1.NAME]), filter(nil), rowset=16 | |
         * access([T1.__pk_increment], [T1.NAME]), partitions(p0) | | is_index_back=false,
         * is_global_index=false, | | range_key([T1.__pk_increment]), range(MIN ; MAX)always true |
         * +----------------------------------------------------------------------+
         */
        try {
            List<String> queryResults = jdbc.query(explainSql, (rs, rowNum) -> rs.getString("Query Plan"));
            long estRowsValue = 0;
            for (int rowNum = 3; rowNum < queryResults.size(); rowNum++) {
                String resultRow = queryResults.get(rowNum);
                estRowsValue = getEstRowsValue(resultRow);
                if (estRowsValue != 0) {
                    break;
                }
            }
            return estRowsValue;
        } catch (Exception e) {
            log.warn("OBOracle mode: Error executing " + explainSql + ": ", e);
            return -1;
        }
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
}
