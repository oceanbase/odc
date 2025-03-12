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
import static com.oceanbase.odc.service.config.OrganizationConfigKeys.DEFAULT_MAX_QUERY_LIMIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
            .maximumSize(300).expireAfterWrite(60, TimeUnit.SECONDS)
            .build(this::internalQuery);
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
     * Query the organization configuration of the current organization
     * and merge with the default values.
     */
    @PreAuthenticate(actions = "read", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public List<Configuration> queryList(@NotNull Long organizationId) {
        Map<String, Configuration> keyToConfiguration = Optional
            .ofNullable(organizationConfigRepository.findByOrganizationId(organizationId))
            .orElse(Collections.emptyList())
            .stream().map(Configuration::convert2DTO)
            .collect(Collectors.toMap(Configuration::getKey, e -> e));

        List<Configuration> configurations = queryListDefault();
        List<Consumer<Configuration>> configurationConsumers = getConfigurationConsumer();

        configurations.forEach(configuration -> {
            keyToConfiguration.computeIfPresent(configuration.getKey(), (key, config) -> {
                configuration.setValue(config.getValue());
                return config;
            });
            configurationConsumers.forEach(consumer -> consumer.accept(configuration));
        });

        return configurations;
    }

    /**
     * Query the organization configuration default values.
     */
    @PreAuthenticate(actions = "read", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public List<Configuration> queryListDefault() {
        List<Configuration> configurations = new ArrayList<>(defaultConfigurations.size());
        for (Configuration configuration : defaultConfigurations) {
            configurations.add(new Configuration(configuration.getKey(), configuration.getValue()));
        }
        return configurations;
    }

    /**
     * Batch update the organization configuration with the changes
     * and clear the cache.
     */
    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public List<Configuration> batchUpdate(@NotNull Long organizationId,
            @NotEmpty List<Configuration> configurations) {
        configurations.forEach(this::validateConfiguration);
        validateSqlQueryLimit(configurations);
        List<OrganizationConfigEntity> organizationConfigEntities = configurations.stream()
                .map(record -> record.convert2DO(organizationId))
                .collect(Collectors.toList());
        organizationConfigRepository.saveAll(organizationConfigEntities);
        log.info("Update organization configurations, organizationId={}, configurations={}",
                organizationId, configurations);
        evictOrgConfigurationsCache(organizationId);
        return queryList(organizationId);
    }

    /**
     * Delete the organization configuration of the current organization
     * and clear the cache.
     */
    @PreAuthenticate(actions = "delete", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public void deleteOrgConfigurations(@NotNull Long organizationId) {
        organizationConfigRepository.deleteById(organizationId);
        evictOrgConfigurationsCache(organizationId);
        log.info("Delete organization configurations, organizationId={}", organizationId);
    }

    public Map<String, Configuration> getOrgConfigurationsFromCache(Long organizationId) {
        return orgIdToConfigurationsCache.get(organizationId);
    }

    private void validateSqlQueryLimit(List<Configuration> configurations) {
        Map<String, Configuration> configMap = configurations.stream()
                .collect(Collectors.toMap(Configuration::getKey, c -> c));

        String maxQueryLimit = configMap.get(DEFAULT_MAX_QUERY_LIMIT).getValue();
        String queryLimit = configMap.get(DEFAULT_QUERY_LIMIT).getValue();

        if (Long.parseLong(queryLimit) > Long.parseLong(maxQueryLimit)) {
            throw new IllegalArgumentException(
                "Query limit exceeds the max value: " + queryLimit + " > " + maxQueryLimit);
        }
    }

    private void validateConfiguration(Configuration configuration) {
        ConfigurationMeta meta = configKeyToConfigMeta.get(configuration.getKey());
        if (Objects.isNull(meta)) {
            throw new IllegalArgumentException("Invalid configuration key: " + configuration.getKey());
        }
        ConfigValueValidator.validate(meta, configuration.getValue());
    }

    private void evictOrgConfigurationsCache(@NotNull Long organizationId) {
        try {
            orgIdToConfigurationsCache.invalidate(organizationId);
        } catch (Exception e) {
            log.warn("Failed to evict cache, organizationId={}, reason={}",
                    organizationId, ExceptionUtils.getRootCauseReason(e));
        }
    }

    private Map<String, Configuration> internalQuery(Long organizationId) {
        return queryList(organizationId).stream()
                .collect(Collectors.toMap(Configuration::getKey, c -> c));
    }

    private static List<Configuration> mergeConfigurations(List<Configuration> defaultConfigurations,
        List<Configuration> updateConfigurations) {
        List<Configuration> mergedConfigurations = new ArrayList<>();
        for (Configuration defaultConfig : defaultConfigurations) {
            Configuration mergedConfig = new Configuration();
            mergedConfig.setKey(defaultConfig.getKey());
            for (Configuration updateConfig : updateConfigurations) {
                if (Objects.nonNull(updateConfig.getKey())
                    && Objects.equals(updateConfig.getKey(), defaultConfig.getKey())) {
                    if (Objects.nonNull(updateConfig.getValue())) {
                        mergedConfig.setValue(updateConfig.getValue());
                    }
                }
            }
            if (Objects.isNull(mergedConfig.getValue()) && Objects.nonNull(defaultConfig.getValue())) {
                mergedConfig.setValue(defaultConfig.getValue());
            }
            mergedConfigurations.add(mergedConfig);
        }
        return mergedConfigurations;
    }

}
