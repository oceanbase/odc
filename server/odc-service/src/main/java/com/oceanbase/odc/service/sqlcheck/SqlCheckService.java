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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule.RuleViolation;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleType;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.odc.service.sqlcheck.model.CheckResult;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckReq;
import com.oceanbase.odc.service.sqlcheck.rule.SqlCheckRules;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SqlCheckService}
 *
 * @author yh263208
 * @date 2022-11-28 11:01
 * @since ODC_release_4.1.0
 */
@Slf4j
@Service
@Validated
@SkipAuthorize("inside connect session")
public class SqlCheckService {

    @Autowired
    private RuleService ruleService;
    @Autowired
    private EnvironmentService environmentService;

    public List<CheckResult> check(@NotNull ConnectionSession session,
            @NotNull @Valid SqlCheckReq req) {
        Long ruleSetId = ConnectionSessionUtil.getRuleSetId(session);
        if (ruleSetId == null) {
            return Collections.emptyList();
        }
        List<Rule> rules = this.ruleService.list(ruleSetId, QueryRuleMetadataParams.builder().build());
        List<SqlCheckRule> sqlCheckRules = getRules(rules, session);
        if (CollectionUtils.isEmpty(sqlCheckRules)) {
            return Collections.emptyList();
        }
        SqlChecker sqlChecker = new DefaultSqlChecker(session.getDialectType(), req.getDelimiter(), sqlCheckRules);
        List<CheckViolation> checkViolations = sqlChecker.check(req.getScriptContent());
        fullFillRiskLevel(rules, checkViolations);
        return SqlCheckUtil.buildCheckResults(checkViolations);
    }

    public List<CheckViolation> check(@NotNull Long environmentId, @NonNull String databaseName,
            @NotNull List<OffsetString> sqls, @NotNull ConnectionConfig config) {
        if (CollectionUtils.isEmpty(sqls)) {
            return Collections.emptyList();
        }
        Environment env = this.environmentService.detail(environmentId);
        List<Rule> rules = this.ruleService.list(env.getRulesetId(), QueryRuleMetadataParams.builder().build());
        OBConsoleDataSourceFactory factory = new OBConsoleDataSourceFactory(config, true, false);
        factory.resetSchema(origin -> databaseName);
        SqlCheckContext checkContext = new SqlCheckContext((long) sqls.size());
        try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) factory.getDataSource()) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            List<SqlCheckRule> checkRules = getRules(rules, config.getDialectType(), jdbc);
            DefaultSqlChecker sqlChecker = new DefaultSqlChecker(config.getDialectType(), null, checkRules);
            List<CheckViolation> checkViolations = new ArrayList<>();
            for (OffsetString sql : sqls) {
                List<CheckViolation> violations = sqlChecker.check(Collections.singletonList(sql), checkContext);
                fullFillRiskLevel(rules, violations);
                checkViolations.addAll(violations);
            }
            return checkViolations;
        }
    }

    public List<SqlCheckRule> getRules(List<Rule> rules, @NonNull ConnectionSession session) {
        return getRules(rules, session.getDialectType(),
                session.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY));
    }

    public List<SqlCheckRule> getRules(List<Rule> rules,
            @NonNull DialectType dialectType, @NonNull JdbcOperations jdbc) {
        if (CollectionUtils.isEmpty(rules)) {
            return Collections.emptyList();
        }
        List<SqlCheckRuleFactory> candidates = SqlCheckRules.getAllFactories(dialectType, jdbc);
        return rules.stream().filter(rule -> {
            RuleMetadata metadata = rule.getMetadata();
            if (metadata == null || !Boolean.TRUE.equals(rule.getEnabled())) {
                return false;
            }
            return Objects.equals(metadata.getType(), RuleType.SQL_CHECK);
        }).map(rule -> {
            try {
                return SqlCheckRules.createByRule(candidates, dialectType, rule);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<Rule> fullFillRiskLevel(List<Rule> rules, @NonNull List<CheckViolation> violations) {
        List<Rule> violatedRules = new ArrayList<>();
        Map<String, Rule> name2RuleMap = CollectionUtils.isEmpty(rules)
                ? new HashMap<>()
                : rules.stream().collect(Collectors.toMap(r -> r.getMetadata().getName(), r -> r));
        violations.forEach(c -> {
            String name = "${" + c.getType().getName() + "}";
            Rule rule = name2RuleMap.getOrDefault(name, null);
            if (Objects.nonNull(rule)) {
                c.setLevel(rule.getLevel());
                Rule newRule = new Rule();
                newRule.setLevel(rule.getLevel());
                newRule.setViolation(RuleViolation.fromCheckViolation(c));
                violatedRules.add(newRule);
            }
        });
        return violatedRules;
    }

}
