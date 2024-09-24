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

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class BaseAffectedRowsExceedLimit implements SqlCheckRule {

    private final Long maxSqlAffectedRows;
    private final JdbcOperations jdbcOperations;
    private final DialectType dialectType;

    public BaseAffectedRowsExceedLimit(@NonNull Long maxSqlAffectedRows, DialectType dialectType,
            JdbcOperations jdbcOperations) {
        this.dialectType = dialectType;
        this.jdbcOperations = jdbcOperations;
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
            long affectedRows = getStatementAffectedRows(statement, this.jdbcOperations, this.maxSqlAffectedRows);
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

    public abstract long getStatementAffectedRows(Statement statement,
            JdbcOperations jdbcOperations, Long maxSqlAffectedRows) throws Exception;

}
