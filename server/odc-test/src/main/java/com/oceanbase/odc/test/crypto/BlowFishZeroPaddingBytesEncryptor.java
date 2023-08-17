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
package com.oceanbase.odc.test.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.common.primitives.Bytes;

/**
 * @author gaoda.xy
 * @date 2023/2/16 21:55
 */
public class BlowFishZeroPaddingBytesEncryptor implements BytesEncryptor {
    private final byte[] password;

    /**
     * @param password key string
     */
    public BlowFishZeroPaddingBytesEncryptor(String password) {
        Validate.notEmpty("password is empty or null");
        this.password = password.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] encrypt(byte[] origin) {
        Validate.notNull(origin, "null input for encrypt");
        try {
            Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
            SecretKeySpec key = new SecretKeySpec(password, "Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            origin = zeroPad(origin);
            return cipher.doFinal(origin);
        } catch (Exception e) {
            String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
            throw new RuntimeException(rootCauseMessage);
        }
    }

    @Override
    public byte[] decrypt(byte[] encrypted) {
        Validate.notNull(encrypted, "null input for decrypt");
        try {
            Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
            SecretKeySpec key = new SecretKeySpec(password, "Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(encrypted);
            return zeroUnpad(decrypted);
        } catch (Exception e) {
            String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
            throw new RuntimeException(rootCauseMessage);
        }
    }

    private byte[] zeroPad(byte[] origin) {
        int minLength = origin.length % 8 == 0 ? origin.length : (origin.length / 8 + 1) * 8;
        origin = Bytes.ensureCapacity(origin, minLength, 0);
        return origin;
    }

    private byte[] zeroUnpad(byte[] decrypted) {
        int end = decrypted.length;
        while (end > 0) {
            if (decrypted[end - 1] == 0) {
                end--;
            } else {
                break;
            }
        }
        return Arrays.copyOf(decrypted, end);
    }
}
