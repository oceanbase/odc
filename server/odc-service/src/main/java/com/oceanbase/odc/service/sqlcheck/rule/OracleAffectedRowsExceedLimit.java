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
public class OracleAffectedRowsExceedLimit extends BaseAffectedRowsExceedLimit {
    public static final String ODC_TEMP_EXPLAIN_STATEMENT_ID = "ODC_TEMP_EXPLAIN_STATEMENT_ID";

    private final JdbcOperations jdbcOperations;
    private final DialectType dialectType;

    public OracleAffectedRowsExceedLimit(@NonNull Long maxSqlAffectedRows, DialectType dialectType,
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
        return Arrays.asList(DialectType.OB_ORACLE, DialectType.ORACLE);
    }

    /**
     * Base method implemented by Oracle types
     */
    @Override
    public long getStatementAffectedRows(Statement statement) {
        long affectedRows = 0;
        if (statement instanceof Update || statement instanceof Delete || statement instanceof Insert) {
            String originalSql = statement.getText();
            try {
                if (this.jdbcOperations == null) {
                    log.warn("JdbcOperations is null, please check your connection");
                    return -1;
                } else {
                    switch (this.dialectType) {
                        case ORACLE:
                            affectedRows = getOracleAffectedRows(originalSql, this.jdbcOperations);
                            break;
                        case OB_ORACLE:
                            affectedRows = getOBOracleAffectedRows(originalSql, this.jdbcOperations);
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

    private long getOracleAffectedRows(String originalSql, JdbcOperations jdbcOperations) {
        /**
         * Plan hash value: 775918519
         *
         * ------------------------------------------------------------------- | Id | Operation | Name |
         * Rows | Cost (%CPU)| Time | ------------------------------------------------------------------- |
         * 0 | DELETE STATEMENT | | 9 | 3 (0)| 00:00:01 | | 1 | DELETE | T1 | | | | | 2 | TABLE ACCESS FULL|
         * T1 | 9 | 3 (0)| 00:00:01 | -------------------------------------------------------------------
         *
         * Query Block Name / Object Alias (identified by operation id):
         * -------------------------------------------------------------
         *
         * 1 - DEL$1 2 - DEL$1 / T1@DEL$1
         *
         * Column Projection Information (identified by operation id):
         * -----------------------------------------------------------
         *
         * 2 - "T1".ROWID[ROWID,10]
         */
        String SetPlanSql =
                "EXPLAIN PLAN SET STATEMENT_ID = '" + ODC_TEMP_EXPLAIN_STATEMENT_ID + "' FOR " + originalSql;
        jdbcOperations.execute(SetPlanSql);
        String getPlanSql =
                "SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY('PLAN_TABLE', '" + ODC_TEMP_EXPLAIN_STATEMENT_ID + "', 'ALL'))";
        List<String> queryResults = jdbcOperations.query(getPlanSql, (rs, rowNum) -> rs.getString("PLAN_TABLE_OUTPUT"));
        long estRowsValue = 0;
        for (int rowNum = 5; rowNum < queryResults.size(); rowNum++) {
            String resultRow = queryResults.get(rowNum);
            estRowsValue = getEstRowsValue(resultRow);
            if (estRowsValue != 0) {
                break;
            }
        }
        return estRowsValue;
    }



    /**
     * OBOracle execute 'explain' statement
     *
     * @param originalSql target sql
     * @param jdbcOperations jdbc Object
     * @return affected rows
     */
    private long getOBOracleAffectedRows(String originalSql, JdbcOperations jdbcOperations) {
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
        long estRowsValue = 0;
        for (int rowNum = 3; rowNum < queryResults.size(); rowNum++) {
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
}
