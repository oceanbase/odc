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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.util.TraceStage;
import com.oceanbase.odc.common.util.TraceWatch;
import com.oceanbase.odc.common.util.TraceWatch.EditableTraceStage;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.service.config.UserConfigFacade;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.sqlcheck.DefaultSqlChecker;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckService;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;

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
public class SqlCheckInterceptor implements SqlExecuteInterceptor {

    private final static String SQL_CHECK_RESULT_KEY = "SQL_CHECK_RESULT";
    private final static String SQL_CHECK_ELAPSED_TIME_KEY = "SQL_CHECK_ELAPSED_TIME";
    @Autowired
    private UserConfigFacade userConfigFacade;
    @Autowired
    private SqlCheckService sqlCheckService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Override
    public boolean preHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull Map<String, Object> context) {
        if (this.authenticationFacade.currentUser().getOrganizationType() != OrganizationType.TEAM) {
            // 个人组织下不检查
            return true;
        }
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        if (ruleSetId == null) {
            return true;
        }
        List<Rule> rules = this.ruleService.list(ruleSetId, QueryRuleMetadataParams.builder().build());
        List<SqlCheckRule> sqlCheckRules = this.sqlCheckService.getRules(rules, session);
        if (CollectionUtils.isEmpty(sqlCheckRules)) {
            return true;
        }
        DefaultSqlChecker sqlChecker = new DefaultSqlChecker(session.getDialectType(), null, sqlCheckRules);
        try (TraceWatch watch = new TraceWatch();
                TraceStage s = watch.start(SqlExecuteStages.INIT_SQL_CHECK_MESSAGE)) {
            Map<String, List<CheckViolation>> sql2Violations = new HashMap<>();
            SqlCheckContext checkContext = new SqlCheckContext();
            response.getSqls().forEach(v -> sql2Violations.computeIfAbsent(v.getSqlTuple().getOriginalSql(), key -> {
                List<CheckViolation> violations = sqlChecker.check(Collections.singletonList(key), checkContext);
                List<Rule> vRules = sqlCheckService.fullFillRiskLevel(rules, violations);
                v.getViolatedRules().addAll(vRules.stream().filter(r -> r.getLevel() > 0).collect(Collectors.toList()));
                return violations;
            }));
            context.put(SQL_CHECK_RESULT_KEY, sql2Violations);
            context.put(SQL_CHECK_ELAPSED_TIME_KEY, s);
            return response.getSqls().stream().noneMatch(v -> CollectionUtils.isNotEmpty(v.getViolatedRules()));
        } catch (Exception e) {
            log.warn("Failed to init sql check message", e);
        }
        return true;
    }

    @Override
    @SuppressWarnings("all")
    public void afterCompletion(@NonNull SqlExecuteResult response, @NonNull ConnectionSession session,
            @NonNull Map<String, Object> context) throws Exception {
        if (!context.containsKey(SQL_CHECK_ELAPSED_TIME_KEY) || !context.containsKey(SQL_CHECK_RESULT_KEY)) {
            return;
        }
        TraceStage s = (TraceStage) context.get(SQL_CHECK_ELAPSED_TIME_KEY);
        Map<String, List<CheckViolation>> map = (Map<String, List<CheckViolation>>) context.get(SQL_CHECK_RESULT_KEY);
        response.setCheckViolations(map.get(response.getOriginSql()));
        if (response.getTraceWatch().isClosed()) {
            return;
        }
        try (EditableTraceStage stage = response.getTraceWatch().startEditableStage(s.getMessage())) {
            stage.setStartTime(s.getStartTime(), TimeUnit.MILLISECONDS);
            stage.setTime(s.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }

}
