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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.metadb.iam.UserOrganizationEntity;
import com.oceanbase.odc.metadb.iam.UserOrganizationRepository;

/**
 * @Author: Lebie
 * @Date: 2023/5/5 16:09
 * @Description: []
 */
public class UserOrganizationServiceTest extends AuthorityTestEnv {
    @Autowired
    private UserOrganizationService userOrganizationService;

    @Autowired
    private UserOrganizationRepository userOrganizationRepository;

    @Before
    public void setUp() {
        userOrganizationRepository.deleteAll();
    }

    @After
    public void tearDown() {
        userOrganizationRepository.deleteAll();
    }

    @Test
    public void testUserBelongsToOrganization_ReturnTrue() {
        userOrganizationRepository.save(getUserOrganizationEntity(1L, 1L));

        Assert.assertTrue(userOrganizationService.userBelongsToOrganization(1L, 1L));
    }

    @Test
    public void testUserBelongsToOrganization_ReturnFalse() {
        userOrganizationRepository.save(getUserOrganizationEntity(1L, 1L));

        Assert.assertFalse(userOrganizationService.userBelongsToOrganization(1L, 2L));
    }

    private UserOrganizationEntity getUserOrganizationEntity(Long userId, Long organizationId) {
        UserOrganizationEntity entity = new UserOrganizationEntity();
        entity.setUserId(userId);
        entity.setOrganizationId(organizationId);
        return entity;
    }
}
