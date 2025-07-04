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
package com.oceanbase.odc.service.iam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.crypto.CryptoUtils;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.AccessKeyStatus;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.AccessKeyEntity;
import com.oceanbase.odc.metadb.iam.AccessKeyRepository;
import com.oceanbase.odc.service.common.response.CustomPage;
import com.oceanbase.odc.service.common.response.PaginatedData;
import com.oceanbase.odc.service.common.util.LockTemplate;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.AccessKeyDTO;
import com.oceanbase.odc.service.iam.model.UpdateAccessKeyRequest;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Authenticated
public class AccessKeyService {

    @Autowired
    private AccessKeyRepository accessKeyRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;


    @Autowired
    private EncryptionFacade encryptionFacade;

    @Value("${odc.access-key.max-per-user:5}")
    private int maxAccessKeysPerUser;

    @Autowired
    private LockTemplate lockTemplate;

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_USER", indexOfIdParam = 0)
    public AccessKeyDTO create(Long userId) {
        Verify.verify(authenticationFacade.currentOrganization().getType().equals(OrganizationType.TEAM),
                "invalid space");

        return lockTemplate.executeWithLock("access-key-limit", userId.toString(), () -> {
            validateUserAccessKeyLimit(userId);

            String accessKeyId = generateAccessKeyId();
            String secretAccessKey = CryptoUtils.generateAes(256);
            String salt = encryptionFacade.generateSalt();
            String encryptedSecretAccessKey = encodeSecretAccessKey(secretAccessKey, salt,
                    authenticationFacade.currentOrganizationId());

            AccessKeyEntity entity = new AccessKeyEntity();
            entity.setOrganizationId(authenticationFacade.currentOrganizationId());
            entity.setUserId(userId);
            entity.setAccessKeyId(accessKeyId);
            entity.setSecretAccessKey(encryptedSecretAccessKey);
            entity.setSalt(salt);
            entity.setStatus(AccessKeyStatus.ACTIVE);
            entity.setCreatorId(authenticationFacade.currentUserId());

            AccessKeyEntity savedEntity = accessKeyRepository.save(entity);
            log.info("Created access key: {}", savedEntity.getAccessKeyId());

            AccessKeyDTO dto = AccessKeyMapper.INSTANCE.entityToDTO(savedEntity);
            dto.setSecretAccessKey(secretAccessKey); // Return plain secret for first time
            return dto;
        });
    }



    @SkipAuthorize("for authentication")
    public Optional<AccessKeyEntity> getByAccessKeyId(String accessKeyId) {
        return accessKeyRepository.findByAccessKeyIdAndStatusNot(accessKeyId, AccessKeyStatus.DELETED);
    }

    @Nullable
    public String getDecryptAccessKey(String accessKeyId) {
        return accessKeyRepository.findByAccessKeyIdAndStatusNot(accessKeyId, AccessKeyStatus.DELETED)
                .filter(AccessKeyEntity::isValid)
                .map(e -> decodeSecretAccessKey(e.getSecretAccessKey(), e.getSalt(), e.getOrganizationId()))
                .orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_USER", indexOfIdParam = 0)
    public AccessKeyDTO update(Long userId, String accessKey, UpdateAccessKeyRequest request) {
        AccessKeyEntity entity = accessKeyRepository.findByAccessKeyId(accessKey)
                .orElseThrow(
                        () -> new NotFoundException(ErrorCodes.NotFound, new Object[] {"Access key", "id", accessKey},
                                "Access key not found"));
        Verify.equals(entity.getUserId(), userId, "UserId");

        if (request.getStatus() != null) {
            validateStatusUpdate(entity.getStatus(), request.getStatus());
            entity.setStatus(request.getStatus());
        }

        AccessKeyEntity savedEntity = accessKeyRepository.save(entity);
        log.info("Updated access key: {}", userId);

        return AccessKeyMapper.INSTANCE.entityToDTO(savedEntity);
    }


    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_USER", indexOfIdParam = 0)
    public AccessKeyDTO delete(Long userId, String accessKey) {
        AccessKeyEntity entity = accessKeyRepository.findByAccessKeyId(accessKey)
                .orElseThrow(
                        () -> new NotFoundException(ErrorCodes.NotFound, new Object[] {"Access key", "id", accessKey},
                                "Access key not found"));
        Verify.equals(entity.getUserId(), userId, "UserId");

        accessKeyRepository.softDelete(accessKey);
        log.info("Deleted access key: {}", accessKey);

        return AccessKeyMapper.INSTANCE.entityToDTO(entity);
    }


    @PreAuthenticate(actions = "update", resourceType = "ODC_USER", indexOfIdParam = 0)
    public PaginatedData<AccessKeyDTO> listByUserId(Long userId, Pageable pageable) {
        Page<AccessKeyEntity> page =
                accessKeyRepository.findByUserIdAndStatusNot(userId, AccessKeyStatus.DELETED, pageable);

        List<AccessKeyDTO> dtos = page.getContent().stream()
                .map(AccessKeyMapper.INSTANCE::entityToDTO)
                .toList();

        return new PaginatedData<>(dtos, CustomPage.from(page));
    }


    private String generateAccessKeyId() {
        return "AK" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String encodeSecretAccessKey(String plainSecretAccessKey, String salt, Long organizationId) {
        if (plainSecretAccessKey == null) {
            return null;
        }
        TextEncryptor encryptor = encryptionFacade.organizationEncryptor(organizationId, salt);
        return encryptor.encrypt(plainSecretAccessKey);
    }

    private String decodeSecretAccessKey(String encryptedSecretAccessKey, String salt, Long organizationId) {
        if (encryptedSecretAccessKey == null) {
            return null;
        }
        TextEncryptor encryptor = encryptionFacade.organizationEncryptor(organizationId, salt);
        return encryptor.decrypt(encryptedSecretAccessKey);
    }


    private void validateStatusUpdate(AccessKeyStatus currentStatus, AccessKeyStatus newStatus) {
        if (currentStatus == AccessKeyStatus.DELETED) {
            throw new BadRequestException("Cannot update deleted access key");
        }

        if (newStatus == AccessKeyStatus.DELETED) {
            throw new BadRequestException("Cannot set status to DELETED, use delete API instead");
        }

    }

    private void validateUserAccessKeyLimit(Long userId) {
        long count = accessKeyRepository.countByUserIdAndStatusNot(userId, AccessKeyStatus.DELETED);
        if (count >= maxAccessKeysPerUser) {
            throw new BadRequestException("User already has the maximum allowed access keys");
        }
    }
}
