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
package com.oceanbase.odc.service.llm.provider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.util.Lazy;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;
import com.oceanbase.odc.service.llm.provider.template.ProviderTemplate;
import com.oceanbase.odc.service.llm.util.YamlUtil;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/14
 */
public abstract class AbstractProviderFacade<ModelCredential, ProviderCredential>
        implements LlmProviderFacade<ModelCredential, ProviderCredential> {

    protected final Lazy<ProviderTemplate> providerTemplate = new Lazy<>(
            () -> {
                try (InputStream inputStream =
                        getClass().getClassLoader().getResourceAsStream(getTemplateResourcePath())) {
                    String template = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    return YamlUtil.from(template, ProviderTemplate.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    @Autowired
    protected EncryptionFacade encryptionFacade;
    @Autowired
    protected SensitivePropertyHandler sensitivePropertyHandler;

    protected abstract String getTemplateResourcePath();

    protected String decrypt(String encrypted, Long organizationId, String salt) {
        TextEncryptor encryptor = encryptionFacade.organizationEncryptor(organizationId, salt);
        return encryptor.decrypt(encrypted);
    }

    protected String encrypt(String plain, Long organizationId, String salt) {
        TextEncryptor encryptor = encryptionFacade.organizationEncryptor(organizationId, salt);
        return encryptor.encrypt(plain);
    }

    protected String decrypt(String encrypted) {
        if (StringUtils.isBlank(encrypted)) {
            return encrypted;
        }
        return sensitivePropertyHandler.decrypt(encrypted);
    }

    @Override
    public ProviderTemplate getProviderTemplate() {
        return providerTemplate.get();
    }

}
