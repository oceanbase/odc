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
package com.oceanbase.odc.service.objectstorage.cloud.model;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * 加密算法枚举
 *
 * @author yh263208
 * @date 2020-11-20 13:45
 * @since ODC_release_2.3
 */
public enum EncryptAlgorithm {
    /**
     * AES加密算法
     */
    AES {
        /**
         * 密钥长度
         */
        private static final int KEY_SIZE = 256;
        /**
         * 加密算法
         */
        private static final String ALGORITHM = "AES";
        /**
         * 随机数生成器算法名称
         */
        private static final String RNG_ALGORITHM = "SHA1PRNG";

        /**
         * 密钥生成方法
         *
         * @param key 密钥
         * @return 返回密钥对象
         * @throws NoSuchAlgorithmException 算法配置错误可能会抛出异常
         */
        private SecretKey getKey(byte[] key) throws NoSuchAlgorithmException {
            SecureRandom random = SecureRandom.getInstance(RNG_ALGORITHM);
            random.setSeed(key);
            KeyGenerator generator = KeyGenerator.getInstance(ALGORITHM);
            generator.init(KEY_SIZE, random);
            return generator.generateKey();
        }

        /**
         * 加密方法
         *
         * @param plainBytes 待加密内容
         * @param key 密钥
         * @return 返回加密后的内容
         */
        @Override
        public byte[] encrypt(byte[] plainBytes, byte[] key) {
            try {
                SecretKey secretKey = getKey(key);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                return cipher.doFinal(plainBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 解密方法
         *
         * @param ciperBytes 已经加密内容
         * @param key 密钥
         * @return 返回解密后的内容
         */
        @Override
        public byte[] decrypt(byte[] ciperBytes, byte[] key) {
            try {
                SecretKey secretKey = getKey(key);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                return cipher.doFinal(ciperBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 编码方法，该方法用于对字节数组进行编码
         *
         * @param input 输入字节
         * @return 输出字符串
         */
        private String encode(byte[] input) {
            Base64.Encoder encoder = Base64.getEncoder();
            return encoder.encodeToString(input);
        }

        /**
         * 解码方法，用于字符串到字节数组的解码
         *
         * @param input 输入字符串
         * @return 返回解码后的字节数组
         */
        private byte[] decode(String input) {
            Base64.Decoder decoder = Base64.getDecoder();
            return decoder.decode(input);
        }

        /**
         * 加密方法
         *
         * @param plainBytes 待加密内容
         * @param key 密钥
         * @return 返回加密后的内容
         */
        @Override
        public String encrypt(String plainBytes, String key, String encode) {
            try {
                SecretKey secretKey = getKey(key.getBytes(encode));
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                return encode(cipher.doFinal(plainBytes.getBytes(encode)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 解密方法
         *
         * @param ciperBytes 已经加密内容
         * @param key 密钥
         * @return 返回解密后的内容
         */
        @Override
        public String decrypt(String ciperBytes, String key, String encode) {
            try {
                SecretKey secretKey = getKey(key.getBytes(encode));
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                return new String(cipher.doFinal(decode(ciperBytes)), encode);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    /**
     * 加密方法
     *
     * @param plainBytes 待加密内容
     * @param key 密钥
     * @return 返回加密后的内容
     */
    abstract public byte[] encrypt(byte[] plainBytes, byte[] key);

    /**
     * 解密方法
     *
     * @param ciperBytes 已经加密内容
     * @param key 密钥
     * @return 返回解密后的内容
     */
    abstract public byte[] decrypt(byte[] ciperBytes, byte[] key);

    /**
     * 加密方法
     *
     * @param plainBytes 待加密内容
     * @param key 密钥
     * @return 返回加密后的内容
     */
    abstract public String encrypt(String plainBytes, String key, String encode);

    /**
     * 解密方法
     *
     * @param ciperBytes 已经加密内容
     * @param key 密钥
     * @return 返回解密后的内容
     */
    abstract public String decrypt(String ciperBytes, String key, String encode);
}
