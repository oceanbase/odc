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

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.crypto.RsaBytesEncryptor.RsaEncryptorType;
import com.oceanbase.odc.common.lang.Pair;

/**
 * @author gaoda.xy
 * @date 2023/8/23 14:57
 */
public class RsaBytesEncryptorTest extends EncryptorTest {

    private static final Pair<String, String> keyPair = RsaBytesEncryptor.generateBase64EncodeKeyPair();

    @Override
    TextEncryptor instance() {
        return Encryptors.rsaBase64(keyPair.left, keyPair.right);
    }

    @Test
    public void test_bothMode() {
        RsaBytesEncryptor encryptor = new RsaBytesEncryptor(RsaEncryptorType.BOTH_MODE, keyPair.left, keyPair.right);
        String origin = "This is the origin string";
        byte[] encrypted = encryptor.encrypt(origin.getBytes(StandardCharsets.UTF_8));
        String decrypted = new String(encryptor.decrypt(encrypted), StandardCharsets.UTF_8);
        Assert.assertEquals(origin, decrypted);
    }

    @Test
    public void test_onlyEncryptor_and_onlyDecryptor() {
        RsaBytesEncryptor encryptor = new RsaBytesEncryptor(RsaEncryptorType.ENCRYPT_MODE, keyPair.left, null);
        RsaBytesEncryptor decryptor = new RsaBytesEncryptor(RsaEncryptorType.DECRYPT_MODE, null, keyPair.right);
        String origin = "This is the origin string";
        byte[] encrypted = encryptor.encrypt(origin.getBytes(StandardCharsets.UTF_8));
        String decrypted = new String(decryptor.decrypt(encrypted), StandardCharsets.UTF_8);
        Assert.assertEquals(origin, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void test_onlyEncryptor_throwException_whenDecryptor() {
        RsaBytesEncryptor encryptor = new RsaBytesEncryptor(RsaEncryptorType.ENCRYPT_MODE, keyPair.left, null);
        String origin = "This is the origin string";
        byte[] encrypted = encryptor.encrypt(origin.getBytes(StandardCharsets.UTF_8));
        encryptor.decrypt(encrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void test_onlyDecryptor_throwException_whenEncryptor() {
        RsaBytesEncryptor decryptor = new RsaBytesEncryptor(RsaEncryptorType.DECRYPT_MODE, null, keyPair.right);
        String origin = "This is the origin string";
        decryptor.encrypt(origin.getBytes(StandardCharsets.UTF_8));
    }

}
