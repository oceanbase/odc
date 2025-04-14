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
package com.oceanbase.odc.metadb.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import com.google.common.collect.Sets;
import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.FlowInstanceService.MergedDbCreatedData;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.UserResourceRole;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter.ApplyDatabase;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalNodeConfig;
import com.oceanbase.odc.service.regulation.risklevel.RiskDetectService;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.BooleanOperator;
import com.oceanbase.odc.service.regulation.risklevel.model.ConditionExpression;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRuleCondition;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRuleConditionGroup;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;

/**
 * @Author: ysj
 * @Date: 2025/3/31 15:53
 * @Since: 4.3.4
 * @Description:
 */
@SuppressWarnings("all")
public class MergeFlowInstanceTest extends ServiceTestEnv {

    @SpyBean
    private FlowInstanceService flowInstanceService;
    @SpyBean
    private DatabaseService databaseService;
    @SpyBean
    private RiskLevelService riskLevelService;
    @SpyBean
    private RiskDetectService riskDetectService;
    @SpyBean
    private ApprovalFlowConfigSelector approvalFlowConfigSelector;
    @SpyBean
    private ResourceRoleService resourceRoleService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    private List<Long> databaseIds = new ArrayList<Long>() {
        {
            add(1L);
            add(2L);
            add(3L);
        }
    };
    private List<Database> databases;
    private static Long organizationId;

    @Before
    public void setUp() throws Exception {
        organizationId = authenticationFacade.currentOrganizationId();
        /**
         * id = 1 -> default risk level id = 2 -> high risk level
         */
        List<RiskLevel> riskLevels = IntStream.range(1, 3).mapToObj(i -> {
            RiskLevel riskLevel = new RiskLevel();
            riskLevel.setId((long) i);
            riskLevel.setOrganizationId(organizationId);
            riskLevel.setLevel(i - 1);
            riskLevel.setApprovalFlowConfigId((long) i);
            ApprovalFlowConfig approvalFlowConfig = new ApprovalFlowConfig();
            approvalFlowConfig.setId(1L);
            approvalFlowConfig.setBuiltIn(true);
            approvalFlowConfig.setOrganizationId(organizationId);
            approvalFlowConfig.setNodes(IntStream.range(1, 3).mapToObj(j -> {
                ApprovalNodeConfig approvalNodeConfig = new ApprovalNodeConfig();
                approvalNodeConfig.setId((long) (j + (i - 1) * 2L));
                approvalNodeConfig.setSequenceNumber(j - 1);
                approvalNodeConfig.setResourceRoleId((long) j);
                if ((j & 1) == 1) {
                    approvalNodeConfig.setResourceRoleName(ResourceRoleName.OWNER.name());
                } else {
                    approvalNodeConfig.setResourceRoleName(ResourceRoleName.DBA.name());
                }
                return approvalNodeConfig;
            }).collect(Collectors.toList()));
            riskLevel.setApprovalFlowConfig(approvalFlowConfig);
            return riskLevel;
        }).collect(Collectors.toList());

        buildDatabase();
        buildResourceRole();

        Mockito.doReturn(this.databases).when(databaseService)
                .listDatabasesDetailsByIds(new LinkedHashSet<>(databaseIds));
        Mockito.doReturn(riskLevels).when(riskLevelService).listByOrganizationId(organizationId);
        Mockito.doReturn(riskLevels.get(riskLevels.size() - 1)).when(riskLevelService).findHighestRiskLevel();
        Mockito.doReturn(riskLevels.get(0)).when(riskLevelService).findDefaultRiskLevel();
        Mockito.doReturn(buildRiskDetectRule(riskLevels)).when(riskDetectService)
                .listAllByOrganizationId(organizationId);
    }

