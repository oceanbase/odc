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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.update.Update;

import lombok.NonNull;

/**
 * {@link MySQLAffectedRows}
 *
 * @author yiminpeng
 * @version 1.0
 * @date 2024-08-01 18:18
 */
public class MySQLAffectedRows implements SqlCheckRule {

    private final Integer maxSQLAffectedRows;

    private final JdbcOperations jdbcOperations;

    public MySQLAffectedRows(@NonNull Integer maxSQLAffectedRows, JdbcOperations jdbcOperations) {
        this.maxSQLAffectedRows = maxSQLAffectedRows <= 0 ? 0 : maxSQLAffectedRows;
        this.jdbcOperations = jdbcOperations;
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

        if (statement instanceof Insert || statement instanceof Update || statement instanceof Delete) {
            if (maxSQLAffectedRows == 0) {
                return Collections.emptyList();
            }
            String originSql = statement.getText();
            long affectedRows = executeExplain(originSql, jdbcOperations);
            if (affectedRows > maxSQLAffectedRows) {
                return Collections.singletonList(SqlCheckUtil
                        .buildViolation(statement.getText(), statement, getType(), new Object[] {}));
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
     * execute 'explain' statement
     *
     * @param originSql target sql
     * @param jdbc jdbc Object
     * @return affected rows
     */
    private long executeExplain(String originSql, JdbcOperations jdbc) {

        if (originSql == null || jdbc == null) {
            return 0;
        }
        String explainSql = "EXPLAIN " + originSql;

        try {
            // parse rows tag: if true, stop parse
            AtomicBoolean ifFindValue = new AtomicBoolean(false);
            List<Long> resultSet = jdbc.query(explainSql, (rs, rowNum) -> {
                String singleRow = rs.getString(1);

                if (!ifFindValue.get() && rowNum > 2) {
                    long affectedRows = getAffectedRows(singleRow);
                    // first non-null value is the column 'EST.ROWS'
                    if (affectedRows != 0) {
                        ifFindValue.set(true);
                        return affectedRows;
                    }
                }
                return null;
            });
            Long firstNonNullResult = resultSet.stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            return firstNonNullResult != null ? firstNonNullResult : 0;

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute sql: " + explainSql + ", error: " + e.getMessage());
        }
    }

    /**
     * parse explain result set
     *
     * @param singleRow row
     * @return affected rows
     */
    private long getAffectedRows(String singleRow) {
        String[] parts = singleRow.split("\\|");
        if (parts.length > 4) {
            String value = parts[4].trim();
            return Long.parseLong(value);
        }
        return 0;
    }
}
