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
import org.junit.Before;
import org.junit.Test;

abstract class EncryptorTest {

    abstract TextEncryptor instance();

    private TextEncryptor instance;

    @Before
    public void setUp() throws Exception {
        this.instance = instance();
    }

    @Test
    public void encryptDecrypt() {
        String origin = "abcd1234";

        String encrypt = instance.encrypt(origin);
        String decrypt = instance.decrypt(encrypt);

        Assert.assertEquals(origin, decrypt);
    }

    @Test
    public void encryptDecrypt_long_string() {
        String origin = "abcd1234_abcd1234_abcd1234_abcd1234_abcd1234_abcd1234_abcd1234_abcd1234_abcd1234_abcd1234";

        String encrypt = instance.encrypt(origin);
        String decrypt = instance.decrypt(encrypt);

        Assert.assertEquals(origin, decrypt);
    }
}
