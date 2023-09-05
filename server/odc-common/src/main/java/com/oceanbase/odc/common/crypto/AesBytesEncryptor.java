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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * AES encryptor。 <br>
 * - use random IV <br>
 * - use 256 as default key length <br>
 * - salt was recommended but not strict required <br>
 * - while salt not set, use key as random seed for generate KeySecret while empty salt <br>
 * 
 * @author yizhou.xw
 * @version : AesBytesEncryptor.java, v 0.1 2020-04-27 15:01
 */
public class AesBytesEncryptor implements BytesEncryptor {

    private static final String FACTORY_INSTANCE = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5PADDING";
    private static final String SECRET_KEY_TYPE = "AES";
    private static final String NO_SALT_SECURE_RANDOM_INSTANCE = "SHA1PRNG";

    /**
     * 默认 KEY 长度
     */
    private static final int DEFAULT_KEY_LENGTH = 256;

    /**
     * IV 指的是 initialization vector，<br>
     * 在 CBC 模式中使用随机的初始化向量，可以使得同一个源每次加密得到的值是不一样的
     */
    private static final int IV_LENGTH = 16;

    /**
     * 迭代次数，越大则加解密成本越大，暴力破解耗时越久，一般不少于 1000
     */
    private static final int ITERATION_COUNT = 1024;

    /**
     * 秘钥长度，默认为 256
     */
    private final int keyLength;

    private final SecretKeySpec secretKey;
    private final Cipher encryptor;
    private final Cipher decryptor;

    public AesBytesEncryptor(String key, String salt, int keyLength) {
        Validate.notEmpty(key, "key");
        Validate.isTrue(keyLength > 0, "keyLength can not be negative");
        this.keyLength = keyLength;
        this.secretKey = newSecretKey(key, salt);
        this.encryptor = createCipher();
        this.decryptor = createCipher();
    }

    public AesBytesEncryptor(String key, String salt) {
        this(key, salt, DEFAULT_KEY_LENGTH);
    }

    @Override
    public synchronized byte[] encrypt(byte[] origin) {
        Validate.notNull(origin, "parameter 'origin' may not be null");
        // Generating random IV
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        initCipher(encryptor, Cipher.ENCRYPT_MODE, iv);
        byte[] encrypted = doFinal(encryptor, origin);
        return addIVToCipher(encrypted, iv);
    }

    @Override
    public synchronized byte[] decrypt(byte[] encrypted) {
        Validate.notNull(encrypted, "parameter 'encrypted' may not be null");
        Validate.isTrue(encrypted.length >= IV_LENGTH,
                "length of parameter 'encrypted' may not less than IV length " + IV_LENGTH);
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(encrypted, 0, iv, 0, IV_LENGTH);
        initCipher(decryptor, Cipher.DECRYPT_MODE, iv);
        byte[] original = doFinal(decryptor, encrypted);
        return Arrays.copyOfRange(original, IV_LENGTH, original.length);
    }

    private byte[] doFinal(Cipher cipher, byte[] input) {
        try {
            return cipher.doFinal(input);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalStateException(
                    "Unable to invoke Cipher due to illegal block size", e);
        } catch (BadPaddingException e) {
            throw new IllegalStateException("Unable to invoke Cipher due to bad padding",
                    e);
        }
    }

    private SecretKeySpec newSecretKey(String key, String salt) {
        try {
            if (StringUtils.isBlank(salt)) {
                SecureRandom random = SecureRandom.getInstance(NO_SALT_SECURE_RANDOM_INSTANCE);
                random.setSeed(key.getBytes());

                KeyGenerator keyGenerator = KeyGenerator.getInstance(SECRET_KEY_TYPE);
                keyGenerator.init(keyLength, random);

                SecretKey secretKey = keyGenerator.generateKey();
                return new SecretKeySpec(secretKey.getEncoded(), SECRET_KEY_TYPE);
            } else {
                SecretKeyFactory factory = SecretKeyFactory.getInstance(FACTORY_INSTANCE);
                KeySpec keySpec = new PBEKeySpec(key.toCharArray(), salt.getBytes(), ITERATION_COUNT, keyLength);
                SecretKey secretKey = factory.generateSecret(keySpec);
                return new SecretKeySpec(secretKey.getEncoded(), SECRET_KEY_TYPE);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Not a valid secret key", e);
        }
    }

    private void initCipher(Cipher cipher, int mode, byte[] iv) {
        try {
            cipher.init(mode, secretKey, new IvParameterSpec(iv));
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Not a valid secret key", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        }
    }

    private Cipher createCipher() {
        try {
            return Cipher.getInstance(CIPHER_INSTANCE);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalStateException("Should not happen", e);
        }
    }

    private byte[] addIVToCipher(byte[] encrypted, byte[] iv) {
        byte[] cipherWithIv = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, cipherWithIv, 0, iv.length);
        System.arraycopy(encrypted, 0, cipherWithIv, iv.length, encrypted.length);
        return cipherWithIv;
    }

}
