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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskDetectRuleEntity;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskDetectRuleRepository;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelEntity;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelRepository;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelStyle;

import cn.hutool.core.util.ReflectUtil;

/**
 * Test cases for {@link V43411RectifyDetectRuleOfTaskTypeMigrate}
 * 
 * @Author: ysj
 * @Date: 2025/4/18 14:31
 * @Since: ODC_release_4.3.4
 * @Description:
 */
public class V43411RectifyDetectRuleOfTaskTypeMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private RiskDetectRuleRepository repository;
    @Autowired
    private RiskLevelRepository riskLevelRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    private NamedParameterJdbcTemplate jdbcTemplate;
    private Map<Long, Long> organizationId2HighestRiskLevelId = new HashMap<>();
    private final V43411RectifyDetectRuleOfTaskTypeMigrate migrate = new V43411RectifyDetectRuleOfTaskTypeMigrate();
    private Method checkExistDetectRuleCondition;

    @Before
    public void setUp() {
        riskLevelRepository.deleteAll();
        repository.deleteAll();
        addMockedHighestRiskLevel();

        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        checkExistDetectRuleCondition = ReflectUtil.getMethod(V43411RectifyDetectRuleOfTaskTypeMigrate.class,
                "checkExistDefaultDetectRuleCondition", JsonNode.class);
        checkExistDetectRuleCondition.setAccessible(true);
    }

    @After
    public void clear() {
        repository.deleteAll();
        riskLevelRepository.deleteAll();
    }

    @Test
    public void migrate_checkConditionMethod_ExistSuccess() throws Exception {
        String existExpression = "{\n"
                + "  \"booleanOperator\": \"OR\",\n"
                + "  \"children\": [\n"
                + "    {\n"
                + "      \"expression\": \"TASK_TYPE\",\n"
                + "      \"operator\": \"EQUALS\",\n"
                + "      \"type\": \"CONDITION\",\n"
                + "      \"value\": \"APPLY_TABLE_PERMISSION\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"type\": \"CONDITION_GROUP\"\n"
                + "}";
        Boolean exist = ReflectUtil.invoke(migrate, checkExistDetectRuleCondition,
                new ObjectMapper().readTree(existExpression));
        Assert.assertTrue(exist);
    }

    @Test
    public void migrate_checkConditionMethod_NotExistSuccess() throws Exception {
        String existExpression = "{\n"
                + "  \"booleanOperator\": \"OR\",\n"
                + "  \"children\": [\n"
                + "    {\n"
                + "      \"expression\": \"TASK_TYPE\",\n"
                + "      \"operator\": \"CONTAINS\",\n"
                + "      \"type\": \"CONDITION\",\n"
                + "      \"value\": \"MOCKDATE\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"type\": \"CONDITION_GROUP\"\n"
                + "}";
        Boolean exist = ReflectUtil.invoke(migrate, checkExistDetectRuleCondition,
                new ObjectMapper().readTree(existExpression));
        Assert.assertFalse(exist);
    }

    @Test
    public void migrate_riskDetectRuleNotExists_migrateSucceed() throws Exception {
        Assert.assertTrue(listAllExistRiskDetectRule().isEmpty());
        migrate.migrate(dataSource);
        List<Map<String, Object>> detectRules = listAllExistRiskDetectRule();
        for (Map<String, Object> detectRule : detectRules) {
            String valueJson = (String) detectRule.get("value_json");
            Boolean exist = ReflectUtil.invoke(migrate, checkExistDetectRuleCondition,
                    new ObjectMapper().readTree(valueJson));
            Assert.assertTrue(exist);
        }
    }

    @Test
    public void migrate_riskDetectRuleExists_migrateSucceed() throws Exception {
        addMockedRiskDetectRules();
        List<Map<String, Object>> detectRules = listAllExistRiskDetectRule();

        Assert.assertEquals(detectRules.size(), organizationId2HighestRiskLevelId.size());
        for (Map<String, Object> detectRule : detectRules) {
            String valueJson = (String) detectRule.get("value_json");
            Boolean exist = ReflectUtil.invoke(migrate, checkExistDetectRuleCondition,
                    new ObjectMapper().readTree(valueJson));
            Assert.assertFalse(exist);
        }

        migrate.migrate(dataSource);
        detectRules = listAllExistRiskDetectRule();
        for (Map<String, Object> detectRule : detectRules) {
            String valueJson = (String) detectRule.get("value_json");
            Boolean exist = ReflectUtil.invoke(migrate, checkExistDetectRuleCondition,
                    new ObjectMapper().readTree(valueJson));
            Assert.assertTrue(exist);
        }
    }

    @Test
    public void migrateRepeatably_riskDetectRule_migrateSucceed() throws Exception {
        Assert.assertTrue(listAllExistRiskDetectRule().isEmpty());
        migrate.migrate(dataSource);
        // repeatably migrate
        migrate.migrate(dataSource);
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> detectRules = listAllExistRiskDetectRule();
        for (Map<String, Object> detectRule : detectRules) {
            String valueJson = (String) detectRule.get("value_json");
            JsonNode detectNode = objectMapper.readTree(valueJson);
            JsonNode detectSubNodes = detectNode.path("children");
            for (JsonNode detectSubNode : detectSubNodes) {
                Boolean exist = ReflectUtil.invoke(migrate, checkExistDetectRuleCondition, detectSubNode);
                Assert.assertFalse(exist);
            }
        }
    }

    private void addMockedHighestRiskLevel() {
        List<RiskLevelEntity> riskLevels =
                riskLevelRepository.saveAllAndFlush(organizationRepository.findIdByType(OrganizationType.TEAM)
                        .stream().map(orgId -> {
                            RiskLevelEntity riskLevelEntity = new RiskLevelEntity();
                            riskLevelEntity.setOrganizationId(orgId);
                            riskLevelEntity.setCreateTime(new Date());
                            riskLevelEntity.setUpdateTime(new Date());
                            riskLevelEntity.setLevel(3);
                            riskLevelEntity.setStyle(RiskLevelStyle.RED);
                            riskLevelEntity.setName(
                                    "${com.oceanbase.odc.builtin-resource.regulation.risklevel.high-risk.name}");
                            riskLevelEntity.setApprovalFlowConfigId(1L);
                            riskLevelEntity.setDescription("test risk level");
                            return riskLevelEntity;
                        }).collect(Collectors.toList()));
        organizationId2HighestRiskLevelId.putAll(riskLevels.stream()
                .collect(Collectors.toMap(RiskLevelEntity::getOrganizationId, RiskLevelEntity::getId, (o, r) -> o)));
    }

    private void addMockedRiskDetectRules() {
        String existExpression = "{\n"
                + "  \"booleanOperator\": \"OR\",\n"
                + "  \"children\": [\n"
                + "    {\n"
                + "      \"expression\": \"TASK_TYPE\",\n"
                + "      \"operator\": \"CONTAINS\",\n"
                + "      \"type\": \"CONDITION\",\n"
                + "      \"value\": \"MOCKDATE\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"type\": \"CONDITION_GROUP\"\n"
                + "}";
        repository.saveAllAndFlush(organizationId2HighestRiskLevelId.keySet()
                .stream().map(orgId -> {
                    RiskDetectRuleEntity riskDetectRuleEntity = new RiskDetectRuleEntity();
                    riskDetectRuleEntity.setOrganizationId(orgId);
                    riskDetectRuleEntity.setRiskLevelId(organizationId2HighestRiskLevelId.get(orgId));
                    riskDetectRuleEntity.setCreateTime(new Date());
                    riskDetectRuleEntity.setUpdateTime(new Date());
                    riskDetectRuleEntity.setName("test risk detect rule");
                    riskDetectRuleEntity.setBuiltIn(true);
                    riskDetectRuleEntity.setCreatorId(1L);
                    riskDetectRuleEntity.setValueJson(existExpression);
                    return riskDetectRuleEntity;
                }).collect(Collectors.toList()));
    }

    private List<Map<String, Object>> listAllExistRiskDetectRule() {
        if (CollectionUtils.isEmpty(organizationId2HighestRiskLevelId)) {
            return Collections.emptyList();
        }
        String sql = "SELECT value_json " +
                "FROM regulation_riskdetect_rule " +
                "WHERE organization_id IN (:organizationIds) AND risk_level_id IN (:riskLevelIds)";
        HashMap<String, Object> params = new HashMap<>();
        params.put("organizationIds", organizationId2HighestRiskLevelId.keySet());
        params.put("riskLevelIds", organizationId2HighestRiskLevelId.values());
        return jdbcTemplate.queryForList(sql, params);
    }
}
