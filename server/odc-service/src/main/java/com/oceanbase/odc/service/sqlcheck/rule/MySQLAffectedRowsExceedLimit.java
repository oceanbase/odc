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
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.insert.InsertTable;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
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
public class MySQLAffectedRowsExceedLimit extends BaseAffectedRowsExceedLimit {

    private final JdbcOperations jdbcOperations;
    private final DialectType dialectType;

    public MySQLAffectedRowsExceedLimit(@NonNull Long maxSqlAffectedRows, DialectType dialectType,
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
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL);
    }

    /**
     * Base method implemented by MySQL types
     */
    @Override
    public long getStatementAffectedRows(Statement statement) {
        long affectedRows = -1;
        if (statement instanceof Update || statement instanceof Delete || statement instanceof Insert) {
            String explainSql = "EXPLAIN " + statement.getText();
            if (this.jdbcOperations == null) {
                throw new IllegalStateException("JdbcOperations is null, please check your connection");
            }
            switch (this.dialectType) {
                case MYSQL:
                    affectedRows = (statement instanceof Insert)
                            ? getMySqlAffectedRowsByCount((Insert) statement)
                            : getMySqlAffectedRowsByExplain(explainSql, this.jdbcOperations);
                    break;
                case OB_MYSQL:
                    affectedRows = getOBAffectedRows(statement.getText(), this.jdbcOperations);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported dialect type: " + this.dialectType);
            }
        }
        return affectedRows;
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
        List<InsertTable> insertTableList = insertStatement.getTableInsert();
        if (insertTableList.isEmpty()) {
            log.warn("InsertTableList is empty, please check your sql");
            return -1;
        }
        InsertTable insertTable = insertTableList.get(0);
        if (insertTable == null || insertTable.getValues() == null) {
            log.warn("InsertTable is null or values is null, please check your sql");
            return -1;
        }
        List<List<Expression>> values = insertTable.getValues();
        if (CollectionUtils.isNotEmpty(values)) {
            if (values.size() == 1 && values.get(0).size() == 1) {
                Expression value = values.get(0).get(0);
                if ((value instanceof Select) || (value instanceof SelectBody)) {
                    return getMySqlAffectedRowsByExplain(insertStatement.getText(), this.jdbcOperations);
                } else {
                    return 1;
                }
            } else {
                return values.size();
            }
        } else if (CollectionUtils.isNotEmpty(insertTable.getSetColumns())) {
            return 1;
        }
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
            log.warn("MySQL mode: Error in execute " + explainSql + " failed. ", e);
            return -1;
        }
    }

}
