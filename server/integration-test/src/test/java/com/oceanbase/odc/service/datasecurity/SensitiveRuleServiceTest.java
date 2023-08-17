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
package com.oceanbase.odc.service.datasecurity;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.datasecurity.MaskingAlgorithmEntity;
import com.oceanbase.odc.metadb.datasecurity.SensitiveRuleEntity;
import com.oceanbase.odc.metadb.datasecurity.SensitiveRuleRepository;
import com.oceanbase.odc.service.datasecurity.model.QuerySensitiveRuleParams;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;
import com.oceanbase.odc.service.datasecurity.util.SensitiveRuleMapper;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * @author gaoda.xy
 * @date 2023/5/19 16:24
 */
public class SensitiveRuleServiceTest extends ServiceTestEnv {

    @Autowired
    private SensitiveRuleService service;

    @Autowired
    private SensitiveRuleRepository ruleRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private MaskingAlgorithmService algorithmService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private static final SensitiveRuleMapper ruleMapper = SensitiveRuleMapper.INSTANCE;
    private static final Long PROJECT_ID = 1L;
    private static final Long MASKING_ALGORITHM_ID = 1L;

    @Before
    public void setUp() throws Exception {
        ruleRepository.deleteAll();
        MaskingAlgorithmEntity entity = new MaskingAlgorithmEntity();
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        Mockito.when(algorithmService.nullSafeGet(Mockito.anyLong())).thenReturn(entity);
    }

    @After
    public void tearDown() throws Exception {
        ruleRepository.deleteAll();
    }

    @Test
    public void test_create_success() {
        SensitiveRule rule = createSensitiveRule("test_create", SensitiveRuleType.REGEX);
        SensitiveRule created = service.create(rule.getProjectId(), rule);
        Assert.assertEquals(rule.getName(), created.getName());
    }

    @Test
    public void test_create_duplicated_throwException() {
        SensitiveRule rule = createSensitiveRule("test_create", SensitiveRuleType.REGEX);
        service.create(rule.getProjectId(), rule);
        rule.setProjectId(rule.getProjectId() + 1L);
        service.create(rule.getProjectId(), rule);
        thrown.expect(BadRequestException.class);
        service.create(rule.getProjectId(), rule);
    }

    @Test
    public void test_update_success() {
        SensitiveRule rule = createSensitiveRule("test_update", SensitiveRuleType.REGEX);
        SensitiveRule created = service.create(rule.getProjectId(), rule);
        rule.setName("updated");
        SensitiveRule updated = service.update(rule.getProjectId(), created.getId(), rule);
        Assert.assertEquals("updated", updated.getName());
    }

    @Test
    public void test_update_builtin_throwUnsupportedException() {
        SensitiveRule rule = createSensitiveRule("test_update", SensitiveRuleType.REGEX);
        SensitiveRule created = service.create(rule.getProjectId(), rule);
        SensitiveRuleEntity entity = ruleMapper.modelToEntity(created);
        entity.setBuiltin(true);
        ruleRepository.saveAndFlush(entity);
        rule.setName("updated");
        thrown.expect(UnsupportedException.class);
        service.update(rule.getProjectId(), created.getId(), rule);
    }

    @Test
    public void test_delete_success() {
        SensitiveRule rule = createSensitiveRule("test_delete", SensitiveRuleType.REGEX);
        SensitiveRule created = service.create(rule.getProjectId(), rule);
        service.delete(created.getProjectId(), created.getId());
        Assert.assertFalse(ruleRepository.findById(created.getId()).isPresent());
    }

    @Test
    public void test_delete_builtin_throwUnsupportedException() {
        SensitiveRule rule = createSensitiveRule("test_delete", SensitiveRuleType.REGEX);
        SensitiveRule created = service.create(rule.getProjectId(), rule);
        SensitiveRuleEntity entity = ruleMapper.modelToEntity(created);
        entity.setBuiltin(true);
        ruleRepository.saveAndFlush(entity);
        thrown.expect(UnsupportedException.class);
        service.delete(rule.getProjectId(), created.getId());
    }

    @Test
    public void test_list_all() {
        SensitiveRule rule;
        rule = createSensitiveRule("test_list_1", SensitiveRuleType.REGEX);
        service.create(rule.getProjectId(), rule);
        rule = createSensitiveRule("test_list_2", SensitiveRuleType.GROOVY);
        service.create(rule.getProjectId(), rule);
        rule = createSensitiveRule("test_list_3", SensitiveRuleType.PATH);
        rule.setProjectId(PROJECT_ID + 1L);
        service.create(rule.getProjectId(), rule);
        QuerySensitiveRuleParams params = QuerySensitiveRuleParams.builder().projectId(PROJECT_ID).build();
        Page<SensitiveRule> listed = service.list(params.getProjectId(), params, Pageable.unpaged());
        Assert.assertEquals(2, listed.getContent().size());
    }

    @Test
    public void test_list_byType() {
        SensitiveRule rule;
        rule = createSensitiveRule("test_list_1", SensitiveRuleType.REGEX);
        service.create(rule.getProjectId(), rule);
        rule = createSensitiveRule("test_list_2", SensitiveRuleType.GROOVY);
        service.create(rule.getProjectId(), rule);
        rule = createSensitiveRule("test_list_3", SensitiveRuleType.PATH);
        rule.setProjectId(PROJECT_ID + 1L);
        service.create(rule.getProjectId(), rule);
        QuerySensitiveRuleParams params = QuerySensitiveRuleParams.builder().projectId(PROJECT_ID)
                .types(Arrays.asList(SensitiveRuleType.REGEX)).build();
        Page<SensitiveRule> listed = service.list(params.getProjectId(), params, Pageable.unpaged());
        Assert.assertEquals(1, listed.getContent().size());
        Assert.assertEquals(listed.getContent().get(0).getName(), "test_list_1");
    }

    @Test
    public void test_setEnabled_success() {
        SensitiveRule rule = createSensitiveRule("test_setEnabled", SensitiveRuleType.REGEX);
        SensitiveRule created = service.create(rule.getProjectId(), rule);
        Assert.assertTrue(created.getEnabled());
        SensitiveRule disabled = service.setEnabled(created.getProjectId(), created.getId(), false);
        Assert.assertFalse(disabled.getEnabled());
    }

    @Test
    public void test_setEnabled_builtin_throwUnsupportedException() {
        SensitiveRule rule = createSensitiveRule("test_setEnabled", SensitiveRuleType.REGEX);
        SensitiveRule created = service.create(rule.getProjectId(), rule);
        SensitiveRuleEntity entity = ruleMapper.modelToEntity(created);
        entity.setBuiltin(true);
        ruleRepository.saveAndFlush(entity);
        thrown.expect(UnsupportedException.class);
        service.setEnabled(created.getProjectId(), created.getId(), false);
    }

    private SensitiveRule createSensitiveRule(String name, SensitiveRuleType type) {
        SensitiveRule rule = new SensitiveRule();
        rule.setProjectId(PROJECT_ID);
        rule.setName(name);
        rule.setEnabled(true);
        rule.setType(type);
        rule.setDatabaseRegexExpression("\\S*phone\\S*");
        rule.setTableRegexExpression("\\S*phone\\S*");
        rule.setColumnRegexExpression("\\S*phone\\S*");
        rule.setColumnCommentRegexExpression("\\S*phone\\S*");
        rule.setGroovyScript("return true;");
        rule.setPathIncludes(Arrays.asList("*.*.*"));
        rule.setPathExcludes(new ArrayList<>());
        rule.setMaskingAlgorithmId(MASKING_ALGORITHM_ID);
        return rule;
    }

}
