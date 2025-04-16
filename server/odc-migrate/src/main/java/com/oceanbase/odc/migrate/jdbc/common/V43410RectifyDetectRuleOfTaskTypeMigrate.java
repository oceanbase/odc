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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.regulation.risklevel.model.ConditionExpression;
import com.oceanbase.odc.service.regulation.risklevel.model.NodeType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Mayang
 * @Description: version: 4.3.4, this migration is only for highRiskLevel detect rules for team
 *               organizations, add the types of tasks supported by default in ODC V434
 */
@Slf4j
@Migratable(version = "4.3.4.10",
        description = "For rectifying default risk level detect rules in V4.3.4")
public class V43410RectifyDetectRuleOfTaskTypeMigrate implements JdbcMigratable {
    private NamedParameterJdbcTemplate jdbcTemplate;
    private TransactionTemplate txTemplate;
    private static final String DEFAULT_TASK_TYPE_DETECT_RULE =
            "{\"expression\":\"TASK_TYPE\",\"operator\":\"EQUALS\",\"type\":\"CONDITION\",\"value\":\"APPLY_TABLE_PERMISSION\"}";
    private static final String DEFAULT_HIGH_RISK_DETECT_NAME = "default high risk level detect rule";

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        final Map<Long, Long> organizationId2HighestRiskLevelId = getOrganizationIdMappingHighestRiskLevelId(
                listAllTeamOrganizationId());
        int affectedRows = upsertRiskLevelDetectRule(organizationId2HighestRiskLevelId);
        log.info("Migrate final detect rules: {}, affected rows: {}",
                JsonUtils.toJson(organizationId2HighestRiskLevelId), affectedRows);
    }


    private List<Long> listAllTeamOrganizationId() {
        String sql = "SELECT id FROM iam_organization WHERE type = 'TEAM'";
        return jdbcTemplate.queryForList(sql, new HashMap<>(), Long.class);
    }

    private Map<Long, Long> getOrganizationIdMappingHighestRiskLevelId(Collection<Long> organizationIds) {
        if (CollectionUtils.isEmpty(organizationIds)) {
            return Collections.emptyMap();
        }
        String sql =
                "SELECT id, organization_id FROM regulation_risklevel WHERE organization_id IN (:organizationIds) AND level = (SELECT MAX(level) FROM regulation_risklevel)";
        HashMap<String, Object> params = new HashMap<>();
        params.put("organizationIds", organizationIds);
        return jdbcTemplate.query(sql, params, rs -> {
            Map<Long, Long> resultMap = new HashMap<>();
            while (rs.next()) {
                long riskLevelId = rs.getLong("id");
                long orgId = rs.getLong("organization_id");
                resultMap.computeIfAbsent(orgId, k -> riskLevelId);
            }
            return resultMap;
        });
    }

    private List<DetectRule> listRiskLevelDetectRules(Map<Long, Long> organizationId2HighestRiskLevelId) {
        if (CollectionUtils.isEmpty(organizationId2HighestRiskLevelId)) {
            return Collections.emptyList();
        }
        String sql = "SELECT id, risk_level_id,organization_id,value_json " +
                "FROM regulation_riskdetect_rule " +
                "WHERE organization_id IN (:organizationIds) AND risk_level_id IN (:riskLevelIds)";
        HashMap<String, Object> params = new HashMap<>();
        params.put("organizationIds", organizationId2HighestRiskLevelId.keySet());
        params.put("riskLevelIds", organizationId2HighestRiskLevelId.values());
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            DetectRule detectRule = new DetectRule();
            detectRule.setId(rs.getLong("id"));
            detectRule.setRiskLevelId(rs.getLong("risk_level_id"));
            detectRule.setOrganizationId(rs.getLong("organization_id"));
            detectRule.setValueJson(rs.getString("value_json"));
            return detectRule;
        });
    }

    private boolean checkExistDefaultDetectRuleCondition(JsonNode detectRuleNode) {
        try {
            if (!NodeType.CONDITION_GROUP.name().equals(detectRuleNode.path("type").asText()) ||
                    !"OR".equals(detectRuleNode.path("booleanOperator").asText())) {
                return false;
            }
            JsonNode children = detectRuleNode.path("children");
            if (!children.isArray()) {
                return false;
            }
            for (JsonNode child : children) {
                if (!NodeType.CONDITION.name().equals(child.path("type").asText()) ||
                        !ConditionExpression.TASK_TYPE.name().equals(child.path("expression").asText()) ||
                        !"EQUALS".equals(child.path("operator").asText())) {
                    continue;
                }
                if (TaskType.APPLY_TABLE_PERMISSION.name().equals(child.path("value").asText())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Check exist default detect rule condition failed", e);
        }
        return false;
    }

    private Map<Long, DetectRule> checkAndFillDetectRulesByOrganizationId(
            Map<Long, Long> organizationId2HighestRiskLevelId) throws Exception {
        final Map<Long, DetectRule> organizationId2DetectRules =
                listRiskLevelDetectRules(organizationId2HighestRiskLevelId)
                        .stream()
                        .collect(Collectors.toMap(DetectRule::getOrganizationId, Function.identity(), (o, r) -> o));
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode defaultTaskTypeDetectRule = objectMapper.readTree(DEFAULT_TASK_TYPE_DETECT_RULE);
        for (Long orgId : organizationId2HighestRiskLevelId.keySet()) {
            DetectRule detectRule = ObjectUtil.defaultIfNull(organizationId2DetectRules.get(orgId), new DetectRule());
            detectRule.setRiskLevelId(organizationId2HighestRiskLevelId.get(orgId));
            detectRule.setOrganizationId(orgId);

            ObjectNode wrapperCondition = objectMapper.createObjectNode();
            wrapperCondition.put("booleanOperator", "OR");
            wrapperCondition.put("type", NodeType.CONDITION_GROUP.name());
            ArrayNode childrenArray = objectMapper.createArrayNode();
            childrenArray.add(defaultTaskTypeDetectRule);
            if (StringUtils.isNotBlank(detectRule.getValueJson())) {
                JsonNode currentValueJson = objectMapper.readTree(detectRule.getValueJson());
                if (checkExistDefaultDetectRuleCondition(currentValueJson)) {
                    organizationId2DetectRules.remove(orgId);
                    continue;
                }
                childrenArray.add(currentValueJson);
            }
            wrapperCondition.set("children", childrenArray);

            detectRule.setValueJsonOld(detectRule.getValueJson());
            detectRule.setValueJson(objectMapper.writeValueAsString(wrapperCondition));
            organizationId2DetectRules.computeIfAbsent(orgId, k -> detectRule);
        }
        return organizationId2DetectRules;
    }

    private int upsertRiskLevelDetectRule(Map<Long, Long> organizationId2HighestRiskLevelId) {
        if (CollectionUtils.isEmpty(organizationId2HighestRiskLevelId)) {
            return 0;
        }
        try {
            final Map<Long, DetectRule> organizationId2DetectRules =
                    checkAndFillDetectRulesByOrganizationId(organizationId2HighestRiskLevelId);
            String sql =
                    "INSERT INTO regulation_riskdetect_rule (create_time, update_time, name, risk_level_id, is_builtin, creator_id, organization_id, value_json_old, value_json) "
                            + "VALUES (NOW(), NOW(), :detectRuleName, :riskLevelId, :isBuiltin, 1, :organizationId, :valueJsonOld, :valueJson) "
                            + "ON DUPLICATE KEY UPDATE update_time = NOW(), value_json_old = :valueJsonOld, value_json = :valueJson";
            int[] affectedRows = this.txTemplate.execute(status -> {
                MapSqlParameterSource[] parameterSources = organizationId2DetectRules.values().stream()
                        .map(detectRule -> new MapSqlParameterSource()
                                .addValue("detectRuleName", DEFAULT_HIGH_RISK_DETECT_NAME)
                                .addValue("riskLevelId", detectRule.getRiskLevelId())
                                .addValue("isBuiltin", -1L)
                                .addValue("organizationId", detectRule.getOrganizationId())
                                .addValue("valueJsonOld", detectRule.getValueJsonOld())
                                .addValue("valueJson", detectRule.getValueJson()))
                        .toArray(MapSqlParameterSource[]::new);
                return jdbcTemplate.batchUpdate(sql, parameterSources);
            });
            return affectedRows == null ? 0 : Arrays.stream(affectedRows).sum();
        } catch (Exception e) {
            log.warn("Migrate V43410 to rectify detect rule of task type error", e);
        }
        return 0;
    }

    @Data
    private static class DetectRule {
        Long id;
        Long riskLevelId;
        Long organizationId;
        String valueJsonOld;
        String valueJson;
    }
}