    private List<RiskDetectRule> buildRiskDetectRule(List<RiskLevel> riskLevels) {
        // default risk detect rule
        RiskDetectRule defaultRiskDetectRule = new RiskDetectRule();
        defaultRiskDetectRule.setId(1L);
        defaultRiskDetectRule.setOrganizationId(organizationId);
        defaultRiskDetectRule.setRiskLevelId(riskLevels.get(0).getId());
        defaultRiskDetectRule.setRiskLevel(riskLevels.get(0));
        RiskDetectRuleConditionGroup defaultRiskOrConditionGroup = new RiskDetectRuleConditionGroup();
        RiskDetectRuleCondition defaultRiskDetectRuleConditionWithDbName = new RiskDetectRuleCondition();
        defaultRiskDetectRuleConditionWithDbName.setExpression(ConditionExpression.DATABASE_NAME);
        defaultRiskDetectRuleConditionWithDbName.setOperator("EQUALS");
        defaultRiskDetectRuleConditionWithDbName.setValue("AUTO_ODC");
        defaultRiskOrConditionGroup.setChildren(Collections.singletonList(defaultRiskDetectRuleConditionWithDbName));
        defaultRiskDetectRule.setRootNode(defaultRiskOrConditionGroup);
        // high risk detect rule
        RiskDetectRule heightRiskDetectRule = new RiskDetectRule();
        heightRiskDetectRule.setId(2L);
        heightRiskDetectRule.setOrganizationId(organizationId);
        heightRiskDetectRule.setRiskLevelId(riskLevels.get(1).getId());
        heightRiskDetectRule.setRiskLevel(riskLevels.get(1));
        RiskDetectRuleConditionGroup heightRiskOrConditionGroup = new RiskDetectRuleConditionGroup();
        heightRiskOrConditionGroup.setBooleanOperator(BooleanOperator.OR);
        RiskDetectRuleCondition heightRiskDetectRuleConditionWithEnvName = new RiskDetectRuleCondition();
        heightRiskDetectRuleConditionWithEnvName.setExpression(ConditionExpression.ENVIRONMENT_NAME);
        heightRiskDetectRuleConditionWithEnvName.setOperator("EQUALS");
        heightRiskDetectRuleConditionWithEnvName
                .setValue("${com.oceanbase.odc.builtin-resource.collaboration.environment.sit.name}");
        RiskDetectRuleCondition copiedDefaultRiskDetectRuleConditionWithDbName = JsonUtils.fromJson(
                JsonUtils.toJson(defaultRiskDetectRuleConditionWithDbName), RiskDetectRuleCondition.class);
        copiedDefaultRiskDetectRuleConditionWithDbName.setValue("AUTO_ODC_DB");
        heightRiskOrConditionGroup.setChildren(Arrays.asList(copiedDefaultRiskDetectRuleConditionWithDbName,
                heightRiskDetectRuleConditionWithEnvName));
        heightRiskDetectRule.setRootNode(heightRiskOrConditionGroup);

        return Arrays.asList(defaultRiskDetectRule, heightRiskDetectRule);
    }

    private void buildDatabase() {
        this.databases = databaseIds.stream().map(id -> {
            Database database = new Database();
            database.setId(id);
            database.setName("AUTO_ODC");
            Environment environment = new Environment();
            environment.setId(id);
            if ((id & 1) == 1) {
                environment.setName("${com.oceanbase.odc.builtin-resource.collaboration.environment.sit.pname}");
            } else {
                environment.setName("${com.oceanbase.odc.builtin-resource.collaboration.environment.sit.name}");
            }
            database.setEnvironment(environment);
            return database;
        }).collect(Collectors.toList());
        this.databases.get(this.databases.size() - 1).setName("AUTO_ODC_DB");
    }

