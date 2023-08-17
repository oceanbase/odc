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
package com.oceanbase.odc.service.encryption;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

public class EncryptionFacadeImplTest {

    @InjectMocks
    private EncryptionFacade encryptionFacade = new EncryptionFacadeImpl();

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private UserService userService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(authenticationFacade.currentUserId()).thenReturn(1L);
        UserEntity userEntity = new UserEntity();
        userEntity.setType(UserType.USER);
        userEntity.setPassword("some-pwd");
        when(userService.nullSafeGet(1L)).thenReturn(userEntity);
    }

    @Test
    public void encryptThenDecrypt_Match() {
        String salt = encryptionFacade.generateSalt();

        String encrypted = encryptionFacade.encryptByCurrentUserPassword("sometext", salt);
        String decrypted = encryptionFacade.decryptByCurrentUserPassword(encrypted, salt);

        Assert.assertEquals("sometext", decrypted);
    }

    @Test
    public void generateSalt_Length_16() {
        String salt = encryptionFacade.generateSalt();
        Assert.assertEquals(16, salt.length());
    }
}
