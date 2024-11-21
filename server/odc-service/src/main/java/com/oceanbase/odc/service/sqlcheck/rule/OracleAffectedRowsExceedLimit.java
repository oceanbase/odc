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
            if (this.jdbcOperations == null) {
                throw new IllegalStateException("JdbcOperations is null, please check your connection");
            }
            switch (this.dialectType) {
                case ORACLE:
                    affectedRows = getOracleAffectedRows(originalSql, this.jdbcOperations);
                    break;
                case OB_ORACLE:
                    affectedRows = getOBAffectedRows(originalSql, this.jdbcOperations);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported dialect type: " + this.dialectType);
            }
        }
        return affectedRows;
    }

    private long getOracleAffectedRows(String originalSql, JdbcOperations jdbcOperations) {
        /**
         * <pre>
         *     Plan hash value: 775918519
         *
         * -------------------------------------------------------------------
         * | Id  | Operation          | Name | Rows  | Cost (%CPU)| Time     |
         * -------------------------------------------------------------------
         * |   0 | DELETE STATEMENT   |      |    11 |     3   (0)| 00:00:01 |
         * |   1 |  DELETE            | T1   |       |            |          |
         * |   2 |   TABLE ACCESS FULL| T1   |    11 |     3   (0)| 00:00:01 |
         * -------------------------------------------------------------------
         *
         * Query Block Name / Object Alias (identified by operation id):
         * -------------------------------------------------------------
         *
         *    1 - DEL$1
         *    2 - DEL$1 / T1@DEL$1
         *
         * Column Projection Information (identified by operation id):
         * -----------------------------------------------------------
         *
         *    2 - "T1".ROWID[ROWID,10]
         * </pre>
         */
        String SetPlanSql =
                "EXPLAIN PLAN SET STATEMENT_ID = '" + ODC_TEMP_EXPLAIN_STATEMENT_ID + "' FOR " + originalSql;
        jdbcOperations.execute(SetPlanSql);
        String getPlanSql =
                "SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY('PLAN_TABLE', '" + ODC_TEMP_EXPLAIN_STATEMENT_ID + "', 'ALL'))";
        List<String> queryResults = jdbcOperations.query(getPlanSql, (rs, rowNum) -> rs.getString("PLAN_TABLE_OUTPUT"));
        long estRowsValue = 0;
        for (int rowNum = 2; rowNum < queryResults.size(); rowNum++) {
            String resultRow = queryResults.get(rowNum);
            estRowsValue = getEstRowsValue(resultRow);
            if (estRowsValue != 0) {
                break;
            }
        }
        return estRowsValue;
    }

}
