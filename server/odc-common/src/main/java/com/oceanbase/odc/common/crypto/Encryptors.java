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

import com.oceanbase.odc.common.encode.ByteArrayToBase64Converter;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author yizhou.xw
 * @version : Encryptors.java, v 0.1 2021-3-29 18:02
 */
@Slf4j
public class Encryptors {

    /**
     * AES-256 带 Salt 模式
     */
    public static BytesEncryptor aes(String key, String salt) {
        return new AesBytesEncryptor(key, salt);
    }

    /**
     * AES 带 Salt 模式, keyLength 可指定
     */
    public static BytesEncryptor aes(String key, String salt, int keyLength) {
        return new AesBytesEncryptor(key, salt, keyLength);
    }

    /**
     * AES-256 BASE64 无 Salt 模式
     */
    public static TextEncryptor aes256Base64(String key) {
        return aesBase64(key, null);
    }

    /**
     * AES-256 BASE64 带 Salt 模式
     */
    public static TextEncryptor aesBase64(String key, String salt) {
        return new TextEncryptorWrapper(aes(key, salt), new ByteArrayToBase64Converter());
    }

    /**
     * AES BASE64 带 Salt 模式, keyLength 可指定
     */
    public static TextEncryptor aesBase64(String key, String salt, int keyLength) {
        return new TextEncryptorWrapper(aes(key, salt, keyLength), new ByteArrayToBase64Converter());
    }

    public static BytesEncryptor blowFishZeroPadding(String key) {
        return new BlowFishZeroPaddingBytesEncryptor(key);
    }

    public static TextEncryptor blowFishZeroPaddingBase64(String key) {
        return new TextEncryptorWrapper(blowFishZeroPadding(key), new ByteArrayToBase64Converter());
    }

    /**
     * AES-192 BASE64 不带 Salt 模式, 适用于 4A 集成场景
     */
    public static TextEncryptor aesBase64Cmcc4A(String key) {
        return new TextEncryptorWrapper(new Cmcc4ABytesEncryptor(key), new ByteArrayToBase64Converter());
    }

    /**
     * 空实现，不处理加解密
     */
    public static TextEncryptor empty() {
        return new TextEncryptor() {
            @Override
            public String encrypt(String plainText) {
                return plainText;
            }

            @Override
            public String decrypt(String encryptedText) {
                return encryptedText;
            }
        };
    }

}
