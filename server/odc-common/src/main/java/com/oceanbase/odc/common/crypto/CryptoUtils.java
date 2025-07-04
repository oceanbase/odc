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

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Hex;

/**
 * @author yizhou.xw
 * @version : CryptoUtils.java, v 0.1 2021-08-10 11:24
 */
public class CryptoUtils {
    private static final int DEFAULT_SALT_SIZE = 16;

    public static String generateSalt() {
        return generateSaltWithSize(DEFAULT_SALT_SIZE);
    }

    public static String generateSaltWithSize(int size) {
        return UUID.randomUUID().toString().replaceAll("-", "")
                .substring(0, size).toLowerCase();
    }

    /**
     * Generate AES key with specified key size
     * 
     * @param keySize the key size in bits (128, 192, or 256)
     * @return hex string representation of the generated AES key
     */
    public static String generateAes(int keySize) {
        javax.crypto.KeyGenerator keyGenerator;
        try {
            keyGenerator = javax.crypto.KeyGenerator.getInstance("AES");
            keyGenerator.init(keySize);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No AES algorithm.");
        }
        SecretKey secretKey = keyGenerator.generateKey();
        return Hex.encodeHexString(secretKey.getEncoded());
    }

}
