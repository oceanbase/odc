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
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.iam.model.UpdateUserReq;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

/**
 * @author wenniu.ly
 * @date 2021/8/11
 */
public class UserServiceForbiddenOperationTest extends MockedAuthorityTestEnv {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Before
    public void setUp() {
        UserEntity adminEntity = new UserEntity();
        adminEntity.setType(UserType.USER);
        adminEntity.setId(ADMIN_USER_ID);
        adminEntity.setOrganizationId(ORGANIZATION_ID);
        adminEntity.setAccountName("admin");
        adminEntity.setBuiltIn(true);
        Mockito.when(userRepository.findById(eq(ADMIN_USER_ID))).thenReturn(Optional.of(adminEntity));
        grantAllPermissions(ResourceType.ODC_USER, ResourceType.ODC_ROLE);
    }

    @After
    public void tearDown() {
        SecurityContextUtils.clear();
    }

    @Test(expected = UnsupportedException.class)
    public void testDeleteAdmin() {
        userService.delete(ADMIN_USER_ID);
    }

    @Test(expected = UnsupportedException.class)
    public void testUpdateAdmin() {
        userService.update(ADMIN_USER_ID, new UpdateUserReq());
    }

    @Test(expected = UnsupportedException.class)
    public void testSetEnableAdmin() {
        userService.setEnabled(ADMIN_USER_ID, false);
    }
}
