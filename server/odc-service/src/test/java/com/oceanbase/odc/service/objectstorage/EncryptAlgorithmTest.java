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
package com.oceanbase.odc.service.objectstorage;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.service.objectstorage.cloud.model.EncryptAlgorithm;

import lombok.extern.slf4j.Slf4j;

/**
 * AES算法测试
 *
 * @author yh263208
 * @date 2020-11-19 21:27
 * @since ODC_release_2.3
 */
@Slf4j
public class EncryptAlgorithmTest {
    /**
     * 原字符串
     */
    private String testMessage = "Hello,world";
    /**
     * 密钥
     */
    private String testKey = UUID.randomUUID().toString();
    private EncryptAlgorithm algorithm = EncryptAlgorithm.AES;

    /**
     * 测试加密算法
     */
    @Test
    public void testEncrypy() {
        byte[] ciperBytes = algorithm.encrypt(testMessage.getBytes(), testKey.getBytes());
        Assert.assertNotEquals(null, ciperBytes);
    }

    /**
     * 测试解密算法
     */
    @Test
    public void testDecrypt() {
        byte[] ciperBytes = algorithm.encrypt(testMessage.getBytes(), testKey.getBytes());
        Assert.assertNotEquals(null, ciperBytes);
        byte[] result = algorithm.decrypt(ciperBytes, testKey.getBytes());
        Assert.assertEquals(new String(result), testMessage);
    }

    /**
     * for generate encrypted value, used in aliyun env <br>
     * should use real value
     */
    @Test
    @Ignore
    public void onlineEncrypt() {
        String encyptKey = "env_encrypt_key";
        String keyId = algorithm.decrypt(
                "encrypted_value", encyptKey, "UTF-8");
        String keySecret = algorithm.decrypt(
                "encrypted_value", encyptKey, "UTF-8");

        System.out.printf("keyId: %s \n", keyId);
        System.out.printf("keySecret: %s \n", keySecret);
    }

    /**
     * for decrypt value, used in aliyun env
     */
    @Test
    @Ignore
    public void onlineDecrypt() {
        String encyptKey = "env_encrypt_key";
        String encryptedKeyId = algorithm.decrypt(
                "raw_text", encyptKey, "UTF-8");
        String encryptedKeySecret = algorithm.decrypt(
                "raw_text", encyptKey, "UTF-8");

        System.out.printf("encryptedKeyId: %s \n", encryptedKeyId);
        System.out.printf("encryptedKeySecret: %s \n", encryptedKeySecret);
    }
}
