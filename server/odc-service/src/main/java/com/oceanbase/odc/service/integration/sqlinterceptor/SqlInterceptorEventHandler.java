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
package com.oceanbase.odc.service.integration.sqlinterceptor;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.integration.IntegrationEvent;
import com.oceanbase.odc.service.integration.IntegrationEventHandler;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.SqlConsoleRuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.SqlConsoleRules;

@Component
public class SqlInterceptorEventHandler implements IntegrationEventHandler {

    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private SqlConsoleRuleService sqlConsoleRuleService;
    @Autowired
    private EnvironmentService environmentService;

    @Override
    public boolean support(IntegrationEvent integrationEvent) {
        return IntegrationType.SQL_INTERCEPTOR.equals(integrationEvent.getCurrentIntegrationType());
    }

    @Override
    public void preCreate(IntegrationEvent integrationEvent) {}

    @Override
    public void preDelete(IntegrationEvent integrationEvent) {
        usageCheck(integrationEvent.getCurrentConfig().getId(), AuditEventAction.DELETE_INTEGRATION);
    }

    @Override
    public void preUpdate(IntegrationEvent integrationEvent) {
        if (!integrationEvent.getCurrentConfig().getEnabled() && integrationEvent.getPreConfig().getEnabled()) {
            usageCheck(integrationEvent.getCurrentConfig().getId(), AuditEventAction.DISABLE_INTEGRATION);
        }
    }

    private void usageCheck(Long integrationId, AuditEventAction auditEventAction) {
        List<Rule> rules = ruleService.getByOrganizationIdAndRuleMetaDataName(
                authenticationFacade.currentOrganizationId(), SqlConsoleRules.EXTERNAL_SQL_INTERCEPTOR.getRuleName());
        Set<Long> ruleSetIds = new HashSet<>();
        for (Rule rule : rules) {
            Map<String, Object> properties = rule.getProperties();
            if (Objects.nonNull(properties)) {
                Object value =
                        properties.getOrDefault(SqlConsoleRules.EXTERNAL_SQL_INTERCEPTOR.getPropertyName(), null);
                if (Objects.nonNull(value) && ((Integer) value).longValue() == integrationId) {
                    ruleSetIds.add(rule.getRulesetId());
                }
            }
        }
        if (!ruleSetIds.isEmpty()) {
            String names = environmentService.list(authenticationFacade.currentOrganizationId()).stream()
                    .filter(e -> ruleSetIds.contains(e.getRulesetId())).map(e -> {
                        Locale locale = LocaleContextHolder.getLocale();
                        String i18nKey = e.getName().substring(2, e.getName().length() - 1);
                        return I18n.translate(i18nKey, null, i18nKey, locale);
                    })
                    .collect(Collectors.joining(", "));
            String errorMessage = String.format(
                    "External SQL interceptor integration id=%s cannot be %s because it has been referenced to following environment: {%s}",
                    integrationId, auditEventAction, names);
            throw new UnsupportedException(ErrorCodes.CannotOperateDueReference,
                    new Object[] {auditEventAction.getLocalizedMessage(),
                            ResourceType.ODC_EXTERNAL_SQL_INTERCEPTOR.getLocalizedMessage(), "name", names,
                            ResourceType.ODC_ENVIRONMENT.getLocalizedMessage()},
                    errorMessage);
        }
    }

}
