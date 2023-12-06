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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTree;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlSplitter;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.parser.SyntaxErrorStatement;
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
        List<OffsetString> sqls = null;
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

    public List<CheckViolation> check(@NonNull List<OffsetString> sqls, SqlCheckContext context) {
        return doCheck(sqls, context, s -> {
            try {
                return new Pair<>(s.getOffset(), doParse(s.getStr()));
            } catch (Exception e) {
                if (e instanceof SyntaxErrorException) {
                    return new Pair<>(s.getOffset(), new SyntaxErrorStatement(s.getStr(), (SyntaxErrorException) e));
                }
            }
            return null;
        });
    }

    public List<CheckViolation> check(SqlCheckContext context, @NonNull List<SqlTuple> sqls) {
        return doCheck(sqls, context, s -> {
            try {
                AbstractSyntaxTree ast = s.getAst();
                if (ast != null) {
                    return new Pair<>(s.getOffset(), ast.getStatement());
                }
                return new Pair<>(s.getOffset(), doParse(s.getOriginalSql()));
            } catch (Exception e) {
                if (e instanceof SyntaxErrorException) {
                    return new Pair<>(s.getOffset(),
                            new SyntaxErrorStatement(s.getOriginalSql(), (SyntaxErrorException) e));
                }
            }
            return null;
        });
    }

    private <T> List<CheckViolation> doCheck(List<T> inputs, SqlCheckContext context,
            Function<T, Pair<Integer, Statement>> function) {
        final SqlCheckContext checkContext;
        if (context != null) {
            checkContext = context;
            checkContext.combine(new SqlCheckContext());
        } else {
            checkContext = new SqlCheckContext();
        }
        List<Pair<Integer, Statement>> stmts = inputs.stream()
                .map(function)
                .filter(stmt -> Objects.nonNull(stmt) && Objects.nonNull(stmt.right))
                .collect(Collectors.toList());
        if (checkContext.currentStmtIndex == null) {
            checkContext.currentStmtIndex = 0L;
        }
        if (checkContext.totalStmtCount == null) {
            checkContext.totalStmtCount = (long) stmts.size();
        }
        return stmts.stream().flatMap(holder -> {
            List<CheckViolation> violations = doCheck(holder.right, checkContext);
            violations.stream().forEach(v -> {
                if (Objects.isNull(v.getOffset())) {
                    v.setOffset(holder.left);
                }
            });
            checkContext.addCheckViolation(holder.right, holder.left, violations);
            checkContext.currentStmtIndex++;
            return violations.stream();
        }).collect(Collectors.toList());
    }

    private List<OffsetString> splitByCommentProcessor(String sqlScript) {
        SqlCommentProcessor processor = new SqlCommentProcessor(dialectType, true, true);
        processor.setDelimiter(delimiter);
        StringBuffer buffer = new StringBuffer();
        List<OffsetString> sqls = processor.split(buffer, sqlScript);
        String bufferStr = buffer.toString();
        if (bufferStr.trim().length() != 0) {
            if (sqls.size() == 0) {
                sqls.add(new OffsetString(0, bufferStr));
            } else {
                sqls.add(new OffsetString(
                        sqls.get(sqls.size() - 1).getOffset() + sqls.get(sqls.size() - 1).getStr().length(),
                        bufferStr));
            }
        }
        return sqls;
    }

    protected abstract Statement doParse(String sql);

    protected abstract List<CheckViolation> doCheck(Statement statement, SqlCheckContext context);

}
