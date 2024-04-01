/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.task.jasypt;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

/**
 * @author yaobin
 * @date 2024-04-01
 * @since 4.2.4
 */
public class DefaultJasyptEncryptor implements JasyptEncryptor {

    private final String prefix;
    private final String suffix;
    private final JasyptEncryptorConfigProperties properties;

    public DefaultJasyptEncryptor(JasyptEncryptorConfigProperties properties) {
        this.properties = properties;
        this.prefix = this.properties.getPrefix();
        this.suffix = this.properties.getSuffix();
    }


    @Override
    public String decrypt(String property) {
        if (!isEncrypted(property)) {
            return property;
        }
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPassword(properties.getSalt());
        encryptor.setAlgorithm(properties.getAlgorithm());
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.setPoolSize(1);
        return encryptor.decrypt(unwrapEncryptedValue(property));
    }

    private boolean isEncrypted(String property) {
        if (property == null || properties.getAlgorithm() == null
                || properties.getSalt() == null || prefix == null || suffix == null) {
            return false;
        }
        final String trimmedValue = property.trim();
        return (trimmedValue.startsWith(prefix) &&
                trimmedValue.endsWith(suffix));
    }

    private String unwrapEncryptedValue(String property) {
        return property.substring(
                prefix.length(),
                (property.length() - suffix.length()));
    }
}
