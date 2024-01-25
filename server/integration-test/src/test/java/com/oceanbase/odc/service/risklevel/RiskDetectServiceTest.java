/*
 * Copyright (c) 2024 OceanBase.
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
package com.oceanbase.odc.service.risklevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskDetectRuleRepository;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.risklevel.RiskDetectService;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.BaseTreeNode;
import com.oceanbase.odc.service.regulation.risklevel.model.BooleanOperator;
import com.oceanbase.odc.service.regulation.risklevel.model.ConditionExpression;
import com.oceanbase.odc.service.regulation.risklevel.model.NodeType;
import com.oceanbase.odc.service.regulation.risklevel.model.QueryRiskDetectRuleParams;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRuleCondition;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRuleConditionGroup;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;

/**
 * @Author: Lebie
 * @Date: 2023/6/16 13:59
 * @Description: []
 */
public class RiskDetectServiceTest extends ServiceTestEnv {
    @Autowired
    private RiskDetectService riskDetectService;

    @Autowired
    private RiskDetectRuleRepository riskDetectRuleRepository;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private RiskLevelService riskLevelService;

    @MockBean
    private UserService userService;

    @Before
    public void setUp() {
        riskDetectRuleRepository.deleteAll();
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(1L);
        Mockito.when(riskLevelService.exists(1L, 1L)).thenReturn(true);
        Mockito.when(riskLevelService.findById(Mockito.anyLong())).thenReturn(getRiskLevel());
        Mockito.when(userService.nullSafeGet(Mockito.anyLong())).thenReturn(getUserEntity());
    }

    @After
    public void tearDown() {
        riskDetectRuleRepository.deleteAll();
    }

    @Test
    public void testCreate_Success() {
        RiskDetectRule actual = riskDetectService.create(getRule());
        Assert.assertNotNull(actual.getRootNode());
    }

    @Test
    public void testDetail_Success() {
        RiskDetectRule created = riskDetectService.create(getRule());
        RiskDetectRule actual = riskDetectService.detail(created.getId());
        Assert.assertEquals(created.getId(), actual.getId());
    }

    @Test
    public void testList_Success() {
        riskDetectService.create(getRule());
        QueryRiskDetectRuleParams params = QueryRiskDetectRuleParams.builder().riskLevelId(1L).build();
        List<RiskDetectRule> actual = riskDetectService.list(params);
        Assert.assertEquals(1, actual.size());
    }

    @Test
    public void testUpdate_Success() {
        RiskDetectRule created = riskDetectService.create(getRule());

        RiskDetectRule update = getRule();

        RiskDetectRuleCondition condition = new RiskDetectRuleCondition();
        condition.setOperator("contains");
        condition.setExpression(ConditionExpression.ENVIRONMENT_ID);
        condition.setValue("1");
        update.setRootNode(condition);

        RiskDetectRule actual = riskDetectService.update(created.getId(), update);
        Assert.assertEquals(actual.getRootNode().getType(), NodeType.CONDITION);
    }

    @Test
    public void testEqualsConditionEvaluate_ReturnTrue() {
        RiskDetectRuleCondition condition = new RiskDetectRuleCondition();
        condition.setExpression(ConditionExpression.ENVIRONMENT_ID);
        condition.setOperator("equals");
        condition.setValue("1");
        RiskLevelDescriber describer = RiskLevelDescriber.builder().environmentId("1").build();

        Assert.assertTrue(condition.evaluate(describer));
    }

    @Test
    public void testNotEqualsConditionEvaluate_ReturnFalse() {
        RiskDetectRuleCondition condition = new RiskDetectRuleCondition();
        condition.setExpression(ConditionExpression.ENVIRONMENT_ID);
        condition.setOperator("not_equals");
        condition.setValue("1");
        RiskLevelDescriber describer = RiskLevelDescriber.builder().environmentId("1").build();

        Assert.assertFalse(condition.evaluate(describer));
    }

    @Test
    public void testContainsConditionEvaluate_ReturnTrue() {
        RiskDetectRuleCondition condition = new RiskDetectRuleCondition();
        condition.setExpression(ConditionExpression.DATABASE_NAME);
        condition.setOperator("contains");
        condition.setValue("abc_");
        RiskLevelDescriber describer = RiskLevelDescriber.builder().databaseName("abc_d").build();

        Assert.assertTrue(condition.evaluate(describer));
    }