    private void buildResourceRole() {
        ResourceRoleEntity resourceRole1 = new ResourceRoleEntity();
        resourceRole1.setId(1L);
        resourceRole1.setResourceType(ResourceType.ODC_DATABASE);
        resourceRole1.setRoleName(ResourceRoleName.OWNER);
        resourceRole1.setCreateTime(new Date());
        ResourceRoleEntity resourceRole2 = new ResourceRoleEntity();
        resourceRole2.setId(2L);
        resourceRole2.setResourceType(ResourceType.ODC_PROJECT);
        resourceRole2.setRoleName(ResourceRoleName.DBA);
        resourceRole2.setCreateTime(new Date());
        Mockito.doReturn(Arrays.asList(resourceRole1, resourceRole2)).when(resourceRoleService).listResourceRoleByIds(
                Sets.newHashSet(1L, 2L));

        UserResourceRole userResourceRole1 = new UserResourceRole();
        userResourceRole1.setResourceId(1L);
        userResourceRole1.setResourceRoleId(1L);
        userResourceRole1.setResourceRole(ResourceRoleName.OWNER);
        userResourceRole1.setResourceType(ResourceType.ODC_DATABASE);
        userResourceRole1.setUserId(1L);

        UserResourceRole userResourceRole2 = new UserResourceRole();
        userResourceRole1.setResourceId(2L);
        userResourceRole1.setResourceRoleId(2L);
        userResourceRole1.setResourceRole(ResourceRoleName.DBA);
        userResourceRole1.setResourceType(ResourceType.ODC_PROJECT);
        userResourceRole1.setUserId(2L);

        Mockito.doReturn(Arrays.asList(userResourceRole1, userResourceRole2)).when(resourceRoleService)
                .listByResourceIdentifierIn(
                        Sets.newHashSet("1:1", "2:2"));
    }

    private List<RiskLevelDescriber> buildRiskLevelDescriber() {
        return databases.stream().map(d -> {
            return RiskLevelDescriber.of(d, TaskType.APPLY_DATABASE_PERMISSION.name());
        }).collect(Collectors.toList());
    }

    @Test
    public void testSelectHeightestRiskLevel() {
        List<RiskLevelDescriber> describers = buildRiskLevelDescriber();
        Map<RiskLevelDescriber, RiskLevel> describer2RiskLevel = approvalFlowConfigSelector.batchSelect(describers);
        ArrayList<RiskLevelDescriber> riskLevelDescribers = new ArrayList<>(describer2RiskLevel.keySet());
        RiskLevelDescriber describer = riskLevelDescribers.get(0);
        Assert.assertEquals(0, (int) describer2RiskLevel.get(describer).getLevel());
        describer = riskLevelDescribers.get(1);
        Assert.assertEquals(1, (int) describer2RiskLevel.get(describer).getLevel());
        describer = riskLevelDescribers.get(2);
        Assert.assertEquals(1, (int) describer2RiskLevel.get(describer).getLevel());
    }

    @Test
    public void testMergeDb() {
        CreateFlowInstanceReq createFlowInstanceReq = new CreateFlowInstanceReq();
        createFlowInstanceReq.setTaskType(TaskType.APPLY_DATABASE_PERMISSION);
        ApplyDatabaseParameter applyDatabaseParameter = new ApplyDatabaseParameter();
        applyDatabaseParameter.setApplyReason("testMergeApplyDatabaseFlowInstance");
        List<ApplyDatabase> applyDatabases = databaseIds.stream().map(id -> {
            ApplyDatabase applyDatabase = new ApplyDatabase();
            applyDatabase.setId(id);
            return applyDatabase;
        }).collect(Collectors.toList());
        applyDatabaseParameter.setDatabases(applyDatabases);
        createFlowInstanceReq.setParameters(applyDatabaseParameter);
        createFlowInstanceReq.setProjectId(2L);

        // final create flow instace count is 2, dabaseIds=[1] and databaseIds=[2,3]
        List<MergedDbCreatedData> mergedDbCreatedDatas =
                flowInstanceService.mergeDbWhenApplyPermissionOfDbOrTable(
                        createFlowInstanceReq);
        Assert.assertEquals(2, mergedDbCreatedDatas.size());

        MergedDbCreatedData createdData = mergedDbCreatedDatas.get(0);
        Assert.assertEquals(1L, (long) new ArrayList<>(createdData.getCandidateResourceIds()).get(0));
        Assert.assertEquals(0, (int) createdData.getRiskLevel().getLevel());

        createdData = mergedDbCreatedDatas.get(1);
        Assert.assertEquals(Sets.newHashSet(2L, 3L), createdData.getCandidateResourceIds());
        Assert.assertEquals(1, (int) createdData.getRiskLevel().getLevel());
    }
}

