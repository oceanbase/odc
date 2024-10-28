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
package com.oceanbase.odc.service.sqlcheck.factory;

import java.util.Map;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRuleFactory;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLAffectedRowsExceedLimit;
import com.oceanbase.odc.service.sqlcheck.rule.OracleAffectedRowsExceedLimit;

import lombok.NonNull;

public class SqlAffectedRowsFactory implements SqlCheckRuleFactory {
    public static final long DEFAULT_MAX_SQL_AFFECTED_ROWS = 1000L;

    private final JdbcOperations jdbc;

    public SqlAffectedRowsFactory(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SqlCheckRuleType getSupportsType() {
        return SqlCheckRuleType.RESTRICT_SQL_AFFECTED_ROWS;
    }

    @Override
    public SqlCheckRule generate(@NonNull DialectType dialectType, Map<String, Object> parameters) {
        String key = getParameterNameKey("allowed-max-sql-affected-count");
        long maxSqlAffectedRows = DEFAULT_MAX_SQL_AFFECTED_ROWS;
        if (parameters != null && !parameters.isEmpty() && parameters.get(key) != null) {
            maxSqlAffectedRows = Long.valueOf(parameters.get(key).toString());
        }
        switch (dialectType) {
            case ORACLE:
            case OB_ORACLE:
                return new OracleAffectedRowsExceedLimit(maxSqlAffectedRows, dialectType,
                        jdbc);
            case MYSQL:
            case OB_MYSQL:
                return new MySQLAffectedRowsExceedLimit(maxSqlAffectedRows, dialectType,
                        jdbc);
            default:
                throw new IllegalArgumentException("Unsupported dialect type: " + dialectType);
        }
    }
}
