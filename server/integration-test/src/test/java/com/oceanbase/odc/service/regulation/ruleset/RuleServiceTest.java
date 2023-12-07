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

package com.oceanbase.odc.service.regulation.ruleset;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingRepository;
import com.oceanbase.odc.migrate.jdbc.common.R4237DefaultRuleApplyingMigrate.InnerDefaultRuleApplying;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.Ruleset;

/**
 * @Author: Lebie
 * @Date: 2023/12/6 15:39
 * @Description: []
 */
public class RuleServiceTest extends ServiceTestEnv {
    private static final String MIGRATE_CONFIG_FILE = "init-config/init/regulation-rule-applying.yaml";
    private static final String rulesetName =
            "${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-dev-ruleset.name}";
    private static final String ruleName =
            "${com.oceanbase.odc.builtin-resource.regulation.rule.sql-console.not-allowed-edit-resultset.name}";

    private List<InnerDefaultRuleApplying> defaultRuleApplyingEntities;
    @Autowired
    private RuleService ruleService;

    @MockBean
    private RulesetService rulesetService;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private RuleApplyingRepository ruleApplyingRepository;

    @Before
    public void setUp() {
        this.defaultRuleApplyingEntities =
                YamlUtils.fromYaml(MIGRATE_CONFIG_FILE, new TypeReference<List<InnerDefaultRuleApplying>>() {});
        Mockito.when(rulesetService.detail(Mockito.anyLong())).thenReturn(getRuleset());
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
    }

    @Test
    public void testListRules_UserChangesRule_ReturnChangedRule() {
        Mockito.when(ruleApplyingRepository.findByOrganizationIdAndRulesetId(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(listRuleApplyingEntities());

        List<Rule> actual = ruleService.list(1L, QueryRuleMetadataParams.builder().build());
        List<InnerDefaultRuleApplying> rulesetName2Applyings = this.defaultRuleApplyingEntities.stream()
                .collect(Collectors.groupingBy(InnerDefaultRuleApplying::getRulesetName)).get(rulesetName);
        Assert.assertEquals(rulesetName2Applyings.size(), actual.size());
        Map<String, List<Rule>> name2Rules =
                actual.stream().collect(Collectors.groupingBy(r -> r.getMetadata().getName()));
        Assert.assertEquals("{\"fake_key\":\"fake_value\"}",
                JsonUtils.toJson(name2Rules.get(ruleName).get(0).getProperties()));
    }

    @Test
    public void testListRules_UserNotChangeRule_ReturnDefaultRule() {
        Mockito.when(ruleApplyingRepository.findByOrganizationIdAndRulesetId(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Collections.emptyList());

        List<Rule> actual = ruleService.list(1L, QueryRuleMetadataParams.builder().build());
        List<InnerDefaultRuleApplying> rulesetName2Applyings = this.defaultRuleApplyingEntities.stream()
                .collect(Collectors.groupingBy(InnerDefaultRuleApplying::getRulesetName)).get(rulesetName);
        Assert.assertEquals(rulesetName2Applyings.size(), actual.size());
        Map<String, List<Rule>> name2Rules =
                actual.stream().collect(Collectors.groupingBy(r -> r.getMetadata().getName()));
        for (InnerDefaultRuleApplying r : rulesetName2Applyings) {
            Assert.assertTrue(isEqualRuleApplying(r, name2Rules.get(r.getRuleName()).get(0)));
        }
    }

    @Test
    public void updateRule_NotUpdateBefore_SaveSuccess() {
        Mockito.when(ruleApplyingRepository.findByOrganizationIdAndRulesetId(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(Collections.emptyList());

        ruleService.update(1L, 1L, getRule());
        Mockito.verify(ruleApplyingRepository, Mockito.times(1)).save(Mockito.any(RuleApplyingEntity.class));
    }

    @Test
    public void updateRule_UpdatedBefore_SaveSuccess() {
        Mockito.when(ruleApplyingRepository.findByOrganizationIdAndRulesetId(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(listRuleApplyingEntities());

        ruleService.update(1L, 1L, getRule());
        Mockito.verify(ruleApplyingRepository, Mockito.times(1)).save(Mockito.any(RuleApplyingEntity.class));
    }

    private Rule getRule() {
        Rule rule = new Rule();
        rule.setId(1L);
        rule.setLevel(1);
        rule.setEnabled(true);
        rule.setAppliedDialectTypes(Arrays.asList(DialectType.MYSQL));
        rule.setRulesetId(1L);
        return rule;
    }

    private Ruleset getRuleset() {
        Ruleset ruleset = new Ruleset();
        ruleset.setId(1L);
        ruleset.setName("${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-dev-ruleset.name}");
        return ruleset;
    }

    private List<RuleApplyingEntity> listRuleApplyingEntities() {
        RuleApplyingEntity ruleApplyingEntity = new RuleApplyingEntity();
        ruleApplyingEntity.setRuleMetadataId(1L);
        ruleApplyingEntity.setPropertiesJson("{\"fake_key\": \"fake_value\"}");
        ruleApplyingEntity.setRulesetId(1L);
        ruleApplyingEntity.setOrganizationId(1L);
        return Arrays.asList(ruleApplyingEntity);
    }

    private boolean isEqualRuleApplying(InnerDefaultRuleApplying left, Rule right) {
        return left.getEnabled().equals(right.getEnabled()) && left.getLevel().equals(right.getLevel())
                && CollectionUtils.isEqualCollection(left.getAppliedDialectTypes().stream().map(
                        DialectType::fromValue).collect(Collectors.toList()), right.getAppliedDialectTypes());
    }
}


