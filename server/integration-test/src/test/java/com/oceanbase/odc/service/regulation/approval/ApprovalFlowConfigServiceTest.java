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
package com.oceanbase.odc.service.regulation.approval;

import java.util.ArrayList;
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
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalFlowConfigEntity;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalFlowConfigRepository;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalNodeConfigEntity;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalNodeConfigRepository;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalNodeConfig;

/**
 * @Author: Lebie
 * @Date: 2023/6/15 10:50
 * @Description: []
 */
public class ApprovalFlowConfigServiceTest extends ServiceTestEnv {
    @Autowired
    private ApprovalFlowConfigService approvalFlowConfigService;

    @Autowired
    private ApprovalFlowConfigRepository approvalFlowConfigRepository;

    @Autowired
    private ApprovalNodeConfigRepository approvalNodeConfigRepository;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private ResourceRoleService resourceRoleService;

    @Before
    public void setUp() {
        approvalFlowConfigRepository.deleteAll();
        approvalNodeConfigRepository.deleteAll();
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(1L);
        Mockito.when(resourceRoleService.findResourceRoleById(1L))
                .thenReturn(Optional.of(getResourceRole(1L, ResourceRoleName.OWNER)));
        Mockito.when(resourceRoleService.findResourceRoleById(2L))
                .thenReturn(Optional.of(getResourceRole(2L, ResourceRoleName.DBA)));
        Mockito.when(resourceRoleService.findResourceRoleById(3L))
                .thenReturn(Optional.of(getResourceRole(3L, ResourceRoleName.DEVELOPER)));
    }

    @After
    public void tearDown() {
        approvalFlowConfigRepository.deleteAll();
        approvalNodeConfigRepository.deleteAll();
    }

    @Test
    public void testCreateApprovalFlowConfig_Success() {
        ApprovalFlowConfig created = approvalFlowConfigService.create(getApprovalFlowConfig());
        Assert.assertEquals(3, created.getNodes().size());
        Assert.assertEquals(1L, created.getNodes().get(0).getResourceRoleId().longValue());
        Assert.assertEquals(2L, created.getNodes().get(1).getResourceRoleId().longValue());
    }

    @Test
    public void testDetailApprovalFlowConfig_Success() {
        ApprovalFlowConfig created = approvalFlowConfigService.create(getApprovalFlowConfig());
        ApprovalFlowConfig detail = approvalFlowConfigService.detail(created.getId());
        Assert.assertNotNull(detail);
    }

    @Test
    public void testListApprovalFlowConfig_Success() {
        approvalFlowConfigService.create(getApprovalFlowConfig());
        List<ApprovalFlowConfig> configs = approvalFlowConfigService.list();
        Assert.assertEquals(1, configs.size());
    }

    @Test
    public void testDeleteApprovalFlowConfig_Success() {
        ApprovalFlowConfig created = approvalFlowConfigService.create(getApprovalFlowConfig());
        approvalFlowConfigService.delete(created.getId());
        List<ApprovalFlowConfigEntity> configs = approvalFlowConfigRepository.findAll();
        List<ApprovalNodeConfigEntity> nodes = approvalNodeConfigRepository.findAll();
        Assert.assertEquals(0, configs.size());
        Assert.assertEquals(0, nodes.size());
    }

    @Test
    public void testUpdateApprovalFlowConfig_Success() {
        ApprovalFlowConfig created = approvalFlowConfigService.create(getApprovalFlowConfig());

        ApprovalFlowConfig updated = getApprovalFlowConfig();
        List<ApprovalNodeConfig> nodes = getApprovalNodes();
        nodes.get(0).setResourceRoleId(2L);
        updated.setNodes(nodes);

        ApprovalFlowConfig actual = approvalFlowConfigService.update(created.getId(), updated);
        Assert.assertEquals(2L, actual.getNodes().get(0).getResourceRoleId().longValue());
    }

    private ApprovalFlowConfig getApprovalFlowConfig() {
        ApprovalFlowConfig config = new ApprovalFlowConfig();
        config.setName("fake_config");
        config.setApprovalExpirationIntervalSeconds(1);
        config.setExecutionExpirationIntervalSeconds(1);
        config.setWaitExecutionExpirationIntervalSeconds(1);
        config.setNodes(getApprovalNodes());
        return config;
    }

    private List<ApprovalNodeConfig> getApprovalNodes() {
        List<ApprovalNodeConfig> nodes = new ArrayList<>();
        ApprovalNodeConfig first = new ApprovalNodeConfig();
        first.setExternalApproval(false);
        first.setResourceRoleId(1L);
        first.setAutoApproval(false);

        ApprovalNodeConfig next = new ApprovalNodeConfig();
        next.setExternalApproval(false);
        next.setResourceRoleId(2L);
        next.setAutoApproval(false);

        ApprovalNodeConfig last = new ApprovalNodeConfig();
        last.setExternalApproval(false);
        last.setResourceRoleId(3L);
        last.setAutoApproval(true);

        nodes.add(first);
        nodes.add(next);
        nodes.add(last);

        return nodes;
    }

    private ResourceRoleEntity getResourceRole(Long id, ResourceRoleName role) {
        ResourceRoleEntity entity = new ResourceRoleEntity();
        entity.setId(id);
        entity.setRoleName(role);
        entity.setResourceType(ResourceType.ODC_PROJECT);
        return entity;
    }

}
