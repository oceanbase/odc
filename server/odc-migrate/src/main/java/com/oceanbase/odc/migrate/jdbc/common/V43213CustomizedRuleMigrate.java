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
package com.oceanbase.odc.migrate.jdbc.common;

import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/10/15 17:45
 * @Description: version: 4.3.2-bp1, this migration is only for fixing the issue #3651, and it only
 *               works for the SQL rules that were added in V4.3.2
 */
@Slf4j
@Migratable(version = "4.3.2.13",
        description = "For fixing SQL rules that were added in V4.3.2")
public class V43213CustomizedRuleMigrate implements JdbcMigratable {
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate txTemplate;

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        List<RulesetInfo> customizedRulesets = listCustomizedRulesets();
        if (CollectionUtils.isEmpty(customizedRulesets)) {
            log.info("No customized rulesets found, skip the migration.");
            return;
        }
        List<Rule> rulesToMigrate = listDefaultEnvSqlAffectedRowsRules(customizedRulesets);
        int affectedRows = insertOnDuplicateKey(rulesToMigrate);
        log.info("Migrate {} rules for customized rulesets, affected rows: {}", rulesToMigrate.size(), affectedRows);
    }

    private List<RulesetInfo> listCustomizedRulesets() {
        return jdbcTemplate.query("SELECT id, organization_id FROM regulation_ruleset WHERE is_builtin = 0",
                (rs, rowNum) -> {
                    RulesetInfo rulesetInfo = new RulesetInfo();
                    rulesetInfo.id = rs.getLong("id");
                    rulesetInfo.organizationId = rs.getLong("organization_id");
                    return rulesetInfo;
                });
    }

    private List<Rule> listDefaultEnvSqlAffectedRowsRules(List<RulesetInfo> customizedRulesets) {
        String sql = "select level, rule_metadata_id, applied_dialect_types, properties_json "
                + "from regulation_default_rule_applying"
                + " where (rule_metadata_id=59 and ruleset_name='${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-default-ruleset.name}')"
                + " or (rule_metadata_id=60 and ruleset_name='${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-default-ruleset.name}')";
        List<Rule> rules = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Rule rule = new Rule();
            rule.level = rs.getInt("level");
            rule.ruleMetadataId = rs.getLong("rule_metadata_id");
            rule.appliedDialectTypes = rs.getString("applied_dialect_types");
            rule.propertiesJson = rs.getString("properties_json");
            rule.enabled = false;
            return rule;
        });
        return rules.stream().flatMap(rule -> customizedRulesets.stream()
                .map(ruleset -> {
                    rule.organizationId = ruleset.organizationId;
                    rule.rulesetId = ruleset.id;
                    return rule;
                })).collect(Collectors.toList());
    }

    private int insertOnDuplicateKey(List<Rule> rules) {
        String sql =
                "INSERT INTO regulation_rule_applying (organization_id, is_enabled, level, ruleset_id, rule_metadata_id, applied_dialect_types, properties_json) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE `id` = `id`";
        int affectedRows = 0;
        for (Rule rule : rules) {
            affectedRows += jdbcTemplate.update(sql, rule.organizationId, rule.enabled, rule.level, rule.rulesetId,
                    rule.ruleMetadataId, rule.appliedDialectTypes, rule.propertiesJson);
        }
        return affectedRows;
    }

    private class RulesetInfo {
        Long id;
        Long organizationId;
    }

    private class Rule {
        Long organizationId;

        Boolean enabled;

        Integer level;

        Long rulesetId;

        Long ruleMetadataId;

        String appliedDialectTypes;

        String propertiesJson;
    }
}
