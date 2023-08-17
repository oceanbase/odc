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
package com.oceanbase.odc.service.bastion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.service.bastion.model.BastionProperties;
import com.oceanbase.odc.service.bastion.model.BastionProperties.EncryptionProperties;

@Component
public class BastionEncryptionService {
    @Autowired
    private BastionProperties bastionProperties;

    public String encrypt(String plainText) {
        return getEncryptor().encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        return getEncryptor().decrypt(encryptedText);
    }

    private TextEncryptor getEncryptor() {
        EncryptionProperties encryption = bastionProperties.getEncryption();
        if (!encryption.isEnabled()) {
            return Encryptors.empty();
        }
        switch (encryption.getAlgorithm()) {
            case AES256_BASE64:
                return Encryptors.aes256Base64(encryption.getSecret());
            case CMCC4A:
                return Encryptors.aesBase64Cmcc4A(encryption.getSecret());
            case RAW:
            default:
                return Encryptors.empty();
        }
    }

}
