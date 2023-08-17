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
package com.oceanbase.odc.service.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.alibaba.fastjson.JSON;
import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.automation.AutomationActionEntity;
import com.oceanbase.odc.metadb.automation.AutomationActionRepository;
import com.oceanbase.odc.metadb.automation.AutomationConditionEntity;
import com.oceanbase.odc.metadb.automation.AutomationConditionRepository;
import com.oceanbase.odc.metadb.automation.AutomationRuleEntity;
import com.oceanbase.odc.metadb.automation.AutomationRuleRepository;
import com.oceanbase.odc.metadb.automation.EventMetadataEntity;
import com.oceanbase.odc.metadb.automation.EventMetadataRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.service.automation.model.AutomationAction;
import com.oceanbase.odc.service.automation.model.AutomationCondition;
import com.oceanbase.odc.service.automation.model.AutomationRule;
import com.oceanbase.odc.service.automation.model.CreateRuleReq;
import com.oceanbase.odc.service.automation.model.QueryAutomationRuleParams;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;

public class AutomationServiceTest extends MockedAuthorityTestEnv {
    @Autowired
    private AutomationService automationService;
    @Autowired
    private AutomationConditionService conditionService;
    @Autowired
    private AutomationActionService actionService;
    @Autowired
    private AutomationRuleRepository ruleRepository;
    @Autowired
    private AutomationActionRepository actionRepository;
    @Autowired
    private AutomationConditionRepository conditionRepository;
    @Autowired
    private EventMetadataRepository eventRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private UserService userService;
    @MockBean
    private ConnectionService connectionService;
    @MockBean
    private EnvironmentService environmentService;

    private EventMetadataEntity eventEntity;

    @Before
    public void setUp() {
        ruleRepository.deleteAll();
        conditionRepository.deleteAll();
        actionRepository.deleteAll();
        grantAllPermissions(ResourceType.ODC_AUTOMATION_RULE);
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(ADMIN_USER_ID);
        Mockito.when(userService.detailWithoutPermissionCheck(Mockito.anyLong())).thenReturn(User.of(1L));
        UserEntity userEntity = new UserEntity();
        userEntity.setId(0L);
        userEntity.setName("admin");
        userEntity.setAccountName("admin");
        Mockito.when(userService.nullSafeGet(Mockito.anyLong())).thenReturn(userEntity);
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setOrganizationId(ORGANIZATION_ID);
        Mockito.when(connectionService.getWithoutPermissionCheck(Mockito.anyLong())).thenReturn(connectionConfig);
        eventEntity = eventRepository.saveAndFlush(createEventEntity());
    }

    @After
    public void tearDown() {
        ruleRepository.deleteAll();
        conditionRepository.deleteAll();
        actionRepository.deleteAll();
        eventRepository.delete(eventEntity);
    }

    @Test
    public void test_DetailRule() {
        long id = createRuleWithConditionsAndActions();

        AutomationRule rule = automationService.detail(id);
        Assert.assertEquals("test rule", rule.getName());
        Assert.assertEquals(2, rule.getConditions().size());
        Assert.assertEquals(1, rule.getActions().size());
    }

