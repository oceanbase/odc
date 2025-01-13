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
package com.oceanbase.odc.service.integration;

import static com.oceanbase.odc.service.integration.model.IntegrationType.SSO;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.metadb.integration.IntegrationRepository;
import com.oceanbase.odc.metadb.integration.IntegrationSpecs;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.ResourcePermissionAccessor;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.Encryption;
import com.oceanbase.odc.service.integration.model.Encryption.EncryptionAlgorithm;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationProperties;
import com.oceanbase.odc.service.integration.model.IntegrationType;
import com.oceanbase.odc.service.integration.model.QueryIntegrationParams;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.model.SqlInterceptorProperties;
import com.oceanbase.odc.service.integration.saml.SamlCredentialManager;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/3/23 20:43
 */
@Slf4j
@Service
@Validated
@Authenticated
public class IntegrationService {
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private IntegrationConfigurationProcessorDelegate integrationConfigurationProcessorDelegate;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    @Autowired
    private EncryptionFacade encryptionFacade;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ResourcePermissionAccessor resourcePermissionAccessor;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CacheManager defaultCacheManager;

    @Autowired
    private SamlCredentialManager samlCredentialManager;

    @PreAuthenticate(actions = "create", resourceType = "ODC_INTEGRATION", isForAll = true)
    public Boolean exists(@NotBlank String name, @NotNull IntegrationType type) {
        Long organizationId = authenticationFacade.currentOrganizationId();
        return integrationRepository.findByNameAndTypeAndOrganizationId(name, type, organizationId).isPresent();
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_INTEGRATION", isForAll = true)
    public IntegrationConfig create(@NotNull @Valid IntegrationConfig config) {
        Long userId = authenticationFacade.currentUserId();
        Long organizationId = authenticationFacade.currentOrganizationId();
        Optional<IntegrationEntity> existsEntity = integrationRepository
                .findByNameAndTypeAndOrganizationId(config.getName(), config.getType(), organizationId);
        PreConditions.validNoDuplicated(ResourceType.ODC_EXTERNAL_APPROVAL, "name", config.getName(),
                existsEntity::isPresent);
        integrationConfigurationProcessorDelegate.preProcessConfig(config, null);
        Encryption encryption = config.getEncryption();
        encryption.check();
        applicationContext.publishEvent(IntegrationEvent.createPreCreate(config));
        IntegrationEntity entity = new IntegrationEntity();
        entity.setName(config.getName());
        entity.setType(config.getType());
        entity.setCreatorId(userId);
        entity.setOrganizationId(organizationId);
        entity.setEnabled(config.getEnabled());
        entity.setBuiltin(false);
        entity.setEncrypted(Boolean.TRUE.equals(encryption.getEnabled()));
        entity.setAlgorithm(encryption.getAlgorithm());
        entity.setSalt(encryptionFacade.generateSalt());
        entity.setSecret(encodeSecret(encryption.getSecret(), entity.getSalt(), entity.getOrganizationId()));
        entity.setConfiguration(config.getConfiguration());
        entity.setDescription(config.getDescription());
        integrationRepository.saveAndFlush(entity);
        log.info("New external integration has been inserted, integration: {}", entity);
        return new IntegrationConfig(entity);
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_INTEGRATION", indexOfIdParam = 0)
    public IntegrationConfig detail(@NotNull Long id) {
        IntegrationConfig config = detailWithoutPermissionCheck(id);
        permissionValidator.checkCurrentOrganization(config);
        return config;
    }

    @SkipAuthorize("inside method permission check")
    public Page<IntegrationConfig> list(@NotNull QueryIntegrationParams params, @NotNull Pageable pageable) {
        Map<String, Set<String>> integrationId2Actions = resourcePermissionAccessor.permittedResourceActions(
                authenticationFacade.currentUserId(), ResourceType.ODC_INTEGRATION, permission -> {
                    ResourcePermission minPermission = new ResourcePermission(permission.getResourceId(),
                            ResourceType.ODC_INTEGRATION.name(), "read");
                    return permission.implies(minPermission);
                });
        if (integrationId2Actions.isEmpty()) {
            return Page.empty(pageable);
        }
        String creatorName = params.getCreatorName();
        List<Long> creatorIds = null;
        if (StringUtils.isNotBlank(creatorName)) {
            creatorIds = userService.getUsersByFuzzyNameWithoutPermissionCheck(creatorName)
                    .stream().map(User::getId).collect(Collectors.toList());
        }
        Specification<IntegrationEntity> spec = Specification
                .where(IntegrationSpecs.nameLike(params.getName()))
                .and(IntegrationSpecs.typeEqual(params.getType()))
                .and(IntegrationSpecs.creatorIdIn(creatorIds))
                .and(IntegrationSpecs.enabledEqual(params.getEnabled()))
                .and(IntegrationSpecs.organizationIdEqual(authenticationFacade.currentOrganizationId()));
        if (!integrationId2Actions.containsKey("*")) {
            spec = spec.and(IntegrationSpecs
                    .idIn(integrationId2Actions.keySet().stream().map(Long::parseLong).collect(Collectors.toList())));
        }
        Page<IntegrationConfig> map = integrationRepository.findAll(spec, pageable).map(IntegrationConfig::new);
        userService.assignCreatorNameByCreatorId(map.getContent(), IntegrationConfig::getCreatorId,
                IntegrationConfig::setCreatorName);
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "delete", resourceType = "ODC_INTEGRATION", indexOfIdParam = 0)
    public IntegrationConfig delete(@NotNull Long id) {
        IntegrationEntity entity = nullSafeGet(id);
        IntegrationConfig config = new IntegrationConfig(entity);
        permissionValidator.checkCurrentOrganization(config);
        if (entity.getBuiltin()) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"builtin integration"},
                    "Operation on builtin integration is not allowed");
        }
        applicationContext.publishEvent(IntegrationEvent.createPreDelete(new IntegrationConfig(entity)));
        integrationRepository.delete(entity);
        log.info("An external integration has been deleted, integration: {}", entity);
        return config;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_INTEGRATION", indexOfIdParam = 0)
    public IntegrationConfig update(@NotNull Long id, @NotNull @Valid IntegrationConfig config) {
        IntegrationEntity entity = nullSafeGet(id);
        IntegrationConfig saveConfig = getDecodeConfig(entity);
        permissionValidator.checkCurrentOrganization(saveConfig);
        if (Boolean.TRUE.equals(entity.getBuiltin())) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"builtin integration"},
                    "Operation on builtin integration is not allowed");
        }
        integrationConfigurationProcessorDelegate.preProcessConfig(config, saveConfig);
        Encryption encryption = config.getEncryption();
        applicationContext.publishEvent(
                IntegrationEvent.createPreUpdate(config, new IntegrationConfig(entity), entity.getSalt()));
        entity.setName(config.getName());
        entity.setConfiguration(config.getConfiguration());
        entity.setEnabled(config.getEnabled());
        entity.setEncrypted(Boolean.TRUE.equals(encryption.getEnabled()));
        entity.setAlgorithm(encryption.getAlgorithm() != null ? encryption.getAlgorithm() : EncryptionAlgorithm.RAW);
        if (!encryption.getEnabled() || StringUtils.isNotBlank(encryption.getSecret())) {
            entity.setSalt(encryptionFacade.generateSalt());
            entity.setSecret(encodeSecret(encryption.getSecret(), entity.getSalt(), entity.getOrganizationId()));
        }
        entity.setDescription(config.getDescription());
        integrationRepository.saveAndFlush(entity);
        updateCache(entity.getId());
        log.info("An external integration has been updated, integration: {}", entity);
        return new IntegrationConfig(entity);
    }

    @SkipAuthorize("odc internal usage")
    public IntegrationConfig getDecodeConfig(IntegrationEntity entity) {
        IntegrationConfig integrationConfig = new IntegrationConfig(entity);
        String secret = decodeSecret(entity.getSecret(), entity.getSalt(), entity.getOrganizationId());
        integrationConfig.getEncryption().setSecret(secret);
        return integrationConfig;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_INTEGRATION", indexOfIdParam = 0)
    public IntegrationConfig setEnabled(@NotNull Long id, @NotNull Boolean enabled) {
        IntegrationEntity entity = nullSafeGet(id);
        permissionValidator.checkCurrentOrganization(new IntegrationConfig(entity));
        if (entity.getBuiltin()) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"builtin integration"},
                    "Operation on builtin integration is not allowed");
        }
        if (!Objects.equals(entity.getEnabled(), enabled)) {
            IntegrationConfig preConfig = new IntegrationConfig(entity);
            entity.setEnabled(enabled);
            applicationContext
                    .publishEvent(IntegrationEvent.createPreUpdate(new IntegrationConfig(entity), preConfig,
                            entity.getSalt()));
            integrationRepository.saveAndFlush(entity);
            log.info("An external integration has been updated, integration: {}", entity);
        }
        return new IntegrationConfig(entity);
    }

    @SkipAuthorize("odc internal usage")
    public IntegrationConfig detailWithoutPermissionCheck(@NotNull Long id) {
        IntegrationEntity entity = nullSafeGet(id);
        IntegrationConfig config = new IntegrationConfig(entity);
        Encryption encryption = Encryption.builder()
                .enabled(entity.getEncrypted())
                .algorithm(entity.getAlgorithm())
                .secret(decodeSecret(entity.getSecret(), entity.getSalt(), entity.getOrganizationId())).build();
        config.setEncryption(encryption);
        if (Objects.nonNull(config.getCreatorId())) {
            try {
                UserEntity userEntity = userService.nullSafeGet(config.getCreatorId());
                config.setCreatorName(userEntity.getAccountName());
            } catch (NotFoundException e) {
                log.warn("Creator id={} for integration id={} is not exist, details={}", entity.getCreatorId(),
                        entity.getId(), e);
            }
        }
        return config;
    }

    @SkipAuthorize("odc internal usage")
    public List<IntegrationEntity> listByTypeAndEnabled(@NotNull IntegrationType type, boolean enabled) {
        long organizationId = authenticationFacade.currentOrganizationId();
        return integrationRepository.findByTypeAndEnabledAndOrganizationId(type, enabled, organizationId);
    }

    @SkipAuthorize("odc internal usage")
    public IntegrationEntity nullSafeGet(@NotNull Long id) {
        return integrationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_INTEGRATION, "id", id));
    }

    @SkipAuthorize("odc internal usage")
    public Optional<IntegrationEntity> findIntegrationById(@NonNull Long id) {
        return integrationRepository.findById(id);
    }

    @SkipAuthorize("odc internal usage")
    public Optional<IntegrationEntity> findByTypeAndOrganizationIdAndName(IntegrationType type, Long organizationId,
            String name) {
        return integrationRepository.findByTypeAndOrganizationIdAndName(type, organizationId, name);
    }

    @Cacheable(cacheNames = "integrationProperties", cacheManager = "defaultCacheManager")
    @SkipAuthorize("odc internal usage")
    public IntegrationProperties getIntegrationProperties(@NonNull Long integrationId) {
        IntegrationConfig config = detailWithoutPermissionCheck(integrationId);
        if (config.getType() == IntegrationType.APPROVAL) {
            return ApprovalProperties.from(config);
        } else if (config.getType() == IntegrationType.SQL_INTERCEPTOR) {
            return SqlInterceptorProperties.from(config);
        } else {
            throw new UnsupportedException("The type of integration is unknown, type=" + config.getType());
        }
    }

    /**
     * this version only support one enabled SSO integration
     *
     * @return
     */
    @SkipAuthorize("odc internal usage")
    public SSOIntegrationConfig getSSoIntegrationConfig() {
        List<Long> teamOrganization = organizationRepository.findIdByType(OrganizationType.TEAM);
        List<IntegrationEntity> enabledOauth2Integration =
                integrationRepository.findByTypeAndEnabledAndOrganizationIdIn(SSO, true, teamOrganization);
        if (CollectionUtils.isNotEmpty(enabledOauth2Integration)) {
            IntegrationEntity integrationEntity = enabledOauth2Integration.get(0);
            return getSSOIntegrationConfig(integrationEntity);
        }
        return null;
    }

    @SkipAuthorize("odc internal usage")
    public SSOIntegrationConfig getSSOIntegrationConfig(IntegrationEntity integrationEntity) {
        SSOIntegrationConfig ssoIntegrationConfig =
                JsonUtils.fromJson(integrationEntity.getConfiguration(), SSOIntegrationConfig.class);
        ssoIntegrationConfig.fillDecryptSecret(decodeSecret(integrationEntity.getSecret(),
                integrationEntity.getSalt(), integrationEntity.getOrganizationId()));
        return ssoIntegrationConfig;
    }

    private String encodeSecret(String plainSecret, String salt, Long organizationId) {
        if (plainSecret == null) {
            return null;
        }
        TextEncryptor encryptor = encryptionFacade.organizationEncryptor(organizationId, salt);
        return encryptor.encrypt(plainSecret);
    }

    @SkipAuthorize("odc internal usage")
    public String decodeSecret(String encryptedSecret, String salt, Long organizationId) {
        if (encryptedSecret == null) {
            return null;
        }
        TextEncryptor encryptor = encryptionFacade.organizationEncryptor(organizationId, salt);
        return encryptor.decrypt(encryptedSecret);
    }

    private void updateCache(Long key) {
        Cache cache = defaultCacheManager.getCache("integrationProperties");
        if (Objects.nonNull(cache)) {
            cache.evictIfPresent(key);
        }
    }

    @SkipAuthorize("odc internal usage")
    public SSOCredential generateSSOCredential() {
        return new SSOCredential(samlCredentialManager.generateCertWithCachedPrivateKey());
    }

}
