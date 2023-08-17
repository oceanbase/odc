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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.RoleService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

public class ActionCheckerTest extends MockedAuthorityTestEnv {
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private RoleService roleService;
    @MockBean
    private ConnectionService connectionService;
    @Autowired
    private AutomationActionChecker checker;

    @Before
    public void setUp() {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setOrganizationId(ORGANIZATION_ID);
        Mockito.when(roleService.nullSafeGet(Mockito.anyLong())).thenReturn(roleEntity);

        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setOrganizationId(ORGANIZATION_ID);
        Mockito.when(connectionService.getWithoutPermissionCheck(Mockito.anyLong())).thenReturn(connectionConfig);
    }

    @Test
    public void test_CheckBindRole_Success() {
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);

        Map<String, Object> bindRoleAction = createBindRole();
        checker.check("BindRole", bindRoleAction);
    }

    @Test(expected = AccessDeniedException.class)
    public void test_CheckBindRole_Failed() {
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(2L);

        Map<String, Object> bindRoleAction = createBindRole();
        checker.check("BindRole", bindRoleAction);

    }

    @Test
    public void test_CheckBindPermission_Success() {
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);

        Map<String, Object> bindPermissionMap = createBindPermission();
        checker.check("BindPermission", bindPermissionMap);
    }

    @Test(expected = AccessDeniedException.class)
    public void test_CheckBindPermission_Failed() {
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(2L);

        Map<String, Object> bindPermissionMap = createBindPermission();
        checker.check("BindPermission", bindPermissionMap);
    }

    private Map<String, Object> createBindRole() {
        Map<String, Object> bindRoleAction = new HashMap<>();
        bindRoleAction.put("roleId", 1);
        return bindRoleAction;
    }

    private Map<String, Object> createBindPermission() {
        Map<String, Object> bindPermissionMap = new HashMap<>();
        bindPermissionMap.put("resourceType", "ODC_CONNECTION");
        bindPermissionMap.put("resourceId", 1);
        bindPermissionMap.put("actions", "apply");
        return bindPermissionMap;
    }
}
