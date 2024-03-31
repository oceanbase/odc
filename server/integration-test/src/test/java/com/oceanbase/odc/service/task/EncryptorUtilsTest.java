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
import org.junit.Test;

import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;

/**
 * @author yaobin
 * @date 2024-03-29
 * @since 4.2.4
 */
public class EncryptorUtilsTest {

    @Test
    public void testJasyptDecryptEncryptedText() {
        String salt = "jasypt";
        String password = "abc123@!";
        System.setProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_PASSWORD, salt);
        System.setProperty(JobEnvKeyConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM_KEY,
                JobConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM_NAME);
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(salt);
        encryptor.setAlgorithm(JobConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM_NAME);
        encryptor.setIvGenerator(new RandomIvGenerator());
        String encryptedText = JobConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM_PREFIX
                + encryptor.encrypt(password) + JobConstants.ODC_PROPERTY_ENCRYPTION_ALGORITHM_SUFFIX;
        Assert.assertEquals(password, JobUtils.decrypt(encryptedText));
    }

    @Test
    public void testJasyptDecryptPlainText() {
        String password = "abc123@!";
        Assert.assertEquals(password, JobUtils.decrypt(password));
    }
}
