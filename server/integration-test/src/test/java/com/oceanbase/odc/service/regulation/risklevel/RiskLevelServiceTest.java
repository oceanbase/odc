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
package com.oceanbase.odc.service.regulation.risklevel;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelEntity;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelRepository;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelStyle;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigService;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;

/**
 * @Author: Lebie
 * @Date: 2023/6/16 11:10
 * @Description: []
 */
public class RiskLevelServiceTest extends ServiceTestEnv {
    @Autowired
    @Qualifier("RiskLevelServiceFrom420")
    private RiskLevelService riskLevelService;

    @Autowired
    private RiskLevelRepository riskLevelRepository;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private ApprovalFlowConfigService approvalFlowConfigService;

    @Before
    public void setUp() {
        riskLevelRepository.deleteAll();
        riskLevelRepository.deleteAll();
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(1L);
        Mockito.when(approvalFlowConfigService.findById(1L)).thenReturn(getApprovalFlowConfig(1L));
        Mockito.when(approvalFlowConfigService.findById(2L)).thenReturn(getApprovalFlowConfig(2L));
        Mockito.when(approvalFlowConfigService.exists(1L, 2L)).thenReturn(true);
    }

    @After
    public void tearDown() {
        riskLevelRepository.deleteAll();
        riskLevelRepository.deleteAll();
    }

    @Test
    public void testList_Success() {
        riskLevelRepository.saveAndFlush(getEntity());

        List<RiskLevel> actual = riskLevelService.list();
        Assert.assertEquals(1, actual.size());
    }

    @Test
    public void testDetail_Success() {
        RiskLevelEntity saved = riskLevelRepository.saveAndFlush(getEntity());

        RiskLevel actual = riskLevelService.detail(saved.getId());
        Assert.assertNotNull(actual);
    }

    @Test
    public void testUpdate_Success() {
        RiskLevelEntity old = riskLevelRepository.saveAndFlush(getEntity());
        RiskLevel updated = getModel();
        updated.setApprovalFlowConfigId(2L);
        RiskLevel actual = riskLevelService.update(old.getId(), updated);
        Assert.assertEquals(2L, actual.getApprovalFlowConfigId().longValue());
    }



    private RiskLevelEntity getEntity() {
        RiskLevelEntity entity = new RiskLevelEntity();
        entity.setLevel(1);
        entity.setApprovalFlowConfigId(1L);
        entity.setOrganizationId(1L);
        entity.setName("high risk level");
        entity.setDescription("high risk level");
        entity.setStyle(RiskLevelStyle.RED);
        return entity;
    }

    private RiskLevel getModel() {
        RiskLevel model = new RiskLevel();
        model.setLevel(1);
        model.setApprovalFlowConfigId(1L);
        model.setOrganizationId(1L);
        model.setName("high risk level");
        model.setDescription("high risk level");
        model.setStyle(RiskLevelStyle.RED);
        return model;
    }

    private ApprovalFlowConfig getApprovalFlowConfig(Long id) {
        ApprovalFlowConfig flow = new ApprovalFlowConfig();
        flow.setId(id);
        return flow;
    }
}
