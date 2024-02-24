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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.config.UserConfigDAO;
import com.oceanbase.odc.metadb.config.UserConfigEntity;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@SkipAuthorize("isolated by user")
public class UserConfigService {
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private UserConfigMetaService userConfigMetaService;
    @Autowired
    private UserConfigDAO userConfigDAO;

    private List<Configuration> defaultConfigurations;
    private Map<String, ConfigurationMeta> configKeyToConfigMeta;

    @PostConstruct
    public void init() {
        List<ConfigurationMeta> allConfigMetas = userConfigMetaService.listAllConfigMetas();
        this.defaultConfigurations = allConfigMetas.stream().map(
                meta -> new Configuration(meta.getKey(), meta.getDefaultValue()))
                .collect(Collectors.toList());
        this.configKeyToConfigMeta = allConfigMetas.stream()
                .collect(Collectors.toMap(ConfigurationMeta::getKey, e -> e));
        log.info("Default user configurations: {}", defaultConfigurations);
    }

    public List<Configuration> listDefaultUserConfigurations() {
        return new ArrayList<>(defaultConfigurations);
    }

    public List<Configuration> listUserConfigurations(@NotNull Long userId) {
        Map<String, Configuration> keyToConfiguration =
                userConfigDAO.queryByUserId(userId).stream().map(Configuration::of)
                        .collect(Collectors.toMap(Configuration::getKey, e -> e));
        List<Configuration> configurations = listDefaultUserConfigurations();
        for (Configuration configuration : configurations) {
            if (keyToConfiguration.containsKey(configuration.getKey())) {
                configuration.setValue(keyToConfiguration.get(configuration.getKey()).getValue());
            }
        }
        return configurations;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Configuration> updateUserConfigurations(@NotNull Long userId,
            @NotEmpty List<Configuration> configurations) {
        for (Configuration configuration : configurations) {
            validateConfiguration(configuration);
        }
        List<UserConfigEntity> entities = configurations.stream()
                .map(c -> c.toEntity(userId)).collect(Collectors.toList());
        int affectRows = userConfigDAO.batchUpsert(entities);
        log.info("Update user configurations, userId={}, affectRows={}, configurations={}",
                userId, affectRows, configurations);
        return listUserConfigurations(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Configuration updateUserConfiguration(@NotNull Long userId, @NotNull Configuration configuration) {
        validateConfiguration(configuration);
        UserConfigEntity entity = configuration.toEntity(userId);
        int affectRows = userConfigDAO.batchUpsert(Arrays.asList(entity));
        log.info("Update user configuration, userId={}, affectRows={}, configuration={}",
                userId, affectRows, configuration);
        UserConfigEntity stored = userConfigDAO.queryByUserIdAndKey(userId, configuration.getKey());
        return Configuration.of(stored);
    }

    private void validateConfiguration(Configuration configuration) {
        ConfigurationMeta meta = configKeyToConfigMeta.get(configuration.getKey());
        if (Objects.isNull(meta)) {
            throw new IllegalArgumentException("Invalid configuration key: " + configuration.getKey());
        }
        ConfigValueValidator.validate(meta, configuration.getValue());
    }
}
