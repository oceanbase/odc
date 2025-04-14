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

import static com.oceanbase.odc.service.config.OrganizationConfigKeys.DEFAULT_MAX_QUERY_LIMIT;
import static com.oceanbase.odc.service.config.OrganizationConfigKeys.DEFAULT_QUERY_LIMIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
import com.oceanbase.odc.metadb.config.OrganizationConfigDAO;
import com.oceanbase.odc.metadb.config.OrganizationConfigEntity;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;
import com.oceanbase.odc.service.session.SessionProperties;

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
    private OrganizationConfigDAO organizationConfigDAO;
    @Autowired
    private OrganizationConfigMetaService organizationConfigMetaService;
    @Autowired
    private SessionProperties sessionProperties;

    private List<Configuration> defaultConfigurations;
    private Map<String, ConfigurationMeta> configKeyToConfigMeta;
    private final LoadingCache<Long, Map<String, Configuration>> orgIdToConfigurationsCache = Caffeine.newBuilder()
            .maximumSize(300).expireAfterWrite(60, TimeUnit.SECONDS)
            .build(this::internalQuery);

    @PostConstruct
    public void init() {
        List<ConfigurationMeta> organizationConfigMetas = organizationConfigMetaService.listAllConfigMetas();

        this.defaultConfigurations = organizationConfigMetas.stream()
                .map(meta -> new Configuration(meta.getKey(), meta.getDefaultValue()))
                .collect(Collectors.toList());
        this.configKeyToConfigMeta = organizationConfigMetas.stream()
                .collect(Collectors.toMap(ConfigurationMeta::getKey, e -> e));
        log.info("Default organization configurations: {}", defaultConfigurations);
    }

    /**
     * Query the organization configurations of the current organization and merge with the default
     * organization configurations.
     */
    @SkipAuthorize("internal authenticated")
    public List<Configuration> queryList(@NotNull Long organizationId) {
        Map<String, Configuration> keyToConfiguration = Optional
                .ofNullable(organizationConfigDAO.queryByOrganizationId(organizationId))
                .orElse(Collections.emptyList())
                .stream().map(Configuration::convert2DTO)
                .collect(Collectors.toMap(Configuration::getKey, e -> e));

        List<Configuration> configurations = queryListDefault();

        configurations.forEach(configuration -> {
            keyToConfiguration.computeIfPresent(configuration.getKey(), (key, config) -> {
                configuration.setValue(config.getValue());
                return config;
            });
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
     * Batch update the organization configuration with the changes and clear the cache.
     */
    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_ORGANIZATION_CONFIG", isForAll = true)
    public List<Configuration> batchUpdate(@NotNull Long organizationId, @NotNull Long userId,
            @NotEmpty List<Configuration> configurations) {
        validateConfiguration(configurations);
        List<OrganizationConfigEntity> organizationConfigEntities = configurations.stream()
                .map(record -> record.convert2DO(organizationId, userId))
                .collect(Collectors.toList());
        int affectRows = organizationConfigDAO.batchUpsert(organizationConfigEntities);
        log.info("Update organization configurations, organizationId={}, affectRows={}, configurations={}",
                organizationId, affectRows, configurations);
        evictOrgConfigurationsCache(organizationId);
        return queryList(organizationId);
    }

    public Map<String, Configuration> getOrgConfigurationsFromCache(Long organizationId) {
        return orgIdToConfigurationsCache.get(organizationId);
    }

    private void validateConfiguration(List<Configuration> configurations) {
        configurations.forEach(config -> {
            ConfigurationMeta meta = configKeyToConfigMeta.get(config.getKey());
            if (Objects.isNull(meta)) {
                throw new IllegalArgumentException("Invalid configuration key: " + config.getKey());
            }
            ConfigValueValidator.validateOrganizationConfig(meta, sessionProperties, config.getValue());
        });
        Map<String, Configuration> configMap = configurations.stream()
                .collect(Collectors.toMap(Configuration::getKey, c -> c));

        String maxQueryLimit = configMap.get(DEFAULT_MAX_QUERY_LIMIT).getValue();
        String queryLimit = configMap.get(DEFAULT_QUERY_LIMIT).getValue();

        if (Integer.parseInt(queryLimit) > Integer.parseInt(maxQueryLimit)) {
            throw new IllegalArgumentException(
                    "Query limit exceeds the max value: " + queryLimit + " > " + maxQueryLimit);
        }
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

}
