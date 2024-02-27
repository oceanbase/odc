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
package com.oceanbase.odc.service.collaboration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.CreateEnvironmentReq;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.environment.model.EnvironmentStyle;
import com.oceanbase.odc.service.collaboration.environment.model.UpdateEnvironmentReq;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.RulesetService;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.Ruleset;

/**
 * @Author: Lebie
 * @Date: 2024/1/31 10:33
 * @Description: []
 */
public class EnvironmentServiceTest extends ServiceTestEnv {
    private static final Long RULESET_ID = 1L;

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private EnvironmentRepository environmentRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private RuleService ruleService;
    @MockBean
    private RulesetService rulesetService;
    @org.junit.Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        Mockito.when(ruleService.list(RULESET_ID, QueryRuleMetadataParams.builder().build())).thenReturn(listRules());
        Mockito.when(rulesetService.create(Mockito.any())).thenReturn(getRuleset());
        environmentRepository.deleteAll();
    }

    @After
    public void tearDown() {
        environmentRepository.deleteAll();
    }

    @Test
    public void testCreateEnvironment_Success() {
        Environment created = environmentService.create(
                CreateEnvironmentReq.builder().copiedRulesetId(RULESET_ID).description("a")
                        .name("test_").enabled(true)
                        .style(EnvironmentStyle.GEEKBLUE).build());
        Assert.assertNotNull(created.getId());
    }

    @Test
    public void testCreateEnvironment_DuplicatedName_ThrowException() {
        environmentService.create(
                CreateEnvironmentReq.builder().copiedRulesetId(RULESET_ID).description("a")
                        .name("test_").enabled(true)
                        .style(EnvironmentStyle.GEEKBLUE).build());
        thrown.expect(BadRequestException.class);
        environmentService.create(
                CreateEnvironmentReq.builder().copiedRulesetId(RULESET_ID).description("a")
                        .name("test_").enabled(true)
                        .style(EnvironmentStyle.GEEKBLUE).build());
    }

    @Test
    public void testUpdateEnvironment_Success() {
        Environment environment = environmentService.create(
                CreateEnvironmentReq.builder().copiedRulesetId(RULESET_ID).description("a")
                        .name("test_").enabled(true)
                        .style(EnvironmentStyle.GEEKBLUE).build());
        Environment updated = environmentService.update(environment.getId(),
                UpdateEnvironmentReq.builder().style(EnvironmentStyle.PURPLE).build());
        Assert.assertEquals(EnvironmentStyle.PURPLE, updated.getStyle());
    }

    @Test
    public void testDeleteEnvironment_Success() {
        Environment environment = environmentService.create(
                CreateEnvironmentReq.builder().copiedRulesetId(RULESET_ID).description("a")
                        .name("test_").enabled(true)
                        .style(EnvironmentStyle.GEEKBLUE).build());
        environmentService.delete(environment.getId());
        Assert.assertEquals(0, environmentRepository.count());
    }


    private Ruleset getRuleset() {
        Ruleset ruleset = new Ruleset();
        ruleset.setBuiltin(true);
        ruleset.setId(RULESET_ID);
        ruleset.setName("${com.oceanbase.odc.builtin-resource.regulation.ruleset.default-dev-ruleset.name}");
        return ruleset;
    }

    private List<Rule> listRules() {
        Rule rule = new Rule();
        rule.setId(1L);
        rule.setLevel(1);
        rule.setEnabled(true);
        rule.setAppliedDialectTypes(Arrays.asList(DialectType.MYSQL));
        rule.setRulesetId(1L);
        return Collections.singletonList(rule);
    }

}
