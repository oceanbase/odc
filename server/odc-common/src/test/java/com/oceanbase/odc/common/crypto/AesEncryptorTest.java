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
package com.oceanbase.odc.common.crypto;

import org.junit.Assert;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : AesEncryptDecryptTest.java, v 0.1 2020-04-27 15:23
 */
@Slf4j
public class AesEncryptorTest extends EncryptorTest {

    @Override
    TextEncryptor instance() {
        return Encryptors.aesBase64("123654", "salt-a");
    }

    /**
     * encrypted.length()=128
     */
    @Test
    public void passwordSize64_EncryptedSizeLessThan256() {
        TextEncryptor encryptor = Encryptors.aesBase64("123456789012345678901234567890123456789012345678901234567890",
                "salt1234salt1234");
        String encrypted = encryptor.encrypt("1234567890123456789012345678901234567890123456789012345678901234");
        log.info("encrypted.length()={}", encrypted.length());
        Assert.assertTrue(encrypted.length() < 256);
    }

    @Test
    public void sameKeyTwice_Different() {
        TextEncryptor encryptor = Encryptors.aesBase64("1234567890123456_1", "salt123-a");
        String encrypted1 = encryptor.encrypt("123654");
        String encrypted2 = encryptor.encrypt("123654");
        Assert.assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    public void differentKey_Different() {
        String encrypt1 = Encryptors.aesBase64("1234567890123456_1", "salt123-a").encrypt("123654");
        String encrypt2 = Encryptors.aesBase64("1234567890123456_2", "salt123-a").encrypt("123654");
        log.info("encrypt1={}", encrypt1);
        log.info("encrypt2={}", encrypt2);
        Assert.assertNotEquals(encrypt1, encrypt2);
    }

    @Test
    public void differentSalt_Different() {
        String encrypt1 = Encryptors.aesBase64("1234567890123456_1", "salt123-a").encrypt("123654");
        String encrypt2 = Encryptors.aesBase64("1234567890123456_1", "salt123-b").encrypt("123654");
        log.info("encrypt1={}", encrypt1);
        log.info("encrypt2={}", encrypt2);
        Assert.assertNotEquals(encrypt1, encrypt2);
    }

    @Test(expected = IllegalStateException.class)
    public void differentSalt_Exception() {
        TextEncryptor saltA = Encryptors.aesBase64("1234567890123456_1", "salt123-a");
        TextEncryptor saltB = Encryptors.aesBase64("1234567890123456_1", "salt123-b");
        String result = saltB.decrypt(saltA.encrypt("123456"));
        log.info("decrypt result={}", result);
        if (!"123456".equals(result)) {
            throw new IllegalStateException("Decrypt result does not match original");
        }
    }

    @Test
    public void emptySalt() {
        TextEncryptor encryptor = Encryptors.aesBase64("1234567890123456_1", "");
        String result = encryptor.decrypt(encryptor.encrypt("123456"));
        Assert.assertEquals("123456", result);
    }

    @Test
    public void keyLength192_emptySalt() {
        TextEncryptor encryptor = Encryptors.aesBase64("1234567890123456_1", "", 192);
        String encrypted = encryptor.encrypt("123456");
        String result = encryptor.decrypt(encrypted);
        Assert.assertEquals("123456", result);
    }
}
