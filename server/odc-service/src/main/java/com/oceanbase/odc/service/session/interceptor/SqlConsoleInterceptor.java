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

    /**
     * 处理 SQL 异步执行请求
     *
     * @param request  SQL 异步执行请求
     * @param response SQL 异步执行响应
     * @param session  数据库连接会话
     * @param context  上下文信息
     * @return 是否允许执行
     */
    private boolean handle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
        @NonNull ConnectionSession session, @NonNull Map<String, Object> context) {
        // 获取规则集 ID
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        // 如果规则集 ID 为空或者是个人团队，则直接返回 true
        if (Objects.isNull(ruleSetId) || isIndividualTeam()) {
            return true;
        }
        // 如果不需要 SQL 控制台检查，则直接返回 true
        if (Objects.equals(Boolean.FALSE, context.get(NEED_SQL_CONSOLE_CHECK))) {
            return true;
        }

        // 获取最大返回行数限制
        Optional<Integer> queryLimit = sqlConsoleRuleService.getProperties(ruleSetId, SqlConsoleRules.MAX_RETURN_ROWS,
            session.getDialectType(), Integer.class);
        queryLimit.ifPresent(limit -> {
            if (Objects.isNull(request.getQueryLimit()) || request.getQueryLimit() > limit) {
                request.setQueryLimit(limit);
            }
        });
        AtomicBoolean allowExecute = new AtomicBoolean(true);

        // 获取 SQL 元组列表
        List<SqlTuple> sqlTuples = response.getSqls().stream().map(SqlTuplesWithViolation::getSqlTuple)
            .collect(Collectors.toList());
        // 获取最大执行 SQL 数量限制
        Optional<Integer> maxSqlSize = sqlConsoleRuleService.getProperties(ruleSetId, SqlConsoleRules.MAX_EXECUTE_SQLS,
            session.getDialectType(), Integer.class);
        AtomicReference<Rule> maxSqlSizeRule = new AtomicReference<>();
        if (maxSqlSize.isPresent()) {
            // 如果 SQL 元组数量超过最大执行 SQL 数量限制，则添加违反规则
            if (sqlTuples.size() > maxSqlSize.get()) {
                ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.MAX_EXECUTE_SQLS.getRuleName())
                    .ifPresent(rule -> {
                        maxSqlSizeRule.set(new Rule());
                        RuleViolation violation = new RuleViolation();
                        violation.setLevel(rule.getLevel());
                        violation.setLocalizedMessage(SqlConsoleRules.MAX_EXECUTE_SQLS
                            .getLocalizedMessage(new Object[] {rule.getProperties()
                                                                   .get(rule.getMetadata().getPropertyMetadatas().get(0)
                                                                       .getName())
                                                                   .toString()}));
                        maxSqlSizeRule.get().setViolation(violation);
                    });
                allowExecute.set(false);
            }
        }
        Map<String, BasicResult> sqlId2BasicResult = new HashMap<>();
        // 确定 SQL 类型
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
                // 判断当前 SQL 类型是否被允许执行
                if (!allowSqlTypesOpt.get().contains(parseResult.getSqlType().name())) {
                    // 获取规则服务中指定规则集 ID 和规则名的规则
                    ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.ALLOW_SQL_TYPES.getRuleName())
                        .ifPresent(rule -> {
                            Rule violationRule = new Rule();
                            RuleViolation violation = new RuleViolation();
                            violation.setLevel(rule.getLevel());
                            // 设置违规信息的本地化消息
                            violation.setLocalizedMessage(SqlConsoleRules.ALLOW_SQL_TYPES
                                .getLocalizedMessage(new Object[] {rule.getProperties()
                                                                       .get(rule.getMetadata().getPropertyMetadatas()
                                                                           .get(0).getName())
                                                                       .toString()}));
                            violation.setOffset(item.getSqlTuple().getOffset());
                            violation.setStart(0);
                            violation.setStop(item.getSqlTuple().getOriginalSql().length());
                            violationRule.setLevel(rule.getLevel());
                            violationRule.setViolation(violation);
                            violatedRules.add(violationRule);
                        });
                    // 不允许执行
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
        // 如果 SQL 执行状态不是成功，则直接返回
        if (response.getStatus() != SqlExecuteStatus.SUCCESS) {
            return;
        }
        // 如果当前用户属于个人团队，则允许导出结果集，并直接返回
        if (isIndividualTeam()) {
            response.setAllowExport(true);
            return;
        }
        // 获取规则集 ID
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        // 如果规则集 ID 为空或当前用户属于个人团队，则直接返回
        if (Objects.isNull(ruleSetId) || isIndividualTeam()) {
            return;
        }
        // 如果禁止编辑结果集，则设置结果集元数据为不可编辑
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

    /**
     * 确定SQL类型
     *
     * @param sqlTuple    SQL元组
     * @param dialectType 方言类型
     * @return BasicResult基本结果
     */
    private BasicResult determineSqlType(@NonNull SqlTuple sqlTuple, @NonNull DialectType dialectType) {
        BasicResult basicResult;
        try {
            // 获取抽象语法树
            AbstractSyntaxTree ast = sqlTuple.getAst();
            // 如果抽象语法树为空，则初始化抽象语法树
            if (ast == null) {
                sqlTuple.initAst(AbstractSyntaxTreeFactories.getAstFactory(dialectType, 0));
                ast = sqlTuple.getAst();
            }
            // 获取解析结果
            basicResult = ast.getParseResult();
        } catch (Exception e) {
            // 如果出现异常，则创建一个未知类型的BasicResult对象
            basicResult = new BasicResult(SqlType.UNKNOWN);
            // 如果异常是语法错误异常，则设置语法错误标志
            if (e instanceof SyntaxErrorException) {
                basicResult.setSyntaxError(true);
            }
        }
        // 如果BasicResult对象的SQL类型为空或未知，则将其设置为其他类型
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
