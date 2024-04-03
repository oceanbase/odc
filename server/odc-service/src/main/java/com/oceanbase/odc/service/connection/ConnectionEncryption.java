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
package com.oceanbase.odc.service.connection;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * @Author: Lebie
 * @Date: 2023/5/24 19:16
 * @Description: []
 */
@Component
public class ConnectionEncryption {
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private EncryptionFacade encryptionFacade;

    public ConnectionConfig encryptPasswords(ConnectionConfig connection) {
        if (Objects.isNull(connection.getCipher())) {
            connection.setCipher(Cipher.AES256SALT);
        }
        if (StringUtils.isEmpty(connection.getSalt())) {
            connection.setSalt(encryptionFacade.generateSalt());
        }
        TextEncryptor encryptor = getEncryptor(connection);
        if (Objects.nonNull(connection.getPassword())) {
            connection.setPasswordEncrypted(encryptor.encrypt(connection.getPassword()));
        }
        if (Objects.nonNull(connection.getSysTenantPassword())) {
            connection.setSysTenantPasswordEncrypted(encryptor.encrypt(connection.getSysTenantPassword()));
        }
        return connection;
    }

    ConnectionConfig decryptPasswords(ConnectionConfig connection) {
        PreConditions.notNull(connection.getCipher(), "connection.cipher");
        PreConditions.notEmpty(connection.getSalt(), "connection.salt");

        TextEncryptor encryptor = getEncryptor(connection);
        connection.setPassword(encryptor.decrypt(connection.getPasswordEncrypted()));
        connection.setSysTenantPassword(encryptor.decrypt(connection.getSysTenantPasswordEncrypted()));

        return connection;
    }

    TextEncryptor getEncryptor(ConnectionConfig connection) {
        PreConditions.notNull(connection.getCreatorId(), "organization.creatorId");
        PreConditions.notNull(connection.getOrganizationId(), "organization.organizationId");
        return encryptionFacade.organizationEncryptor(connection.getOrganizationId(), connection.getSalt());
    }

}
