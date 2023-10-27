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
package com.oceanbase.odc.service.session.interceptor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.TraceStage;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.tools.sqlparser.FastFailErrorListener;
import com.oceanbase.tools.sqlparser.FastFailErrorStrategy;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleExpressionFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Scope_or_scope_aliasContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Set_system_parameter_clauseContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.StmtContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Var_and_valContext;
import com.oceanbase.tools.sqlparser.oboracle.OBParserBaseVisitor;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link NlsFormatInterceptor}
 *
 * @author yh263208
 * @date 2023-07-04 14:24
 * @since ODC_release_4.2.0
 */
@Slf4j
@Component
public class NlsFormatInterceptor implements SqlExecuteInterceptor {

    @Override
    public void afterCompletion(@NonNull SqlExecuteResult response,
            @NonNull ConnectionSession session, @NonNull Map<String, Object> context) {
        DialectType dialect = session.getDialectType();
        if (response.getStatus() != SqlExecuteStatus.SUCCESS || dialect != DialectType.OB_ORACLE) {
            return;
        }
        try (TraceStage stage = response.getTraceWatch().start(SqlExecuteStages.SET_NLS_FORMAT)) {
            List<String> sqls = SqlCommentProcessor.removeSqlComments(response.getOriginSql(), ";", dialect, false);
            if (CollectionUtils.isEmpty(sqls) || sqls.size() != 1) {
                log.warn("Sql is empty or multi sql exists, sql={}", response.getOriginSql());
                return;
            }
            String sql = sqls.get(0).trim();
            if (!StringUtils.startsWithIgnoreCase(sql, "set") && !startWithAlterSession(sql)) {
                return;
            }
            getVariableAssigns(sql).stream().filter(VariableAssign::isSession).forEach(v -> {
                String value = getNlsFormatValue(v.getValue());
                if (value == null) {
                    return;
                }
                if ("nls_timestamp_format".equalsIgnoreCase(v.getName())) {
                    ConnectionSessionUtil.setNlsTimestampFormat(session, value);
                } else if ("nls_date_format".equalsIgnoreCase(v.getName())) {
                    ConnectionSessionUtil.setNlsDateFormat(session, value);
                } else if ("nls_timestamp_tz_format".equalsIgnoreCase(v.getName())) {
                    ConnectionSessionUtil.setNlsTimestampTZFormat(session, value);
                }
            });
        } catch (IOException e) {
            log.warn("Failed to close trace stage", e);
        }
    }

    private boolean startWithAlterSession(String sql) {
        char[] chars = "altersessionset".toCharArray();
        String tmpSql = sql.toLowerCase();
        int length = tmpSql.length();
        int j = 0;
        for (int i = 0; i < length && j < chars.length; i++) {
            char s = tmpSql.charAt(i);
            if (s == ' ' || s == '\t' || s == '\n' || s == '\r') {
                continue;
            }
            if (s != chars[j]) {
                return false;
            }
            j++;
        }
        return j >= chars.length;
    }

    private String getNlsFormatValue(Expression value) {
        if (!(value instanceof ConstExpression)) {
            return null;
        }
        ConstExpression e = (ConstExpression) value;
        return StringUtils.unwrap(e.getExprConst(), "'");
    }

    private List<VariableAssign> getVariableAssigns(String sql) {
        try {
            OBLexer lexer = new OBLexer(CharStreams.fromString(sql));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new FastFailErrorListener());
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            OBParser parser = new OBParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new FastFailErrorListener());
            parser.setErrorHandler(new FastFailErrorStrategy());
            StmtContext stmtContext = parser.stmt();
            if (stmtContext.variable_set_stmt() != null) {
                return stmtContext.variable_set_stmt().var_and_val_list().var_and_val().stream()
                        .map(c -> new NlsFormatVariableVisitor().visit(c))
                        .filter(Objects::nonNull).collect(Collectors.toList());
            } else if (stmtContext.alter_session_stmt() != null) {
                return stmtContext.alter_session_stmt().alter_session_set_clause()
                        .set_system_parameter_clause_list().set_system_parameter_clause().stream()
                        .map(c -> new NlsFormatVariableVisitor().visit(c)).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to set nls format", e);
        }
        return Collections.emptyList();
    }

    @Override
    public int getOrder() {
        return 4;
    }

    @Getter
    @Setter
    private static class VariableAssign {

        private boolean session;
        private boolean global;
        private final String name;
        private final Expression value;

        public VariableAssign(@NonNull String name, Expression value) {
            this.name = StringUtils.unwrap(name, "\"");
            this.value = value;
        }
    }

    private static class NlsFormatVariableVisitor extends OBParserBaseVisitor<VariableAssign> {

        @Override
        public VariableAssign visitVar_and_val(Var_and_valContext ctx) {
            if (ctx.scope_or_scope_alias() == null) {
                return null;
            }
            Expression value = null;
            if (ctx.set_expr_or_default().bit_expr() != null) {
                value = new OracleExpressionFactory(ctx.set_expr_or_default().bit_expr()).generate();
            }
            VariableAssign assign = new VariableAssign(ctx.column_name().getText(), value);
            Scope_or_scope_aliasContext scope = ctx.scope_or_scope_alias();
            if (scope.GLOBAL() != null || scope.GLOBAL_ALIAS() != null) {
                assign.setGlobal(true);
            } else if (scope.SESSION() != null || scope.SESSION_ALIAS() != null) {
                assign.setSession(true);
            }
            return assign;
        }

        @Override
        public VariableAssign visitSet_system_parameter_clause(Set_system_parameter_clauseContext ctx) {
            Expression value = new OracleExpressionFactory(ctx.bit_expr()).generate();
            VariableAssign assign = new VariableAssign(ctx.var_name().getText(), value);
            assign.setSession(true);
            return assign;
        }
    }

}
