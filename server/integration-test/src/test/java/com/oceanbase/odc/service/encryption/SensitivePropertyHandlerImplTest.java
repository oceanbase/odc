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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.service.config.SystemConfigService;

public class SensitivePropertyHandlerImplTest extends ServiceTestEnv {
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;
    @Value("${odc.system.security.encryption-public-key:#{null}}")
    private String encryptionPublicKey;
    @Value("${odc.system.security.encryption-secret-key:#{null}}")
    private String encryptionSecretKey;

    @Test
    public void encryptionSecret_Enabled_NotEmpty() {
        String s = getEnabledHandler().publicKey();
        Assert.assertFalse(s.isEmpty());
    }

    @Test
    public void encryptionSecret_Disabled_Null() {
        String s = getDisabledHandler().publicKey();
        Assert.assertNull(s);
    }

    @Test
    public void encryptDecrypt_Matches() {
        SensitivePropertyHandlerImpl sensitivePropertyHandler = getEnabledHandler();
        TextEncryptor encryptor = Encryptors.rsaBase64Encryptor(sensitivePropertyHandler.publicKey());
        String encrypted = encryptor.encrypt("somevalue");
        String decrypted = sensitivePropertyHandler.decrypt(encrypted);
        Assert.assertEquals("somevalue", decrypted);
    }

    private SensitivePropertyHandlerImpl getEnabledHandler() {
        return new SensitivePropertyHandlerImpl(true, encryptionPublicKey, encryptionSecretKey, systemConfigService,
                jdbcLockRegistry);
    }

    private SensitivePropertyHandlerImpl getDisabledHandler() {
        return new SensitivePropertyHandlerImpl(false, encryptionPublicKey, encryptionSecretKey, systemConfigService,
                jdbcLockRegistry);
    }
}
