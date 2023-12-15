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
package com.oceanbase.odc.service.iam;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.UserResourceRole;

/**
 * @Author: Lebie
 * @Date: 2023/5/5 15:43
 * @Description: []
 */
public class ResourceRoleServiceTest extends MockedAuthorityTestEnv {
    @Autowired
    private ResourceRoleService resourceRoleService;

    @Autowired
    private UserResourceRoleRepository userResourceRoleRepository;

    @MockBean
    private ResourceRoleRepository resourceRoleRepository;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @Before
    public void setUp() {
        userResourceRoleRepository.deleteAll();
    }

    @After
    public void tearDown() {
        userResourceRoleRepository.deleteAll();
    }

    @Test
    public void testSaveAll_Success() {
        Mockito.when(resourceRoleRepository.findAll()).thenReturn(getResourceRoleEntity());
        Mockito.when(resourceRoleRepository.findById(1L)).thenReturn(Optional.of(getResourceRoleEntity().get(0)));
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);

        resourceRoleService.saveAll(Arrays.asList(getProjectOwner()));

        int actual = userResourceRoleRepository.findByUserId(1L).size();
        Assert.assertEquals(1, actual);
    }

    @Test
    public void testListByResourceId_Success() {
        Mockito.when(resourceRoleRepository.findAll()).thenReturn(getResourceRoleEntity());
        Mockito.when(resourceRoleRepository.findById(Mockito.anyLong()))
                .thenReturn(Optional.of(getResourceRoleEntity().get(0)));
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        resourceRoleService.saveAll(Arrays.asList(getProjectOwner()));

        int actual = resourceRoleService.listByResourceId(1L).size();
        Assert.assertEquals(1, actual);
    }

    @Test
    public void testListByOrganizationIdAndUserId_Success() {
        Mockito.when(resourceRoleRepository.findAll()).thenReturn(getResourceRoleEntity());
        Mockito.when(resourceRoleRepository.findById(1L)).thenReturn(Optional.of(getResourceRoleEntity().get(0)));
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        resourceRoleService.saveAll(Arrays.asList(getProjectOwner()));

        int actual = resourceRoleService.listByOrganizationIdAndUserId(1L, 1L).size();
        Assert.assertEquals(1, actual);
    }

    @Test
    public void testDeleteById_Success() {
        Mockito.when(resourceRoleRepository.findAll()).thenReturn(getResourceRoleEntity());
        Mockito.when(resourceRoleRepository.findById(1L)).thenReturn(Optional.of(getResourceRoleEntity().get(0)));
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        resourceRoleService.saveAll(Arrays.asList(getProjectOwner()));

        int actual = resourceRoleService.deleteByResourceId(1L);
        Assert.assertEquals(1, actual);
    }

    private UserResourceRole getProjectOwner() {
        UserResourceRole projectOwner = new UserResourceRole();
        projectOwner.setUserId(1L);
        projectOwner.setResourceId(1L);
        projectOwner.setResourceRole(ResourceRoleName.OWNER);
        return projectOwner;
    }

    private List<ResourceRoleEntity> getResourceRoleEntity() {
        ResourceRoleEntity entity = new ResourceRoleEntity();
        entity.setId(1L);
        entity.setRoleName(ResourceRoleName.OWNER);
        entity.setResourceType(ResourceType.ODC_PROJECT);
        return Arrays.asList(entity);
    }
}
