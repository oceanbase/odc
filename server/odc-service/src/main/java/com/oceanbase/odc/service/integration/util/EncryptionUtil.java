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
package com.oceanbase.odc.service.integration.util;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.service.integration.model.Encryption;

/**
 * Encryption utility for integration usage.
 * 
 * @author gaoda.xy
 * @date 2023/4/3 17:44
 */
public class EncryptionUtil {

    private static final LoadingCache<Encryption, TextEncryptor> encryptorCache = Caffeine.newBuilder().maximumSize(100)
            .expireAfterAccess(10, TimeUnit.MINUTES).build(EncryptionUtil::getEncryptor);

    public static String encrypt(String plainText, Encryption encryption) {
        return Objects.requireNonNull(encryptorCache.get(encryption)).encrypt(plainText);
    }

    public static String decrypt(String encryptedText, Encryption encryption) {
        return Objects.requireNonNull(encryptorCache.get(encryption)).decrypt(encryptedText);
    }

    private static TextEncryptor getEncryptor(Encryption encryption) {
        if (!encryption.getEnabled()) {
            return Encryptors.empty();
        }
        switch (encryption.getAlgorithm()) {
            case AES256_BASE64:
                return Encryptors.aes256Base64(encryption.getSecret());
            case AES192_BASE64_4A:
                return Encryptors.aesBase64Cmcc4A(encryption.getSecret());
            case RAW:
            default:
                return Encryptors.empty();
        }
    }
}
