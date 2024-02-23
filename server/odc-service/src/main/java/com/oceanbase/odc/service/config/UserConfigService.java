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
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @PostConstruct
    public void init() {
        List<ConfigurationMeta> allConfigMetas = userConfigMetaService.listAllConfigMetas();
        this.defaultConfigurations = allConfigMetas.stream().map(
                meta -> new Configuration(meta.getKey(), meta.getDefaultValue()))
                .collect(Collectors.toList());
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
        return Collections.emptyList();
    }

    public Configuration updateUserConfiguration(@NotNull Long userId, @NotNull Configuration configuration) {
        return configuration;
    }
}
