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
package com.oceanbase.odc.service.session;

import java.sql.Connection;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.config.OrganizationConfigProvider;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.SqlConsoleRules;
import com.oceanbase.odc.service.session.model.SessionSettings;

import lombok.extern.slf4j.Slf4j;

/**
 * Transaction service object, used to config some settings
 *
 * @author yh263208
 * @date 2021-06-08 11:32
 * @since ODC_release_2.4.2
 */
@Slf4j
@Service
@Validated
@SkipAuthorize("inside connect session")
public class SessionSettingsService {

    @Autowired
    private SessionProperties sessionProperties;

    @Autowired
    private OrganizationConfigProvider organizationConfigProvider;

    @Autowired
    private RuleService ruleService;

    public SessionSettings getSessionSettings(@NotNull ConnectionSession session) {
        SessionSettings settings = new SessionSettings();
        Boolean autocommit = false;
        if (!ConnectionSessionUtil.isLogicalSession(session)) {
            JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
            autocommit = jdbcOperations.execute(Connection::getAutoCommit);
        }
        settings.setAutocommit(autocommit);
        settings.setObVersion(ConnectionSessionUtil.getVersion(session));
        settings.setDelimiter(ConnectionSessionUtil.getSqlCommentProcessor(session).getDelimiter());
        settings.setQueryLimit(ConnectionSessionUtil.getQueryLimit(session));
        Long rulesetId = ConnectionSessionUtil.getRuleSetId(session);
        SqlConsoleRules targetKey = SqlConsoleRules.MAX_RETURN_ROWS;
        Rule targetRule = ruleService.getByRulesetIdAndMetadataName(rulesetId, targetKey);
        // if rule is not enabled, use organization config max query limit
        Integer maxQueryLimit =
                targetRule.getEnabled().equals(true)
                        ? (Integer) targetRule.getProperties().get(targetKey.getPropertyName())
                        : organizationConfigProvider.getDefaultMaxQueryLimit();
        settings.setMaxQueryLimit(maxQueryLimit);
        return settings;
    }

    public SessionSettings setSessionSettings(@NotNull ConnectionSession session,
            @NotNull @Valid SessionSettings settings) {
        Integer wait2UpdateQueryLimit = settings.getQueryLimit();
        if (sessionProperties.getResultSetMaxRows() >= 0) {
            PreConditions.lessThanOrEqualTo("queryLimit", LimitMetric.TRANSACTION_QUERY_LIMIT,
                    wait2UpdateQueryLimit, sessionProperties.getResultSetMaxRows());
        }
        if (!ConnectionSessionUtil.isLogicalSession(session)) {
            JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
            Boolean autocommit = jdbcOperations.execute(Connection::getAutoCommit);
            if (!Objects.equals(autocommit, settings.getAutocommit())) {
                jdbcOperations.execute((ConnectionCallback<Void>) conn -> {
                    conn.setAutoCommit(settings.getAutocommit());
                    return null;
                });
            }
        }
        SqlCommentProcessor processor = ConnectionSessionUtil.getSqlCommentProcessor(session);
        if (!settings.getDelimiter().equals(processor.getDelimiter())) {
            processor.setDelimiter(settings.getDelimiter());
        }
        Integer queryLimit = ConnectionSessionUtil.getQueryLimit(session);

        if (!Objects.equals(wait2UpdateQueryLimit, queryLimit)) {
            Long rulesetId = ConnectionSessionUtil.getRuleSetId(session);
            SqlConsoleRules targetKey = SqlConsoleRules.MAX_RETURN_ROWS;
            Integer environmentQueryLimit = (Integer) ruleService
                    .getByRulesetIdAndMetadataName(rulesetId, targetKey)
                    .getProperties().get(targetKey.getPropertyName());
            wait2UpdateQueryLimit = Math.min(wait2UpdateQueryLimit, environmentQueryLimit);
            ConnectionSessionUtil.setQueryLimit(session, wait2UpdateQueryLimit);
        }
        return settings;
    }

}
