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
package com.oceanbase.odc.service.config;

import static com.oceanbase.odc.service.config.OrganizationConfigKeys.DEFAULT_QUERY_LIMIT;
import static com.oceanbase.odc.service.config.OrganizationConfigKeys.DEFAULT_QUERY_COUNT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.config.OrganizationConfigEntity;
import com.oceanbase.odc.metadb.config.OrganizationConfigRepository;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhuo
 * @date 2025/2/10 15:24
 * @description organization config service
 * @since 1.8
 */
@Slf4j
@Service
@Validated
public class OrganizationConfigService {
    @Autowired
    private OrganizationConfigRepository organizationConfigRepository;
    @Autowired
    private OrganizationConfigMetaService organizationConfigMetaService;

    private List<Configuration> defaultConfigurations;
    private Map<String, ConfigurationMeta> configKeyToConfigMeta;
    private LoadingCache<Long, Map<String, Configuration>> orgIdToConfigurationsCache = Caffeine.newBuilder()
            .maximumSize(500).expireAfterWrite(60, TimeUnit.SECONDS)
            .build(this::internalGetOrgConfigurations);
    private List<Consumer<Configuration>> configurationConsumers = new ArrayList<>();

    @SkipAuthorize("odc internal usage")
    public List<Consumer<Configuration>> getConfigurationConsumer() {
        return this.configurationConsumers;
    }

    @PostConstruct
    public void init() {
        List<ConfigurationMeta> allConfigMetas = organizationConfigMetaService.listAllConfigMetas();
        this.defaultConfigurations = allConfigMetas.stream().map(
                meta -> new Configuration(meta.getKey(), meta.getDefaultValue()))
                .collect(Collectors.toList());
        this.configKeyToConfigMeta = allConfigMetas.stream()
                .collect(Collectors.toMap(ConfigurationMeta::getKey, e -> e));
        log.info("Default organization configurations: {}", defaultConfigurations);
    }

    /**
     * Query the organization configuration of the current organization.
     */
    @PreAuthenticate(actions = "read", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public List<Configuration> listOrganizationConfigurations(@NotNull Long organizationId) {
        Map<String, Configuration> keyToConfiguration;
        try {
            keyToConfiguration = organizationConfigRepository
                    .findByOrganizationId(organizationId)
                    .stream().map(Configuration::ofOrg)
                    .collect(Collectors.toMap(Configuration::getKey, e -> e));
        } catch (Exception e) {
            log.warn("Failed to load organization configurations: ", e);
            throw new RuntimeException("Failed to load organization configurations", e);
        }
        List<Configuration> configurations = listDefaultOrganizationConfigurations();
        List<Consumer<Configuration>> configurationConsumers = getConfigurationConsumer();

        for (Configuration configuration : configurations) {
            keyToConfiguration.computeIfPresent(configuration.getKey(), (key, orgConfig) -> {
                configuration.setValue(orgConfig.getValue());
                return orgConfig;
            });
            for (Consumer<Configuration> consumer : configurationConsumers) {
                consumer.accept(configuration);
            }
        }
        return configurations;
    }

    /**
     * Restore the organization configuration to the default values.
     */
    @PreAuthenticate(actions = "read", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public List<Configuration> listDefaultOrganizationConfigurations() {
        List<Configuration> configurations = new ArrayList<>(defaultConfigurations.size());
        for (Configuration configuration : defaultConfigurations) {
            configurations.add(new Configuration(configuration.getKey(), configuration.getValue()));
        }
        return configurations;
    }

    /**
     * Delete the organization configuration of the current organization.
     */
    @PreAuthenticate(actions = "delete", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public void deleteOrgConfigurations(@NotNull Long organizationId) {
        organizationConfigRepository.deleteById(organizationId);
        evictOrgConfigurationsCache(organizationId);
        log.info("Delete organization configurations, organizationId={}", organizationId);
    }

    /**
     * Batch update the organization configuration with the changes.
     */
    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public List<Configuration> batchUpdate(@NotNull Long organizationId,
            @NotEmpty List<Configuration> configurations) {
        configurations.forEach(this::validateConfiguration);
        validateSqlQueryNumber(configurations);
        List<OrganizationConfigEntity> organizationConfigEntities = configurations.stream()
                .map(record -> record.toOrgEntity(organizationId))
                .collect(Collectors.toList());
        organizationConfigRepository.saveAll(organizationConfigEntities);
        log.info("Update organization configurations, organizationId={}, configurations={}",
                organizationId, configurations);
        evictOrgConfigurationsCache(organizationId);
        return listOrganizationConfigurations(organizationId);
    }

    /**
     * Update the organization configuration of the specified key
     */
    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public Configuration update(@NotNull Long organizationId, @NotNull Configuration configuration) {
        validateConfiguration(configuration);
        OrganizationConfigEntity entity = configuration.toOrgEntity(organizationId);
        organizationConfigRepository.save(entity);
        log.info("Update organization configuration, organizationId={}, configuration={}",
                organizationId, configuration);
        OrganizationConfigEntity stored = organizationConfigRepository.findByOrganizationIdAndKey(
                entity.getId(), configuration.getKey());
        evictOrgConfigurationsCache(organizationId);
        return Configuration.ofOrg(stored);
    }

    private void validateSqlQueryNumber(List<Configuration> configurations) {
        Map<String, Configuration> configMap = configurations.stream()
                .collect(Collectors.toMap(Configuration::getKey, c -> c));

        String queryLimit = configMap.get(DEFAULT_QUERY_LIMIT).getValue();
        String queryNumber = configMap.get(DEFAULT_QUERY_COUNT).getValue();

        if (Long.parseLong(queryNumber) > Long.parseLong(queryLimit)) {
            throw new IllegalArgumentException("Query number exceeds the limit: " + queryNumber + " > " + queryLimit);
        }
    }

    private void validateConfiguration(Configuration configuration) {
        ConfigurationMeta meta = configKeyToConfigMeta.get(configuration.getKey());
        if (Objects.isNull(meta)) {
            throw new IllegalArgumentException("Invalid configuration key: " + configuration.getKey());
        }
        ConfigValueValidator.validate(meta, configuration.getValue());
    }

    public Map<String, Configuration> getOrgConfigurationsFromCache(Long organizationId) {
        return orgIdToConfigurationsCache.get(organizationId);
    }

    private Map<String, Configuration> internalGetOrgConfigurations(Long organizationId) {
        return listOrganizationConfigurations(organizationId).stream()
                .collect(Collectors.toMap(Configuration::getKey, c -> c));
    }

    private void evictOrgConfigurationsCache(@NotNull Long organizationId) {
        try {
            orgIdToConfigurationsCache.invalidate(organizationId);
        } catch (Exception e) {
            log.warn("Failed to evict cache, organizationId={}, reason={}",
                    organizationId, ExceptionUtils.getRootCauseReason(e));
        }
    }

}
