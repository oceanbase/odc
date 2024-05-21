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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTree;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.SqlConsoleRuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule.RuleViolation;
import com.oceanbase.odc.service.regulation.ruleset.model.SqlConsoleRules;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.model.SqlTuplesWithViolation;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.dbbrowser.parser.result.BasicResult;
import com.oceanbase.tools.sqlparser.SyntaxErrorException;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/7/17 15:37
 * @Description: []
 */
@Slf4j
@Component
public class SqlConsoleInterceptor extends BaseTimeConsumingInterceptor {

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private SqlConsoleRuleService sqlConsoleRuleService;

    public final static String NEED_SQL_CONSOLE_CHECK = "NEED_SQL_CONSOLE_CHECK";
    public final static String SQL_CONSOLE_INTERCEPTED = "SQL_CONSOLE_INTERCEPTED";

    @Override
    public boolean doPreHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) {
        Map<String, Object> ctx = context.getContextMap();
        boolean sqlConsoleIntercepted = handle(request, response, session, ctx);
        ctx.put(SQL_CONSOLE_INTERCEPTED, sqlConsoleIntercepted);
        if (Objects.nonNull(ctx.get(SqlCheckInterceptor.SQL_CHECK_INTERCEPTED))) {
            return sqlConsoleIntercepted && (Boolean) ctx.get(SqlCheckInterceptor.SQL_CHECK_INTERCEPTED);
        } else {
            return true;
        }
    }

    private boolean handle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull Map<String, Object> context) {
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        if (Objects.isNull(ruleSetId) || isIndividualTeam()) {
            return true;
        }
        if (Objects.equals(Boolean.FALSE, context.get(NEED_SQL_CONSOLE_CHECK))) {
            return true;
        }

        Optional<Integer> queryLimit = sqlConsoleRuleService.getProperties(ruleSetId, SqlConsoleRules.MAX_RETURN_ROWS,
                session.getDialectType(), Integer.class);
        queryLimit.ifPresent(limit -> {
            if (Objects.isNull(request.getQueryLimit()) || request.getQueryLimit() > limit) {
                request.setQueryLimit(limit);
            }
        });
        AtomicBoolean allowExecute = new AtomicBoolean(true);

        List<SqlTuple> sqlTuples = response.getSqls().stream().map(SqlTuplesWithViolation::getSqlTuple)
                .collect(Collectors.toList());
        Optional<Integer> maxSqlSize = sqlConsoleRuleService.getProperties(ruleSetId, SqlConsoleRules.MAX_EXECUTE_SQLS,
                session.getDialectType(), Integer.class);
        AtomicReference<Rule> maxSqlSizeRule = new AtomicReference<>();
        if (maxSqlSize.isPresent()) {
            if (sqlTuples.size() > maxSqlSize.get()) {
                ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.MAX_EXECUTE_SQLS.getRuleName())
                        .ifPresent(rule -> {
                            maxSqlSizeRule.set(new Rule());
                            RuleViolation violation = new RuleViolation();
                            violation.setLevel(rule.getLevel());
                            violation.setLocalizedMessage(SqlConsoleRules.MAX_EXECUTE_SQLS
                                    .getLocalizedMessage(new Object[] {rule.getProperties()
                                            .get(rule.getMetadata().getPropertyMetadatas().get(0).getName())
                                            .toString()}));
                            maxSqlSizeRule.get().setViolation(violation);
                        });
                allowExecute.set(false);
            }
        }
        Map<String, BasicResult> sqlId2BasicResult = new HashMap<>();
        sqlTuples.forEach(sql -> sqlId2BasicResult.putIfAbsent(
                sql.getSqlId(), determineSqlType(sql, session.getDialectType())));

        boolean forbiddenToCreatePl =
                sqlConsoleRuleService.isForbidden(SqlConsoleRules.NOT_ALLOWED_CREATE_PL, session);
        Optional<List<String>> allowSqlTypesOpt = sqlConsoleRuleService.getListProperties(ruleSetId,
                SqlConsoleRules.ALLOW_SQL_TYPES, session.getDialectType(), String.class);

        for (int i = 0; i < response.getSqls().size(); i++) {
            SqlTuplesWithViolation item = response.getSqls().get(i);
            List<Rule> violatedRules = item.getViolatedRules();
            if (Objects.nonNull(maxSqlSizeRule.get()) && i == maxSqlSize.get()) {
                if (Objects.nonNull(maxSqlSizeRule.get().getViolation())) {
                    maxSqlSizeRule.get().getViolation().setOffset(item.getSqlTuple().getOffset());
                    maxSqlSizeRule.get().getViolation().setStart(0);
                    maxSqlSizeRule.get().getViolation().setStop(item.getSqlTuple().getOriginalSql().length());
                    violatedRules.add(maxSqlSizeRule.get());
                }
            }
            BasicResult parseResult = sqlId2BasicResult.get(item.getSqlTuple().getSqlId());
            if (parseResult.isPlDdl() && forbiddenToCreatePl) {
                ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.NOT_ALLOWED_CREATE_PL.getRuleName())
                        .ifPresent(rule -> {
                            Rule violationRule = new Rule();
                            RuleViolation violation = new RuleViolation();
                            violation.setLevel(rule.getLevel());
                            violation.setLocalizedMessage(
                                    SqlConsoleRules.NOT_ALLOWED_CREATE_PL.getLocalizedMessage(null));
                            violation.setOffset(item.getSqlTuple().getOffset());
                            violation.setStart(0);
                            violation.setStop(item.getSqlTuple().getOriginalSql().length());
                            violationRule.setViolation(violation);
                            violatedRules.add(violationRule);
                        });
                allowExecute.set(false);
            }
            if (allowSqlTypesOpt.isPresent()) {
                /**
                 * skip syntax error
                 */
                if (Objects.nonNull(parseResult.getSyntaxError()) && parseResult.getSyntaxError()) {
                    continue;
                }
                if (!allowSqlTypesOpt.get().contains(parseResult.getSqlType().name())) {
                    ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.ALLOW_SQL_TYPES.getRuleName())
                            .ifPresent(rule -> {
                                Rule violationRule = new Rule();
                                RuleViolation violation = new RuleViolation();
                                violation.setLevel(rule.getLevel());
                                violation.setLocalizedMessage(SqlConsoleRules.ALLOW_SQL_TYPES
                                        .getLocalizedMessage(new Object[] {rule.getProperties()
                                                .get(rule.getMetadata().getPropertyMetadatas().get(0).getName())
                                                .toString()}));
                                violation.setOffset(item.getSqlTuple().getOffset());
                                violation.setStart(0);
                                violation.setStop(item.getSqlTuple().getOriginalSql().length());
                                violationRule.setLevel(rule.getLevel());
                                violationRule.setViolation(violation);
                                violatedRules.add(violationRule);
                            });
                    allowExecute.set(false);
                }
            }
        }
        return allowExecute.get();
    }

    @Override
    protected String getExecuteStageName() {
        return SqlExecuteStages.SQL_CONSOLE_RULE;
    }

    @Override
    public void doAfterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull AsyncExecuteContext context) {
        if (response.getStatus() != SqlExecuteStatus.SUCCESS) {
            return;
        }
        if (isIndividualTeam()) {
            response.setAllowExport(true);
            return;
        }
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        if (Objects.isNull(ruleSetId) || isIndividualTeam()) {
            return;
        }
        if (sqlConsoleRuleService.isForbidden(SqlConsoleRules.NOT_ALLOWED_EDIT_RESULTSET, session)) {
            if (Objects.nonNull(response.getResultSetMetaData())) {
                response.getResultSetMetaData().setEditable(false);
            }
        }
        response.setAllowExport(
                !sqlConsoleRuleService.isForbidden(SqlConsoleRules.NOT_ALLOWED_EXPORT_RESULTSET, session));
    }

    private boolean isIndividualTeam() {
        return authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL;
    }

    private BasicResult determineSqlType(@NonNull SqlTuple sqlTuple, @NonNull DialectType dialectType) {
        BasicResult basicResult;
        try {
            AbstractSyntaxTree ast = sqlTuple.getAst();
            if (ast == null) {
                sqlTuple.initAst(AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0));
                ast = sqlTuple.getAst();
            }
            basicResult = ast.getParseResult();
        } catch (Exception e) {
            basicResult = new BasicResult(SqlType.UNKNOWN);
            if (e instanceof SyntaxErrorException) {
                basicResult.setSyntaxError(true);
            }
        }
        if (Objects.isNull(basicResult.getSqlType()) || basicResult.getSqlType() == SqlType.UNKNOWN) {
            basicResult.setSqlType(SqlType.OTHERS);
        }
        return basicResult;
    }

    @Override
    public int getOrder() {
        return 2;
    }

}
