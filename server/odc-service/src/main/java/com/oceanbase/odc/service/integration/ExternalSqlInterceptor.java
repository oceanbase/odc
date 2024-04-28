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
package com.oceanbase.odc.service.integration;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.core.sql.execute.SqlExecuteStages;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.integration.client.SqlInterceptorClient;
import com.oceanbase.odc.service.integration.model.SqlCheckStatus;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.SqlConsoleRuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule.RuleViolation;
import com.oceanbase.odc.service.regulation.ruleset.model.SqlConsoleRules;
import com.oceanbase.odc.service.session.interceptor.BaseTimeConsumingInterceptor;
import com.oceanbase.odc.service.session.model.AsyncExecuteContext;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/4/14 11:26
 */
@Slf4j
@Component
public class ExternalSqlInterceptor extends BaseTimeConsumingInterceptor {

    @Autowired
    private SqlInterceptorClient sqlInterceptorClient;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private RuleService ruleService;

    @Autowired
    private SqlConsoleRuleService sqlConsoleRuleService;

    private static final String ODC_SITE_URL = "odc.site.url";

    @Override
    public boolean doPreHandle(@NonNull SqlAsyncExecuteReq request, @NonNull SqlAsyncExecuteResp response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) {
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        if (Objects.isNull(ruleSetId) || isIndividualTeam()) {
            return true;
        }
        Optional<Integer> externalSqlInterceptorIdOpt = sqlConsoleRuleService.getProperties(ruleSetId,
                SqlConsoleRules.EXTERNAL_SQL_INTERCEPTOR, session.getDialectType(), Integer.class);
        if (!externalSqlInterceptorIdOpt.isPresent()) {
            return true;
        }
        Long externalSqlInterceptorId = externalSqlInterceptorIdOpt.get().longValue();
        Optional<IntegrationEntity> interceptorOpt =
                integrationService.findIntegrationById(externalSqlInterceptorId);
        if (!interceptorOpt.isPresent()) {
            return true;
        }
        SqlInterceptorProperties properties =
                (SqlInterceptorProperties) integrationService.getIntegrationProperties(interceptorOpt.get().getId());
        TemplateVariables variables = buildTemplateVariables(request.getSql(), session);
        SqlCheckStatus result = sqlInterceptorClient.check(properties, variables);
        switch (result) {
            case IN_WHITE_LIST:
                return true;
            case IN_BLACK_LIST:
                ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.EXTERNAL_SQL_INTERCEPTOR.getRuleName())
                        .ifPresent(rule -> {
                            Rule violationRule = new Rule();
                            RuleViolation violation = new RuleViolation();
                            violation.setText(request.getSql());
                            violation.setLevel(2);
                            violation.setOffset(0);
                            violation.setStart(0);
                            violation.setStop(request.getSql().length());
                            violation.setLocalizedMessage(SqlConsoleRules.EXTERNAL_SQL_INTERCEPTOR
                                    .getLocalizedMessage(new Object[] {rule.getProperties()
                                            .get(rule.getMetadata().getPropertyMetadatas().get(0).getName())
                                            .toString()}));
                            violationRule.setViolation(violation);
                            response.getViolatedRules().add(violationRule);
                        });
                return false;
            case NEED_REVIEW:
                ruleService.getByRulesetIdAndName(ruleSetId, SqlConsoleRules.EXTERNAL_SQL_INTERCEPTOR.getRuleName())
                        .ifPresent(rule -> {
                            Rule violationRule = new Rule();
                            RuleViolation violation = new RuleViolation();
                            violation.setText(request.getSql());
                            violation.setLevel(1);
                            violation.setOffset(0);
                            violation.setStart(0);
                            violation.setStop(request.getSql().length());
                            violation.setLocalizedMessage(SqlConsoleRules.EXTERNAL_SQL_INTERCEPTOR
                                    .getLocalizedMessage(new Object[] {rule.getProperties()
                                            .get(rule.getMetadata().getPropertyMetadatas().get(0).getName())
                                            .toString()}));
                            violationRule.setViolation(violation);
                            response.getViolatedRules().add(violationRule);
                        });
                return false;
            default:
                throw new UnexpectedException("SQL intercept failed, unknown intercept status: " + result);
        }
    }

    @Override
    public void afterCompletion(@NonNull SqlExecuteResult response,
            @NonNull ConnectionSession session, @NonNull AsyncExecuteContext context) {}


    @Override
    protected String getExecuteStageName() {
        return SqlExecuteStages.EXTERNAL_SQL_INTERCEPTION;
    }

    private TemplateVariables buildTemplateVariables(String sql, ConnectionSession session) {
        TemplateVariables variables = new TemplateVariables();
        // set SQL content
        variables.setAttribute(Variable.SQL_CONTENT, sql);
        // set SQL content json array
        List<String> statements = SqlUtils.split(session, sql, true);
        variables.setAttribute(Variable.SQL_CONTENT_JSON_ARRAY, JsonUtils.toJson(statements));
        // set user related variables
        variables.setAttribute(Variable.USER_ID, authenticationFacade.currentUserId());
        variables.setAttribute(Variable.USER_NAME, authenticationFacade.currentUsername());
        variables.setAttribute(Variable.USER_ACCOUNT, authenticationFacade.currentUserAccountName());
        // set connection related variables
        ConnectionConfig connection = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        if (Objects.nonNull(connection)) {
            variables.setAttribute(Variable.CONNECTION_NAME, connection.getName());
            variables.setAttribute(Variable.CONNECTION_TENANT, connection.getTenantName());
            for (Entry<String, String> entry : connection.getProperties().entrySet()) {
                variables.setAttribute(Variable.CONNECTION_PROPERTIES, entry.getKey(), entry.getValue());
            }
        }
        // set ODC URL site
        List<Configuration> configurations = systemConfigService.queryByKeyPrefix(ODC_SITE_URL);
        if (CollectionUtils.isNotEmpty(configurations)) {
            variables.setAttribute(Variable.ODC_SITE_URL, configurations.get(0).getValue());
        }
        return variables;
    }

    private boolean isIndividualTeam() {
        return authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
