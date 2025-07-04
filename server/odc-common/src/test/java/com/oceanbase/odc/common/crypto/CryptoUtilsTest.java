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

public class CryptoUtilsTest {

    @Test
    public void generateSalt_ExpectLength16() {
        String salt = CryptoUtils.generateSalt();
        Assert.assertEquals(16, salt.length());
    }

    @Test
    public void generateAes_256Bit_ExpectLength64() {
        String aesKey = CryptoUtils.generateAes(256);
        Assert.assertEquals(64, aesKey.length()); // 256 bits = 32 bytes = 64 hex chars
    }

    @Test
    public void generateAes_192Bit_ExpectLength48() {
        String aesKey = CryptoUtils.generateAes(192);
        Assert.assertEquals(48, aesKey.length()); // 192 bits = 24 bytes = 48 hex chars
    }

    @Test
    public void generateAes_128Bit_ExpectLength32() {
        String aesKey = CryptoUtils.generateAes(128);
        Assert.assertEquals(32, aesKey.length()); // 128 bits = 16 bytes = 32 hex chars
    }

    @Test
    public void generateAes_DifferentCalls_DifferentKeys() {
        String key1 = CryptoUtils.generateAes(256);
        String key2 = CryptoUtils.generateAes(256);
        Assert.assertNotEquals(key1, key2);
    }

    @Test
    public void generateAes_ValidHexString() {
        String aesKey = CryptoUtils.generateAes(256);
        Assert.assertTrue(aesKey.matches("[0-9a-f]+"));
    }
}