    @Test
    public void testNotContainsConditionEvaluate_ReturnTrue() {
        RiskDetectRuleCondition condition = new RiskDetectRuleCondition();
        condition.setExpression(ConditionExpression.PROJECT_NAME);
        condition.setOperator("not_contains");
        condition.setValue("project_a");
        RiskLevelDescriber describer = RiskLevelDescriber.builder().projectName("project_b").build();
        Assert.assertTrue(condition.evaluate(describer));
    }

    @Test
    public void testInConditionEvaluate_ReturnTrue() {
        RiskDetectRuleCondition condition = new RiskDetectRuleCondition();
        condition.setExpression(ConditionExpression.PROJECT_NAME);
        condition.setOperator("in");
        condition.setValue(Arrays.asList("projectA", "projectB"));
        RiskLevelDescriber describer = RiskLevelDescriber.builder().projectName("projectA").build();
        Assert.assertTrue(condition.evaluate(describer));
    }

    @Test
    public void testNotInConditionEvaluate_ReturnTrue() {
        RiskDetectRuleCondition condition = new RiskDetectRuleCondition();
        condition.setExpression(ConditionExpression.PROJECT_NAME);
        condition.setOperator("not_in");
        condition.setValue(Arrays.asList("projectA", "projectB"));
        RiskLevelDescriber describer = RiskLevelDescriber.builder().projectName("projectC").build();
        Assert.assertTrue(condition.evaluate(describer));
    }

    @Test
    public void testConditionGroupEvaluate_ReturnTrue() {
        RiskDetectRuleConditionGroup group = getRootNode();
        RiskLevelDescriber describer = RiskLevelDescriber.builder().environmentId("1").databaseName("db1").build();
        Assert.assertTrue(group.evaluate(describer));
    }

    @Test

    public void testConditionGroupEvaluate_ReturnFalse() {
        RiskDetectRuleConditionGroup group = getRootNode();
        RiskLevelDescriber describer =
                RiskLevelDescriber.builder().environmentId("1").databaseName("db3").taskType("async").build();
        Assert.assertFalse(group.evaluate(describer));
    }


    private RiskDetectRule getRule() {
        RiskDetectRule rule = new RiskDetectRule();
        rule.setName("test_rule");
        rule.setRiskLevelId(1L);
        rule.setRootNode(getRootNode());
        return rule;
    }

    private RiskDetectRuleConditionGroup getRootNode() {
        /**
         * where environment_id = 1 and (task_type not in("export", "import") or database_name in ("db1",
         * "db2"))
         */
        RiskDetectRuleConditionGroup root = new RiskDetectRuleConditionGroup();
        root.setBooleanOperator(BooleanOperator.AND);

        List<BaseTreeNode> baseNodes = new ArrayList<>();

        RiskDetectRuleCondition condition = new RiskDetectRuleCondition();
        condition.setExpression(ConditionExpression.ENVIRONMENT_ID);
        condition.setOperator("EQUALS");
        condition.setValue("1");
        baseNodes.add(condition);

        RiskDetectRuleConditionGroup group = new RiskDetectRuleConditionGroup();
        group.setBooleanOperator(BooleanOperator.OR);
        RiskDetectRuleCondition condition1 = new RiskDetectRuleCondition();
        condition1.setExpression(ConditionExpression.TASK_TYPE);
        condition1.setOperator("NOT_IN");
        condition1.setValue(Arrays.asList("EXPORT", "IMPORT"));

        RiskDetectRuleCondition condition2 = new RiskDetectRuleCondition();
        condition2.setExpression(ConditionExpression.DATABASE_NAME);
        condition2.setOperator("IN");
        condition2.setValue(Arrays.asList("db1", "db2"));
        group.setChildren(Collections.singletonList(condition2));
        baseNodes.add(group);

        root.setChildren(baseNodes);

        return root;
    }

    private Optional<RiskLevel> getRiskLevel() {
        RiskLevel riskLevel = new RiskLevel();
        riskLevel.setId(1L);
        return Optional.of(riskLevel);
    }

    private UserEntity getUserEntity() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setName("whatever_name");
        userEntity.setAccountName("whatever_account_name");
        return userEntity;
    }

}
