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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.google.common.collect.ImmutableMap;
import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.service.automation.model.AutomationAction;
import com.oceanbase.odc.service.automation.model.AutomationCondition;
import com.oceanbase.odc.service.automation.model.CreateRuleReq;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;

public class UserChangeAutomationEventHandlerTest extends MockedAuthorityTestEnv {
    @Autowired
    private AutomationService automationService;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private UserRepository userRepository;

    @Before
    public void init() {
        grantAllPermissions(ResourceType.ODC_AUTOMATION_RULE);
        CreateRuleReq createRuleReq = new CreateRuleReq();
        createRuleReq.setName("test");
        createRuleReq.setEventId(2L);
        createRuleReq.setEnabled(true);
        AutomationAction automationAction = new AutomationAction();
        automationAction.setAction("BindRole");
        automationAction.setArguments(ImmutableMap.of("roleId", 1));
        createRuleReq.setActions(Collections.singletonList(automationAction));

        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(ADMIN_USER_ID);

        AutomationCondition condition = new AutomationCondition();
        condition.setExpression("extra#department");
        condition.setObject("User");
        condition.setOperation("contains");
        condition.setValue("odc");
        createRuleReq.setConditions(Collections.singletonList(condition));
        automationService.create(createRuleReq);
    }

    @After
    public void clear() {
        userRepository.deleteAll();
        userRoleRepository.deleteAll();
    }

    @Test
    public void test_user_create_trigger_role_bind_event() {
        UserEntity user = createUser("test", "test");
        SpringContextUtil.publishEvent(new TriggerEvent(TriggerEvent.USER_CREATED, new User(user)));
        List<UserRoleEntity> userRoleEntities = userRoleRepository.findByUserIdAndRoleIdAndOrganizationId(user.getId(),
                ADMIN_ROLE_ID, user.getOrganizationId());
        Assert.assertTrue(CollectionUtils.isNotEmpty(userRoleEntities));
    }

    protected UserEntity createUser(String username, String accountName) {
        UserEntity entity = new UserEntity();
        entity.setName(username);
        entity.setAccountName(accountName);
        entity.setType(UserType.USER);
        entity.setPassword("123456");
        entity.setCipher(Cipher.BCRYPT);
        entity.setCreatorId(ADMIN_USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setBuiltIn(false);
        entity.setActive(true);
        entity.setEnabled(true);
        entity.setDescription("internal for unit test");
        Map<String, String> extraInfo = new HashMap<>();
        extraInfo.put("department", "odc");
        entity.setExtraPropertiesJson(JsonUtils.toJson(extraInfo));
        return userRepository.saveAndFlush(entity);
    }
}
