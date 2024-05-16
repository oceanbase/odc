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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.sqlcheck.DefaultSqlChecker;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckService;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SqlCheckInterceptor}
 *
 * @author yh263208
 * @date 2023-06-08 11:15
 * @since ODC_release_4.2.0
 * @see SqlExecuteInterceptor
 */
@Slf4j
@Component
public class SqlCheckInterceptor extends BaseTimeConsumingInterceptor {

    public final static String NEED_SQL_CHECK_KEY = "NEED_SQL_CHECK";
    public final static String SQL_CHECK_INTERCEPTED = "SQL_CHECK_INTERCEPTED";
    private final static String SQL_CHECK_RESULT_KEY = "SQL_CHECK_RESULT";
    @Autowired
    private SqlCheckService sqlCheckService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Override
    public boolean doPreHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) {
        Map<String, Object> ctx = context.getContextMap();
        boolean sqlCheckIntercepted = handle(request, response, session, ctx);
        ctx.put(SQL_CHECK_INTERCEPTED, sqlCheckIntercepted);
        if (Objects.nonNull(ctx.get(SqlConsoleInterceptor.SQL_CONSOLE_INTERCEPTED))) {
            return sqlCheckIntercepted && (Boolean) ctx.get(SqlConsoleInterceptor.SQL_CONSOLE_INTERCEPTED);
        } else {
            return true;
        }
    }

    private boolean handle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull Map<String, Object> context) {
        if (this.authenticationFacade.currentUser().getOrganizationType() != OrganizationType.TEAM
                || Boolean.FALSE.equals(context.get(NEED_SQL_CHECK_KEY))) {
            // 个人组织下不检查
            return true;
        }
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        if (ruleSetId == null) {
            return true;
        }
        List<Rule> rules = this.ruleService.listAllFromCache(ruleSetId);
        List<SqlCheckRule> sqlCheckRules = this.sqlCheckService.getRules(rules, session);
        if (CollectionUtils.isEmpty(sqlCheckRules)) {
            return true;
        }
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(session.getDialectType(), null, sqlCheckRules);
        try {
            Map<Integer, List<CheckViolation>> offset2Violations = new HashMap<>();
            SqlCheckContext checkContext = new SqlCheckContext((long) response.getSqls().size());
            response.getSqls().forEach(v -> {
                List<CheckViolation> violations = sqlChecker.check(checkContext,
                        Collections.singletonList(v.getSqlTuple()));
                fullFillRiskLevelAndSetViolation(violations, rules, response);
                violations
                        .forEach(c -> offset2Violations.computeIfAbsent(c.getOffset(), k -> new ArrayList<>()).add(c));
            });
            context.put(SQL_CHECK_RESULT_KEY, offset2Violations);
            return response.getSqls().stream().noneMatch(v -> {
                if (CollectionUtils.isEmpty(v.getViolatedRules())) {
                    return false;
                }
                return v.getViolatedRules().stream().anyMatch(rule -> rule.getLevel() > 0);
            });
        } catch (Exception e) {
            log.warn("Failed to init sql check message", e);
        }
        return true;
    }

    @Override
    protected String getExecuteStageName() {
        return SqlExecuteStages.SQL_CHECK;
    }

    @Override
    @SuppressWarnings("all")
    public void afterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull AsyncExecuteContext context) throws Exception {
        Map<String, Object> ctx = context.getContextMap();
        if (!ctx.containsKey(SQL_CHECK_RESULT_KEY)) {
            return;
        }
        Map<Integer, List<CheckViolation>> map = (Map<Integer, List<CheckViolation>>) ctx.get(SQL_CHECK_RESULT_KEY);
        List<CheckViolation> results = map.get(response.getSqlTuple().getOffset());
        if (CollectionUtils.isEmpty(results)) {
            return;
        }
        if (!hasSyntaxError(session.getDialectType(), response)) {
            results.removeIf(i -> i.getType() == SqlCheckRuleType.SYNTAX_ERROR);
        }
        response.setCheckViolations(results);
    }

    @Override
    public int getOrder() {
        return 3;
    }

    private boolean hasSyntaxError(DialectType dialectType, SqlExecuteResult response) {
        if (response.getStatus() != SqlExecuteStatus.FAILED) {
            return false;
        }
        int errorCode = response.getErrorCode();
        if (errorCode == -1) {
            return isSyntaxErrorMessage(response.getTrack());
        }
        switch (dialectType) {
            case OB_ORACLE:
                return errorCode == 900;
            case MYSQL:
            case OB_MYSQL:
            case ODP_SHARDING_OB_MYSQL:
                return errorCode == 1064;
            default:
                return isSyntaxErrorMessage(response.getTrack());
        }
    }

    private boolean isSyntaxErrorMessage(String message) {
        return StringUtils.containsIgnoreCase(message, "syntax")
                && StringUtils.containsIgnoreCase(message, "error");
    }

    private void fullFillRiskLevelAndSetViolation(List<CheckViolation> violations,
            List<Rule> rules, SqlAsyncExecuteResp response) {
        List<Rule> vRules = new ArrayList<>(sqlCheckService.fullFillRiskLevel(rules, violations));
        Map<Integer, List<Rule>> offset2Rules = vRules.stream().collect(
                Collectors.groupingBy(rule -> rule.getViolation().getOffset()));
        response.getSqls().forEach(item -> item.getViolatedRules().addAll(
                offset2Rules.getOrDefault(item.getSqlTuple().getOffset(), new ArrayList<>())));
    }

}
