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
package com.oceanbase.odc.service.sqlcheck;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.rule.SqlCheckRules;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;

/**
 * {@link DefaultSqlChecker}
 *
 * @author yh263208
 * @date 2022-12-14 15:12
 * @since ODC_release_4.1.0
 */
public class DefaultSqlChecker extends BaseSqlChecker {

    private final List<SqlCheckRule> rules;
    private final AbstractSyntaxTreeFactory factory;

    public DefaultSqlChecker(@NonNull DialectType dialectType,
            String delimiter, @NonNull List<SqlCheckRule> rules) {
        super(dialectType, delimiter);
        this.rules = rules;
        this.factory = AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0);
    }

    public DefaultSqlChecker(@NonNull ConnectionSession session, String delimiter) {
        this(session.getDialectType(), delimiter, SqlCheckRules.getAllDefaultRules(
                session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY), session.getDialectType()));
    }

    public DefaultSqlChecker(JdbcOperations jdbcOperations, @NonNull DialectType dialectType, String delimiter) {
        this(dialectType, delimiter, SqlCheckRules.getAllDefaultRules(jdbcOperations, dialectType));
    }

    @Override
    protected Statement doParse(String sql) {
        return this.factory == null ? null : this.factory.buildAst(sql).getStatement();
    }

    @Override
    protected List<CheckViolation> doCheck(Statement statement, SqlCheckContext context) {
        return this.rules.stream().filter(r -> r.getSupportsDialectTypes().contains(dialectType))
                .flatMap(rule -> rule.check(statement, context).stream()).collect(Collectors.toList());
    }

}
