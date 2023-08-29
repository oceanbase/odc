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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlSplitter;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;
import com.oceanbase.tools.sqlparser.oracle.PlSqlLexer;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;

/**
 * {@link BaseSqlChecker}
 *
 * @author yh263208
 * @date 2022-12-14 13:56
 * @since ODC_release_4.1.0
 * @see SqlChecker
 */
abstract class BaseSqlChecker implements SqlChecker {

    private final static String DEFAULT_DELIMITER = ";";
    private final String delimiter;
    protected final DialectType dialectType;

    public BaseSqlChecker(@NonNull DialectType dialectType, String delimiter) {
        this.dialectType = dialectType;
        this.delimiter = delimiter == null ? DEFAULT_DELIMITER : delimiter;
    }

    @Override
    public List<CheckViolation> check(@NonNull String sqlScript) {
        List<String> sqls;
        if (dialectType.isMysql()) {
            sqls = splitByCommentProcessor(sqlScript);
        } else if (dialectType == DialectType.OB_ORACLE) {
            if (DEFAULT_DELIMITER.equals(this.delimiter)) {
                // 如果用户没有改 delimiter 就用现成的分句逻辑
                SqlSplitter sqlSplitter = new SqlSplitter(PlSqlLexer.class, this.delimiter);
                sqlSplitter.setRemoveCommentPrefix(false);
                sqls = sqlSplitter.split(sqlScript);
            } else {
                // 如果用户改变了 delimiter，为了避免分句的潜在问题需要使用新的分句逻辑
                sqls = splitByCommentProcessor(sqlScript);
            }
        } else {
            throw new IllegalStateException("Unknown dialect type, " + dialectType);
        }
        return check(sqls, null);
    }

    public List<CheckViolation> check(@NonNull List<String> sqls, SqlCheckContext context) {
        final SqlCheckContext checkContext;
        if (context != null) {
            checkContext = context;
            checkContext.combine(new SqlCheckContext());
        } else {
            checkContext = new SqlCheckContext();
        }
        List<SqlHolder> sqlHolders = sqls.stream().map(s -> {
            try {
                return new SqlHolder(s, doParse(s), null);
            } catch (Exception e) {
                if (e instanceof SyntaxErrorException) {
                    return new SqlHolder(s, null, (SyntaxErrorException) e);
                }
            }
            return new SqlHolder(s, null, null);
        }).filter(s -> s.statement != null || s.exception != null).collect(Collectors.toList());
        checkContext.totalStmtCount = sqlHolders.stream().filter(s -> s.exception == null).count();
        checkContext.currentStmtIndex = 0L;
        return sqlHolders.stream().flatMap(holder -> {
            List<CheckViolation> violations = new ArrayList<>();
            if (holder.statement != null) {
                violations = doCheck(holder.statement, checkContext);
                checkContext.addCheckViolation(holder.statement, violations);
                checkContext.currentStmtIndex++;
            } else if (holder.exception != null) {
                violations.add(new CheckViolation(holder.sql, 1, 0, 0, holder.sql.length() - 1,
                        SqlCheckRuleType.SYNTAX_ERROR, new Object[] {holder.exception.getMessage()}));
            }
            return violations.stream();
        }).collect(Collectors.toList());
    }

    private List<String> splitByCommentProcessor(String sqlScript) {
        SqlCommentProcessor processor = new SqlCommentProcessor(dialectType, true, true);
        processor.setDelimiter(delimiter);
        StringBuffer buffer = new StringBuffer();
        List<String> sqls = processor.split(buffer, sqlScript);
        String bufferStr = buffer.toString();
        if (bufferStr.trim().length() != 0) {
            sqls.add(bufferStr);
        }
        return sqls;
    }

    protected abstract Statement doParse(String sql);

    protected abstract List<CheckViolation> doCheck(Statement statement, SqlCheckContext context);

    private static class SqlHolder {

        private final SyntaxErrorException exception;
        private final String sql;
        private final Statement statement;

        public SqlHolder(@NonNull String sql, Statement statement, SyntaxErrorException exception) {
            this.statement = statement;
            this.sql = sql;
            this.exception = exception;
        }
    }

}
