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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.RsaBytesEncryptor;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.lang.Pair;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SensitivePropertyHandlerImpl implements SensitivePropertyHandler {
    private static final int ENCRYPTION_KEY_SIZE = 1024;
    private final String publicKey;
    private final TextEncryptor textEncryptor;

    public SensitivePropertyHandlerImpl(
            @Value("${odc.system.security.sensitive-property-encrypted:true}") Boolean sensitiveInputEncrypted) {
        if (sensitiveInputEncrypted) {
            Pair<String, String> keyPair = RsaBytesEncryptor.generateBase64EncodeKeyPair(ENCRYPTION_KEY_SIZE);
            this.publicKey = keyPair.left;
            this.textEncryptor = Encryptors.rsaBase64Decryptor(keyPair.right);
        } else {
            this.publicKey = null;
            this.textEncryptor = Encryptors.empty();
        }
        log.info("SensitiveInputHandler initialized, sensitiveInputEncrypted={}", sensitiveInputEncrypted);
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
