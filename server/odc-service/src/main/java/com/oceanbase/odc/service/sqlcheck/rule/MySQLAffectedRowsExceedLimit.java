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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.update.Update;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link MySQLAffectedRowsExceedLimit}
 *
 * @author yiminpeng
 * @version 1.0
 * @date 2024-08-01 18:18
 */
@Slf4j
public class MySQLAffectedRowsExceedLimit implements SqlCheckRule {

    private final Long maxSqlAffectedRows;

    private final JdbcOperations jdbcOperations;

    private final DialectType dialectType;

    public MySQLAffectedRowsExceedLimit(@NonNull Long maxSqlAffectedRows, DialectType dialectType,
            JdbcOperations jdbcOperations) {
        this.maxSqlAffectedRows = maxSqlAffectedRows <= 0 ? 0 : maxSqlAffectedRows;
        this.jdbcOperations = jdbcOperations;
        this.dialectType = dialectType;
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

        if (statement instanceof Update || statement instanceof Delete || statement instanceof Insert) {
            long affectedRows = 0;
            String explainSql = "EXPLAIN " + statement.getText();
            try {
                if (jdbcOperations == null) {
                    log.warn("jdbcOperations is null, please check your connection");
                    return Collections.emptyList();
                } else {
                    switch (dialectType) {
                        case MYSQL:
                            affectedRows = (statement instanceof Insert)
                                    ? getMySqlAffectedRowsByCount((Insert)statement)
                                    : getMySqlAffectedRowsByExplain(explainSql, jdbcOperations);
                            break;
                        case OB_MYSQL:
                            affectedRows = getOBMySqlAffectedRows(explainSql, jdbcOperations);
                            break;
                        default:
                            log.warn("Unsupported dialect type: {}", dialectType);
                            break;
                    }
                }
            } catch (Exception e) {
                log.warn("Error in calling getAffectedRows method", e);
                affectedRows = -1;
            }

            if (affectedRows > maxSqlAffectedRows) {
                return Collections.singletonList(SqlCheckUtil
                        .buildViolation(statement.getText(), statement, getType(),
                                new Object[] {maxSqlAffectedRows, affectedRows}));
            } else {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Get supported database types
     */
    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL);
    }

    /**
     * MySQL checks the count of value list. Process mode: case1: For INSERT INTO ... VALUES(...)
     * statements, ODC checks the count of value list. case2: For INSERT INTO ... SELECT ... statements,
     * ODC runs EXPLAIN statements to get affected rows.
     *
     * @param insertStatement target sql
     * @return affected rows
     */
    private long getMySqlAffectedRowsByCount(Insert insertStatement) {

        List<Expression> insertClass = insertStatement.getTableInsert().get(0)
            .getValues().get(0);
        String expressionType = insertClass.get(0)
            .getClass().getSimpleName();
        // case1: For INSERT INTO ... VALUES(...)
        if ("ConstExpression".equals(expressionType)) {
            return insertClass.size();
        }
        // case2: For INSERT INTO ... SELECT ...
        if ("Select".equals(expressionType)) {
            try {
                return getMySqlAffectedRowsByExplain(insertStatement.getText(), jdbcOperations);
            } catch (Exception e) {
                throw new RuntimeException("error: " + e.getMessage() + ", SQL: " + insertStatement.getText());
            }
        }
        log.warn("Unsupported insert sql syntax: " + insertStatement.getText());
        return -1;
    }

    /**
     * MySQL execute 'explain' statement
     *
     * @param explainSql target sql
     * @param jdbc jdbc Object
     * @return affected rows
     */
    private long getMySqlAffectedRowsByExplain(String explainSql, JdbcOperations jdbc) {

        try {
            List<Long> resultSet = jdbc.query(explainSql,
                    (rs, rowNum) -> rs.getLong("rows"));

            Long firstNonNullResult = resultSet.stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            return firstNonNullResult != null ? firstNonNullResult : 0;

        } catch (Exception e) {
            throw new RuntimeException("error: " + e.getMessage() + ", SQL: " + explainSql);
        }
    }

    /**
     * OBMySQL execute 'explain' statement
     *
     * @param explainSql target sql
     * @param jdbc jdbc Object
     * @return affected rows
     */
    private long getOBMySqlAffectedRows(String explainSql, JdbcOperations jdbc) {

        /**
         * <pre>
         *
         *     explain result (json):
         *
         *     ==================================================    --rowNum = 1
         *     |ID|OPERATOR          |NAME|EST.ROWS|EST.TIME(us)|    --rowNum = 2
         *     0 |DISTRIBUTED UPDATE|    |1       |37          |     --rowNum = 3
         *     1 |└─TABLE GET       |user|1       |5           |     --rowNum = 4
         *     ==================================================    --rowNum = 5
         *     ...
         *
         * </pre>
         */
        try {
            AtomicBoolean ifFindAffectedRow = new AtomicBoolean(false);
            List<String> queryResults = jdbc.query(explainSql, (rs, rowNum) -> rs.getString("Query Plan"));
            List<Long> resultSet = new ArrayList<>();
            for (int rowNum = 0; rowNum < queryResults.size(); rowNum++) {
                String resultRow = queryResults.get(rowNum);
                if (!ifFindAffectedRow.get() && rowNum > 2) {
                    // Find the first non-null value in the column 'EST.ROWS'
                    long estRowsValue = getEstRowsValue(resultRow);
                    if (estRowsValue != 0) {
                        ifFindAffectedRow.set(true);
                        resultSet.add(estRowsValue);
                    }
                }
                resultSet.add(null);
            }

            Long firstNonNullResult = resultSet.stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            return firstNonNullResult != null ? firstNonNullResult : 0;

        } catch (Exception e) {
            throw new RuntimeException("error: " + e.getMessage() + ", SQL: " + explainSql);
        }
    }

    /**
     * parse explain result set
     *
     * @param singleRow row
     * @return affected rows
     */
    private long getEstRowsValue(String singleRow) {
        String[] parts = singleRow.split("\\|");
        if (parts.length > 4) {
            String value = parts[4].trim();
            return Long.parseLong(value);
        }
        return 0;
    }

}
