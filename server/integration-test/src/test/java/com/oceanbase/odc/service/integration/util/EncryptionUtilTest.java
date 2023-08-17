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
package com.oceanbase.odc.service.integration.util;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.integration.model.Encryption;
import com.oceanbase.odc.service.integration.model.Encryption.EncryptionAlgorithm;

/**
 * @author gaoda.xy
 * @date 2023/4/19 10:27
 */
public class EncryptionUtilTest {
    @Test
    public void test_AES256_BASE64() {
        String raw = RandomStringUtils.random(256);
        Encryption encryption = createEncryption(EncryptionAlgorithm.AES256_BASE64);
        String encrypted = EncryptionUtil.encrypt(raw, encryption);
        Assert.assertNotEquals(raw, encrypted);
        String decrypted = EncryptionUtil.decrypt(encrypted, encryption);
        Assert.assertEquals(raw, decrypted);
    }

    @Test
    public void test_AES192_BASE64_4A() {
        String raw = RandomStringUtils.random(256);
        Encryption encryption = createEncryption(EncryptionAlgorithm.AES192_BASE64_4A);
        String encrypted = EncryptionUtil.encrypt(raw, encryption);
        Assert.assertNotEquals(raw, encrypted);
        String decrypted = EncryptionUtil.decrypt(encrypted, encryption);
        Assert.assertEquals(raw, decrypted);
    }

    @Test
    public void test_RAW() {
        String raw = RandomStringUtils.random(256);
        Encryption encryption = createEncryption(EncryptionAlgorithm.RAW);
        String encrypted = EncryptionUtil.encrypt(raw, encryption);
        Assert.assertEquals(raw, encrypted);
        String decrypted = EncryptionUtil.decrypt(encrypted, encryption);
        Assert.assertEquals(raw, decrypted);
    }


    private Encryption createEncryption(EncryptionAlgorithm algorithm) {
        switch (algorithm) {
            case RAW:
                return new Encryption(true, EncryptionAlgorithm.RAW, null);
            case AES256_BASE64:
                return new Encryption(true, EncryptionAlgorithm.AES256_BASE64,
                        RandomStringUtils.randomAlphanumeric(32));
            case AES192_BASE64_4A:
                return new Encryption(true, EncryptionAlgorithm.AES192_BASE64_4A,
                        RandomStringUtils.randomAlphanumeric(24));
            default:
                throw new UnsupportedException();
        }
    }
}
