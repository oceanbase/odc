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

import static org.mockito.ArgumentMatchers.eq;

import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.service.iam.model.UpdateRoleReq;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

/**
 * @author wenniu.ly
 * @date 2021/8/11
 */
public class RoleServiceForbiddenOperationTest extends MockedAuthorityTestEnv {
    @Autowired
    private RoleService roleService;

    @MockBean(name = "roleRepository")
    private RoleRepository roleRepository;

    @Before
    public void setUp() {
        RoleEntity adminEntity = new RoleEntity();
        adminEntity.setId(ADMIN_ROLE_ID);
        adminEntity.setName("system_admin");
        adminEntity.setBuiltIn(true);
        Mockito.when(roleRepository.findById(eq(ADMIN_ROLE_ID))).thenReturn(Optional.of(adminEntity));
        grantAllPermissions(ResourceType.ODC_ROLE, ResourceType.ODC_USER);
    }

    @After
    public void tearDown() {
        SecurityContextUtils.clear();
    }

    @Test(expected = UnsupportedException.class)
    public void testDeleteSystemAdmin() {
        roleService.delete(ADMIN_ROLE_ID);
    }

    @Test(expected = UnsupportedException.class)
    public void testUpdateSystemAdmin() {
        UpdateRoleReq updateRoleReq = new UpdateRoleReq();
        updateRoleReq.setName("update_role_name");
        roleService.update(ADMIN_ROLE_ID, updateRoleReq);
    }

    @Test(expected = UnsupportedException.class)
    public void testSetEnableSystemAdmin() {
        roleService.setEnabled(ADMIN_ROLE_ID, false);
    }
}
