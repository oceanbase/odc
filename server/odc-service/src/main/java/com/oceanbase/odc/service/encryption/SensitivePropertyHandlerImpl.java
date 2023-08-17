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

import java.util.Objects;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SensitivePropertyHandlerImpl implements SensitivePropertyHandler {
    private static final int SECRET_LENGTH = 16;
    private String encryptionSecret;
    private TextEncryptor textEncryptor;

    public SensitivePropertyHandlerImpl(
            @Value("${odc.system.security.sensitive-property-encrypted:true}") Boolean sensitiveInputEncrypted) {
        this.encryptionSecret = sensitiveInputEncrypted ? RandomStringUtils.randomAlphanumeric(SECRET_LENGTH)
                : ENCRYPTION_NOT_SUPPORTED;
        this.textEncryptor = Objects.equals(ENCRYPTION_NOT_SUPPORTED, this.encryptionSecret) ? Encryptors.empty()
                : Encryptors.blowFishZeroPaddingBase64(this.encryptionSecret);
        log.info("SensitiveInputHandler initialized, sensitiveInputEncrypted={}", sensitiveInputEncrypted);
    }

    @Override
    public String encryptionSecret() {
        return this.encryptionSecret;
    }

    @Override
    public String encrypt(String plainText) {
        return textEncryptor.encrypt(plainText);
    }

    @Override
    public String decrypt(String encryptedText) {
        return textEncryptor.decrypt(encryptedText);
    }

}
