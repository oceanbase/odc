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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.Validate;

/**
 * AES_192 CBC mode bytes encryptor with specific initial vector and no secret key salt. This
 * encryptor is only used in CMCC-4A integration scenario.
 * 
 * @author gaoda.xy
 * @date 2023/4/4 11:35
 */
public class Cmcc4ABytesEncryptor implements BytesEncryptor {
    private static final String CIPHER_TYPE_AES = "AES";
    private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5PADDING";
    private static final String DEFAULT_IV = "a1b2c3d4e5f6g7h8";
    private final SecretKeySpec secretKey;

    public Cmcc4ABytesEncryptor(String secretKey) {
        Validate.notEmpty(secretKey, "secretKey");
        this.secretKey = new SecretKeySpec(secretKey.getBytes(), CIPHER_TYPE_AES);
    }

    @Override
    public byte[] encrypt(byte[] origin) {
        Validate.notNull(origin, "parameter 'origin' may not be null");
        return doFinal(origin, Cipher.ENCRYPT_MODE);
    }

    @Override
    public byte[] decrypt(byte[] encrypted) {
        Validate.notNull(encrypted, "parameter 'encrypted' may not be null");
        return doFinal(encrypted, Cipher.DECRYPT_MODE);
    }

    private byte[] doFinal(byte[] bytes, int mode) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(DEFAULT_IV.getBytes());
            cipher.init(mode, secretKey, ivParameterSpec);
            return cipher.doFinal(bytes);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new IllegalArgumentException("Not a valid encryption algorithm", e);
        } catch (NoSuchPaddingException e) {
            throw new IllegalStateException("Should not happen", e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Not a valid secret key", e);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalStateException("Unable to invoke Cipher due to illegal block size", e);
        } catch (BadPaddingException e) {
            throw new IllegalStateException("Unable to invoke Cipher due to bad padding", e);
        }
    }
}
