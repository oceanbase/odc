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

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.service.connection.ConnectionService;

public class UserPermissionServiceTest extends AuthorityTestEnv {

    @Autowired
    private UserPermissionService userPermissionService;
    @MockBean
    private ConnectionService connectionService;

    @Before
    public void setUp() {
        deleteAll();
    }

    @After
    public void clear() {
        deleteAll();
    }

    @Test
    public void test_bindUserAndPermission_Internal() {
        UserEntity user = createUser("user", ORGANIZATION_ID);
        userPermissionService.bindUserAndConnectionAccessPermission(user.getId(), null, "connect", 1L);
        List<UserPermissionEntity> userPermission = userPermissionRepository.findByUserId(user.getId());
        Assert.assertEquals(1, userPermission.size());
    }


    private UserEntity createUser(String name, Long organizationId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setName(name);
        userEntity.setAccountName(name);
        userEntity.setPassword("123456");
        userEntity.setCreatorId(ADMIN_USER_ID);
        userEntity.setEnabled(true);
        userEntity.setBuiltIn(false);
        userEntity.setType(UserType.USER);
        userEntity.setOrganizationId(organizationId);
        userEntity.setCipher(Cipher.RAW);
        userEntity.setActive(true);
        return userRepository.saveAndFlush(userEntity);
    }

}
