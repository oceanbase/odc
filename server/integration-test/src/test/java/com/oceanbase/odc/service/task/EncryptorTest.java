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
package com.oceanbase.odc.service.task;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.jasypt.AccessEnvironmentJasyptEncryptorConfigProperties;
import com.oceanbase.odc.service.task.jasypt.DefaultJasyptEncryptor;
import com.oceanbase.odc.service.task.jasypt.JasyptEncryptorConfigProperties;

/**
 * @author yaobin
 * @date 2024-03-29
 * @since 4.2.4
 */
public class EncryptorTest {
    private final String algorithm = "PBEWithHmacSHA512AndAES_128";
    private final String salt = "jasypt";
    private final String password = "abc123@!";

    @Before
    public void init() {
        System.setProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SALT, salt);
        System.setProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM, algorithm);
        System.setProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_PREFIX, "ENC(");
        System.setProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_SUFFIX, ")");
    }

    @Test
    public void testJasyptDecryptEncryptedText() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(salt);
        encryptor.setAlgorithm(algorithm);
        encryptor.setIvGenerator(new RandomIvGenerator());
        String encryptedText = "ENC(" +
                encryptor.encrypt(password) + ")";

        JasyptEncryptorConfigProperties properties = new AccessEnvironmentJasyptEncryptorConfigProperties();

        Assert.assertEquals(password, new DefaultJasyptEncryptor(properties)
                .decrypt(encryptedText));
    }

    @Test
    public void testJasyptDecryptPlainText() {
        JasyptEncryptorConfigProperties properties = new AccessEnvironmentJasyptEncryptorConfigProperties();
        Assert.assertEquals(password, new DefaultJasyptEncryptor(properties)
                .decrypt(password));
    }
}
