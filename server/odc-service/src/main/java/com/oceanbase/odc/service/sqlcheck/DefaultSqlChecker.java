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

import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.parser.SqlCheckOBMySQLParser;
import com.oceanbase.odc.service.sqlcheck.parser.SqlCheckOBOracleParser;
import com.oceanbase.odc.service.sqlcheck.rule.SqlCheckRules;
import com.oceanbase.tools.sqlparser.FastFailErrorListener;
import com.oceanbase.tools.sqlparser.FastFailErrorStrategy;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;
import com.oceanbase.tools.sqlparser.obmysql.PLLexer;
import com.oceanbase.tools.sqlparser.obmysql.PLParser;
import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;
import com.oceanbase.tools.sqlparser.oracle.PlSqlParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.util.CaseChangingCharStream;

import lombok.NonNull;

/**
 * {@link DefaultSqlChecker}
 *
 * @author yh263208
 * @date 2022-12-14 15:12
 * @since ODC_release_4.1.0
 */
public class DefaultSqlChecker extends BaseSqlChecker {

    private final SQLParser sqlParser;
    private final List<SqlCheckRule> rules;

    public DefaultSqlChecker(@NonNull DialectType dialectType,
            String delimiter, @NonNull List<SqlCheckRule> rules) {
        super(dialectType, delimiter);
        this.sqlParser = dialectType == DialectType.OB_ORACLE
                ? new SqlCheckOBOracleParser()
                : new SqlCheckOBMySQLParser();
        this.rules = rules;
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
        try {
            return this.sqlParser.parse(new StringReader(sql));
        } catch (SyntaxErrorException e) {
            tryParsePl(sql);
            return null;
        }
    }

    @Override
    protected List<CheckViolation> doCheck(Statement statement, SqlCheckContext context) {
        return this.rules.stream().filter(r -> r.getSupportsDialectTypes().contains(dialectType))
                .flatMap(rule -> rule.check(statement, context).stream()).collect(Collectors.toList());
    }

    /**
     * 目前 ob 的 parser 在解析 pl 和 sql 是分开的，这导致 sql check 时会存在一个问题：如果用户送检的是一个 pl 的 ddl，那么此时的 sql parser
     * 就会报错语法错误。这里这个方法就是为了处理这种情况：当 parser 报语法错误的时候再用 pl 的 parser 解析一次，如果 pl 的 parser 也报错才认为有语法错误。
     */
    private void tryParsePl(String sql) {
        Lexer lexer;
        if (this.dialectType == DialectType.OB_ORACLE) {
            lexer = new PlSqlLexer(new CaseChangingCharStream(CharStreams.fromString(sql), true));
        } else {
            lexer = new PLLexer(CharStreams.fromString(sql));
        }
        lexer.removeErrorListeners();
        lexer.addErrorListener(new FastFailErrorListener());
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Parser parser;
        if (this.dialectType == DialectType.OB_ORACLE) {
            parser = new PlSqlParser(tokens);
        } else {
            parser = new PLParser(tokens);
        }
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setErrorHandler(new FastFailErrorStrategy());
        if (this.dialectType == DialectType.OB_ORACLE) {
            ((PlSqlParser) parser).sql_script();
        } else {
            ((PLParser) parser).stmt_block();
        }
    }

}
