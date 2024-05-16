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

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.util.EncodeUtils;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/8/22 19:30
 */
public class RsaBytesEncryptor implements BytesEncryptor {

    private static final String ALGORITHM_NAME = "RSA";
    private static final String ALGORITHM_NAME_WITH_PADDING = "RSA/ECB/PKCS1Padding";
    private static final int DEFAULT_KEY_SIZE = 1024;

    private final RsaEncryptorType type;
    private final Cipher encryptor;
    private final Cipher decryptor;
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public RsaBytesEncryptor(@NonNull RsaEncryptorType type, String publicKeyBase64, String privateKeyBase64) {
        this.type = type;
        if (type == RsaEncryptorType.ENCRYPT_MODE) {
            Validate.notBlank(publicKeyBase64, "The public key is required");
            this.encryptor = createCipher();
            this.decryptor = null;
            this.publicKey = transformToPublicKey(publicKeyBase64);
            this.privateKey = null;
        } else if (type == RsaEncryptorType.DECRYPT_MODE) {
            Validate.notBlank(privateKeyBase64, "The private key is required");
            this.encryptor = null;
            this.decryptor = createCipher();
            this.privateKey = transformToPrivateKey(privateKeyBase64);
            this.publicKey = null;
        } else {
            Validate.notBlank(publicKeyBase64, "The public key is required");
            this.encryptor = createCipher();
            this.publicKey = transformToPublicKey(publicKeyBase64);
            Validate.notBlank(privateKeyBase64, "The private key is required");
            this.decryptor = createCipher();
            this.privateKey = transformToPrivateKey(privateKeyBase64);
        }
    }

    @Override
    public synchronized byte[] encrypt(byte[] origin) {
        if (type == RsaEncryptorType.DECRYPT_MODE) {
            throw new IllegalStateException("The encryptor only support decrypt");
        }
        if (encryptor == null) {
            throw new IllegalStateException("The encryptor is required but null");
        }
        initCipher(encryptor, Cipher.ENCRYPT_MODE, publicKey);
        return doFinal(encryptor, origin);
    }

    @Override
    public synchronized byte[] decrypt(byte[] encrypted) {
        if (type == RsaEncryptorType.ENCRYPT_MODE) {
            throw new IllegalStateException("The encryptor only support encrypt");
        }
        if (decryptor == null) {
            throw new IllegalStateException("The decryptor is required but null");
        }
        initCipher(decryptor, Cipher.DECRYPT_MODE, privateKey);
        return doFinal(decryptor, encrypted);
    }

    public static Pair<String, String> generateBase64EncodeKeyPair() {
        return generateBase64EncodeKeyPair(DEFAULT_KEY_SIZE);
    }

    public static Pair<String, String> generateBase64EncodeKeyPair(int keySize) {
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        }
        keyPairGenerator.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String privateKeyBase64 = EncodeUtils.base64EncodeToString(privateKey.getEncoded());
        String publicKeyBase64 = EncodeUtils.base64EncodeToString(publicKey.getEncoded());
        return new Pair<>(publicKeyBase64, privateKeyBase64);
    }

    private Cipher createCipher() {
        try {
            return Cipher.getInstance(ALGORITHM_NAME_WITH_PADDING);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalStateException("Should not happen", e);
        }
    }

    private void initCipher(Cipher cipher, int mode, Key key) {
        try {
            cipher.init(mode, key);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Not a valid key", e);
        }
    }

    private byte[] doFinal(Cipher cipher, byte[] input) {
        try {
            return cipher.doFinal(input);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalStateException("Unable to invoke Cipher due to illegal block size", e);
        } catch (BadPaddingException e) {
            throw new IllegalStateException("Unable to invoke Cipher due to bad padding", e);
        }
    }

    private RSAPublicKey transformToPublicKey(String publicKeyBase64Encoded) {
        byte[] publicKeyDecoded = EncodeUtils.base64DecodeFromString(publicKeyBase64Encoded);
        try {
            KeyFactory keyFactory = createKeyFactory();
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyDecoded));
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Not a valid public key", e);
        }
    }

    private RSAPrivateKey transformToPrivateKey(String privateKeyBase64Encoded) {
        byte[] privateKeyDecoded = EncodeUtils.base64DecodeFromString(privateKeyBase64Encoded);
        try {
            KeyFactory keyFactory = createKeyFactory();
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyDecoded));
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Not a valid private key", e);
        }
    }

    private KeyFactory createKeyFactory() {
        try {
            return KeyFactory.getInstance(ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        }
    }

    public enum RsaEncryptorType {
        ENCRYPT_MODE,
        DECRYPT_MODE,
        BOTH_MODE
    }

}
