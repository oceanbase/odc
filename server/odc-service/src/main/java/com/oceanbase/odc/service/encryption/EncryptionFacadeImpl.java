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

import java.time.Duration;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.crypto.CryptoUtils;
import com.oceanbase.odc.common.crypto.Encryptors;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.Organization;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * @author yizhou.xw
 * @version : EncryptionFacadeImpl.java, v 0.1 2021-07-26 9:31
 */
@Service
@SkipAuthorize("odc internal usage")
public class EncryptionFacadeImpl implements EncryptionFacade {
    private static final int SALT_SIZE = 16;
    private static final long AES_CACHE_SIZE = 1000L;
    private static final long AES_CACHE_LIVE_SECONDS = 600L;

    private static LoadingCache<EncryptorKey, TextEncryptor> aesEncryptorCache =
            Caffeine.newBuilder()
                    .maximumSize(AES_CACHE_SIZE)
                    .expireAfterWrite(Duration.ofSeconds(AES_CACHE_LIVE_SECONDS))
                    .build(key -> Encryptors.aesBase64(key.encryptionPassword, key.salt));

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private UserService userService;
    @Autowired
    private OrganizationService organizationService;

    @Override
    public String encryptByCurrentUserPassword(String text, String salt) {
        if (Objects.isNull(text)) {
            return null;
        }
        TextEncryptor encryptor = currentUserEncryptor(salt);
        return encryptor.encrypt(text);
    }

    @Override
    public String decryptByCurrentUserPassword(String encryptedText, String salt) {
        if (StringUtils.isEmpty(encryptedText)) {
            return encryptedText;
        }
        TextEncryptor encryptor = currentUserEncryptor(salt);
        return encryptor.decrypt(encryptedText);
    }

    @Override
    public String generateSalt() {
        return CryptoUtils.generateSalt();
    }

    @Override
    public TextEncryptor currentUserEncryptor(String salt) {
        String userPassword = userService.nullSafeGet(authenticationFacade.currentUserId()).getPassword();
        return passwordEncryptor(userPassword, salt);
    }

    @Override
    public TextEncryptor passwordEncryptor(String encryptionPassword, String salt) {
        return getEncryptor(encryptionPassword, salt);
    }

    @Override
    public TextEncryptor userEncryptor(Long userId, String salt) {
        String encryptionPassword = userService.nullSafeGet(userId).getPassword();
        return passwordEncryptor(encryptionPassword, salt);
    }

    @Override
    public TextEncryptor organizationEncryptor(Long organizationId, String salt) {
        Organization organization = organizationService.get(organizationId).orElseThrow(
                () -> new NotFoundException(ResourceType.ODC_ORGANIZATION, "organizationId", organizationId));
        return getEncryptor(organization.getSecret(), salt);
    }

    private TextEncryptor getEncryptor(String encryptionPassword, String salt) {
        return aesEncryptorCache.get(new EncryptorKey(encryptionPassword, salt));
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    private static class EncryptorKey {
        String encryptionPassword;
        String salt;
    }

}
