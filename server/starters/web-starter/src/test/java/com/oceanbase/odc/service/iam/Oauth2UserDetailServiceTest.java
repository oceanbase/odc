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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.VerifyException;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.iam.auth.SsoUserDetailService;
import com.oceanbase.odc.service.iam.auth.oauth2.MappingResult;
import com.oceanbase.odc.service.iam.model.Organization;

public class Oauth2UserDetailServiceTest extends ServiceTestEnv {

    @Autowired
    private SsoUserDetailService   SSOUserDetailService;
    @MockBean
    private OrganizationRepository organizationRepository;
    @MockBean
    private OrganizationService organizationService;
    @Autowired
    private UserRepository userRepository;

    @Before
    public void setUp() {
        Organization organization = new Organization();
        organization.setId(1L);
        Mockito.when(organizationService.create(Mockito.any())).thenReturn(organization);
        Mockito.when(organizationRepository.findByName(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(organizationRepository.findById(Mockito.anyLong()))
                .thenReturn(Optional.of(organization.toEntity()));
    }

    @After
    public void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    public void test_GetOrCreateUser_Success() {
        final Map<String, Object> userInfoMap = new HashMap<>();
        MappingResult mappingResult = new MappingResult();
        mappingResult.setUserAccountName("test");
        mappingResult.setSourceUserInfoMap(userInfoMap);
        mappingResult.setOrganizationId(1L);

        SSOUserDetailService.getOrCreateUser(mappingResult);
        Optional<UserEntity> user = userRepository.findByAccountName("test");
        Assert.assertTrue(user.isPresent());
    }

    @Test
    public void test_GetOrCreateUser_CreateUserRollBack() {
        try {
            final Map<String, Object> userInfoMap = new HashMap<>();
            MappingResult mappingResult = new MappingResult();
            mappingResult.setUserAccountName("test");
            mappingResult.setSourceUserInfoMap(userInfoMap);
            mappingResult.setOrganizationId(1L);
            SSOUserDetailService.getOrCreateUser(mappingResult);
        } catch (VerifyException e) {
            Optional<UserEntity> user = userRepository.findByAccountName("test");
            // could not find user if transaction rollback
            Assert.assertFalse(user.isPresent());
        }
    }


}