    @Test
    public void test_ListRules() {
        ruleRepository.saveAndFlush(createRuleEntity("rule1", eventEntity.getId()));
        ruleRepository.saveAndFlush(createRuleEntity("rule2", eventEntity.getId()));
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Direction.DESC, "createTime"));
        QueryAutomationRuleParams params =
                QueryAutomationRuleParams.builder().name("rule1").enabled(true).build();
        Page<AutomationRule> rules = automationService.listRules(pageable, params);
        Assert.assertEquals(1, rules.getContent().size());
        eventRepository.deleteById(eventEntity.getId());
    }

    @Test
    public void test_CreateRule() {

        List<AutomationCondition> conditions = new ArrayList<>();
        conditions.add(AutomationCondition.of(createConditionEntity(1L)));
        conditions.add(AutomationCondition.of(createConditionEntity(1L)));

        CreateRuleReq req = new CreateRuleReq();
        req.setName("test rule");
        req.setEventId(eventEntity.getId());
        req.setEnabled(true);
        req.setConditions(conditions);
        AutomationRule rule = automationService.create(req);
        List<AutomationConditionEntity> saved = conditionRepository.findAll();
        Assert.assertEquals(2, saved.size());
        eventRepository.deleteById(eventEntity.getId());
    }

    @Test
    public void test_SetRuleEnabled() {
        Long id = ruleRepository.saveAndFlush(createRuleEntity("test rule", eventEntity.getId())).getId();
        AutomationRule automationRule = automationService.setRuleEnabled(id, false);
        Assert.assertFalse(automationRule.getEnabled());
        eventRepository.deleteById(eventEntity.getId());
    }

    @Test
    public void test_Update() {
        List<AutomationAction> actions = new ArrayList<>();
        actions.add(AutomationAction.of(createActionEntity(1L)));
        actions.add(AutomationAction.of(createActionEntity(1L)));
        actions.add(AutomationAction.of(createActionEntity(1L)));
        CreateRuleReq req = new CreateRuleReq();
        req.setActions(actions);
        req.setName("test_update_automation_rule");

        long id = createRuleWithConditionsAndActions();
        AutomationRule rule = automationService.update(id, req);
        Assert.assertEquals(3, rule.getActions().size());
    }

    @Test
    public void test_Delete() {
        long id = createRuleWithConditionsAndActions();
        AutomationRule deleted = automationService.delete(id);
        Assert.assertEquals(2, deleted.getConditions().size());

        List<AutomationRuleEntity> rules = ruleRepository.findAll();
        List<AutomationActionEntity> actions = actionRepository.findAll();
        Assert.assertEquals(0, rules.size());
        Assert.assertEquals(0, actions.size());
    }

    @Test
    public void test_Exists() {
        createRuleWithConditionsAndActions();
        Assert.assertTrue(automationService.exists("test rule"));
    }

    @Test
    public void test_InsertCondition() {
        conditionService.insert(1L, AutomationCondition.of(createConditionEntity(1L)));
        conditionService.insert(1L, AutomationCondition.of(createConditionEntity(1L)));
        List<AutomationConditionEntity> entities = conditionRepository.findAll();
        Assert.assertEquals(2, entities.size());
    }

    @Test
    public void test_InsertAction() {
        actionService.insert(1L, AutomationAction.of(createActionEntity(1L)));
        actionService.insert(1L, AutomationAction.of(createActionEntity(1L)));
        List<AutomationActionEntity> entities = actionRepository.findAll();
        Assert.assertEquals(2, entities.size());
    }

    @Test
    public void test_ListRulesByEventName() {
        AutomationRuleEntity rule1 = ruleRepository.saveAndFlush(createRuleEntity("rule1", eventEntity.getId()));
        conditionRepository.saveAndFlush(createConditionEntity(rule1.getId()));
        ruleRepository.saveAndFlush(createRuleEntity("rule2", eventEntity.getId()));
        ruleRepository.saveAndFlush(createRuleEntity("rule3", eventEntity.getId()));

        List<AutomationRule> rules = automationService.listRulesByEventName(eventEntity.getName());
        Assert.assertEquals(3, rules.size());
        eventRepository.deleteById(eventEntity.getId());
    }

    private long createRuleWithConditionsAndActions() {
        AutomationRuleEntity ruleEntity = createRuleEntity("test rule", eventEntity.getId());
        AutomationRuleEntity savedEntity = ruleRepository.saveAndFlush(ruleEntity);
        long id = savedEntity.getId();
        conditionRepository.saveAndFlush(createConditionEntity(id));
        conditionRepository.saveAndFlush(createConditionEntity(id));
        actionRepository.saveAndFlush(createActionEntity(id));
        return id;
    }

    private EventMetadataEntity createEventEntity() {
        EventMetadataEntity eventEntity = new EventMetadataEntity();
        eventEntity.setName("test event");
        eventEntity.setCreatorId(1L);
        eventEntity.setOrganizationId(1L);
        eventEntity.setBuiltin(true);
        eventEntity.setHidden(false);

        List<String> objectList = new ArrayList<>();
        objectList.add("USER");
        objectList.add("DEPARTMENT");
        eventEntity.setVariableNames(JSON.toJSONString(objectList));
        return eventEntity;
    }

    private AutomationRuleEntity createRuleEntity(String ruleName, Long eventId) {
        AutomationRuleEntity ruleEntity = new AutomationRuleEntity();
        ruleEntity.setName(ruleName);
        ruleEntity.setEventId(eventId);
        ruleEntity.setCreatorId(1L);
        ruleEntity.setOrganizationId(1L);
        ruleEntity.setEnabled(true);
        ruleEntity.setBuiltIn(true);
        return ruleEntity;
    }

    private AutomationConditionEntity createConditionEntity(Long ruleId) {
        AutomationConditionEntity conditionEntity = new AutomationConditionEntity();
        conditionEntity.setRuleId(ruleId);
        conditionEntity.setObject("test");
        conditionEntity.setExpression("test");
        conditionEntity.setOperation("contains");
        conditionEntity.setValue("test");
        conditionEntity.setCreatorId(1L);
        conditionEntity.setOrganizationId(1L);
        conditionEntity.setEnabled(true);
        return conditionEntity;
    }

    private AutomationActionEntity createActionEntity(Long ruleId) {
        AutomationActionEntity actionEntity = new AutomationActionEntity();
        actionEntity.setRuleId(ruleId);
        actionEntity.setAction("BindPermission");
        actionEntity.setCreatorId(1L);
        actionEntity.setOrganizationId(1L);
        actionEntity.setEnabled(true);

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("resourceType", "ODC_CONNECTION");
        objectMap.put("resourceId", 123123);
        actionEntity.setArgsJsonArray(JSON.toJSONString(objectMap));
        return actionEntity;
    }

}
