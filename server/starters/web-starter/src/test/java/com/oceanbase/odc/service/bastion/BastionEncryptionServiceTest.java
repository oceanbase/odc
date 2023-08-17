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
package com.oceanbase.odc.service.bastion;

import static org.mockito.Mockito.when;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.oceanbase.odc.service.bastion.model.BastionProperties;
import com.oceanbase.odc.service.bastion.model.BastionProperties.EncryptionAlgorithm;
import com.oceanbase.odc.service.bastion.model.BastionProperties.EncryptionProperties;

@RunWith(MockitoJUnitRunner.class)
public class BastionEncryptionServiceTest {
    @InjectMocks
    private BastionEncryptionService bastionEncryptionService;

    @Mock
    private BastionProperties bastionProperties;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void encrypt_NotEnabled_same() {
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEnabled(false);
        when(bastionProperties.getEncryption()).thenReturn(encryptionProperties);

        String encrypt = bastionEncryptionService.encrypt("hello");

        Assert.assertEquals("hello", encrypt);
    }

    @Test
    public void encrypt_EnabledAlgorithmRAW_same() {
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEnabled(true);
        encryptionProperties.setAlgorithm(EncryptionAlgorithm.RAW);
        when(bastionProperties.getEncryption()).thenReturn(encryptionProperties);

        String encrypt = bastionEncryptionService.encrypt("hello");

        Assert.assertEquals("hello", encrypt);
    }

    @Test
    public void encrypt_EnabledAlgorithmAES_encrypted() {
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEnabled(true);
        encryptionProperties.setAlgorithm(EncryptionAlgorithm.AES256_BASE64);
        encryptionProperties.setSecret("somesecret");
        when(bastionProperties.getEncryption()).thenReturn(encryptionProperties);

        String encrypt = bastionEncryptionService.encrypt("hello");

        Assert.assertNotEquals("hello", encrypt);
    }

    @Test
    public void encrypt_EnabledAlgorithmCMCC4A_encrypted() {
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEnabled(true);
        encryptionProperties.setAlgorithm(EncryptionAlgorithm.CMCC4A);
        encryptionProperties.setSecret(RandomStringUtils.randomAlphanumeric(192 / 8));
        when(bastionProperties.getEncryption()).thenReturn(encryptionProperties);

        String encrypt = bastionEncryptionService.encrypt("hello");

        Assert.assertNotEquals("hello", encrypt);
    }

}
