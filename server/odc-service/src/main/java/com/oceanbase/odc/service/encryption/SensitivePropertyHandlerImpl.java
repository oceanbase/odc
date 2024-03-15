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
package com.oceanbase.odc.service.encryption;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.RsaBytesEncryptor;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.metadb.config.SystemConfigEntity;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SensitivePropertyHandlerImpl implements SensitivePropertyHandler {
    private static final int ENCRYPTION_KEY_SIZE = 1024;
    private final String publicKey;
    private final TextEncryptor textEncryptor;
    private static final long TRY_LOCK_TIMEOUT_SECONDS = 5;
    private static final String LOCK_KEY = "ODC_ENCRYPTION_SECRET_LOCK_KEY";
    private static final String ENCRYPTION_PUBLIC_KEY_SYSTEM_CONFIG_KEY = "odc.system.security.encryption-public-key";
    private static final String ENCRYPTION_SECRET_KEY_SYSTEM_CONFIG_KEY = "odc.system.security.encryption-secret-key";

    public SensitivePropertyHandlerImpl(
            @Value("${odc.system.security.sensitive-property-encrypted:true}") Boolean sensitiveInputEncrypted,
            @Value("${odc.system.security.encryption-public-key:#{null}}") String encryptionPublicKey,
            @Value("${odc.system.security.encryption-secret-key:#{null}}") String encryptionSecretKey,
            SystemConfigService systemConfigService, JdbcLockRegistry jdbcLockRegistry) {
        if (sensitiveInputEncrypted) {
            try {
                if (Objects.nonNull(encryptionPublicKey) && Objects.nonNull(encryptionSecretKey)) {
                    this.publicKey = encryptionPublicKey;
                    this.textEncryptor = Encryptors.rsaBase64Decryptor(encryptionSecretKey);
                    return;
                }
                log.info("Try to lock odc encryption secret...");
                Lock lock = jdbcLockRegistry.obtain(LOCK_KEY);
                if (lock.tryLock(TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    try {
                        log.info("Successfully acquired the encryption secret lock");
                        Pair<String, String> existedKeyPair = queryPublicKeyToSecretKeyPair(systemConfigService);
                        if (existedKeyPair != null) {
                            this.publicKey = existedKeyPair.left;
                            this.textEncryptor = Encryptors.rsaBase64Decryptor(existedKeyPair.right);
                            return;
                        }
                        Pair<String, String> keyPair =
                                RsaBytesEncryptor.generateBase64EncodeKeyPair(ENCRYPTION_KEY_SIZE);
                        this.publicKey = keyPair.left;
                        this.textEncryptor = Encryptors.rsaBase64Decryptor(keyPair.right);
                        SystemConfigEntity publicKey = createSystemConfigEntity(ENCRYPTION_PUBLIC_KEY_SYSTEM_CONFIG_KEY,
                                keyPair.left, "ODC asymmetric encryption public key");
                        SystemConfigEntity secretKey = createSystemConfigEntity(ENCRYPTION_SECRET_KEY_SYSTEM_CONFIG_KEY,
                                keyPair.right, "ODC asymmetric encryption secret key");
                        systemConfigService.insert(Arrays.asList(publicKey, secretKey));
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.info(
                            "Failed to get encryption secret lock, try to get encryption secret from system configuration");
                    Pair<String, String> keyPair = queryPublicKeyToSecretKeyPair(systemConfigService);
                    if (Objects.nonNull(keyPair)) {
                        this.publicKey = keyPair.left;
                        this.textEncryptor = Encryptors.rsaBase64Decryptor(keyPair.right);
                    } else {
                        throw new RuntimeException("Failed to get encryption secret from system configuration");
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to init encryption secret, message={}", e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            this.publicKey = null;
            this.textEncryptor = Encryptors.empty();
        }
        log.info("SensitiveInputHandler initialized, sensitiveInputEncrypted={}", sensitiveInputEncrypted);
    }

    private boolean verifySystemConfig(List<Configuration> key) {
        return key != null && !key.isEmpty() && Objects.nonNull(key.get(0)) && Objects.nonNull(key.get(0).getValue());
    }

    private SystemConfigEntity createSystemConfigEntity(String key, String value, String description) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setKey(key);
        entity.setValue(value);
        entity.setDescription(description);
        return entity;
    }

    private Pair<String, String> queryPublicKeyToSecretKeyPair(SystemConfigService systemConfigService) {
        List<Configuration> publicKey = systemConfigService.queryByKeyPrefix(ENCRYPTION_PUBLIC_KEY_SYSTEM_CONFIG_KEY);
        List<Configuration> secretKey = systemConfigService.queryByKeyPrefix(ENCRYPTION_SECRET_KEY_SYSTEM_CONFIG_KEY);
        if (verifySystemConfig(publicKey) && verifySystemConfig(secretKey)) {
            return new Pair<>(publicKey.get(0).getValue(), secretKey.get(0).getValue());
        }
        return null;
    }

    @Override
    public String publicKey() {
        return this.publicKey;
    }

    @Override
    public String decrypt(String encryptedText) {
        return textEncryptor.decrypt(encryptedText);
    }

}
