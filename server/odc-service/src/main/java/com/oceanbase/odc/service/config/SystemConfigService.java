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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.config.SystemConfigDAO;
import com.oceanbase.odc.metadb.config.SystemConfigEntity;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.util.ConfigurationUtils;
import com.oceanbase.odc.service.systemconfig.SystemConfigRefreshMatcher;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/7/27 下午8:29
 * @Description: []
 */
@Service
@Slf4j
public class SystemConfigService {
    private static final String[] SENSITIVE_KEYS = {"key", "secret", "password"};
    private static final String SENSITIVE_MASK_VALUE = "******";

    @Autowired
    private SystemConfigDAO systemConfigDAO;

    @Autowired
    private ContextRefresher contextRefresher;

    @Autowired
    private List<SystemConfigRefreshMatcher> systemConfigRefreshMatchers;

    /**
     * odc system configuration value stored in metadb by default, <br>
     * but sometimes the value of version config may be overwritten, <br>
     * e.g. in aliyun cloud, we disable data_export feature for jushita cloud.
     */
    private List<Consumer<Configuration>> configurationConsumers = new ArrayList<>();

    @SkipAuthorize("odc internal usage")
    public void addConfigurationConsumer(Consumer<Configuration> consumer) {
        this.configurationConsumers.add(consumer);
    }

    @SkipAuthorize("odc internal usage")
    public List<Consumer<Configuration>> getConfigurationConsumer() {
        return this.configurationConsumers;
    }

    @SkipAuthorize("public readonly resource")
    public List<Configuration> listAll() {
        return queryByKeyPrefix("");
    }

    /**
     * query all, will mask sensitive value
     */
    @SkipAuthorize("public readonly resource")
    public List<Configuration> query() {
        List<Configuration> allConfigs = listAll();
        for (Configuration config : allConfigs) {
            for (Consumer<Configuration> consumer : getConfigurationConsumer()) {
                consumer.accept(config);
            }
            if (isSensitive(config.getKey())) {
                config.setValue(SENSITIVE_MASK_VALUE);
            }
        }
        return allConfigs;
    }

    private boolean isSensitive(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (StringUtils.containsIgnoreCase(key, sensitiveKey)) {
                return true;
            }
        }
        return false;
    }

    @SkipAuthorize("odc internal usage")
    public List<Configuration> queryByKeyPrefix(String keyPrefix) {
        List<SystemConfigEntity> configEntities = systemConfigDAO.queryByKeyPrefix(keyPrefix);
        return ConfigurationUtils.fromEntity(configEntities).stream().peek(config -> {
            for (Consumer<Configuration> consumer : getConfigurationConsumer()) {
                consumer.accept(config);
            }
        }).collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usage")
    public Configuration queryByKey(String key) {
        SystemConfigEntity systemConfigEntity = systemConfigDAO.queryByKey(key);
        Configuration config = ConfigurationUtils.fromEntity(systemConfigEntity);
        for (Consumer<Configuration> consumer : getConfigurationConsumer()) {
            consumer.accept(config);
        }
        return config;
    }

    @SkipAuthorize("odc internal usage")
    public synchronized void refresh() {
        if (needRefresh()) {
            contextRefresher.refresh();
            log.info("refresh system configuration succeed");
        } else {
            log.debug("no system configuration changes detected");
        }
    }

    @SkipAuthorize("public readonly resource")
    @Transactional(rollbackFor = Exception.class)
    public void insert(@NotNull List<SystemConfigEntity> entities) {
        entities.forEach(entity -> systemConfigDAO.insert(entity));
    }

    private boolean needRefresh() {
        boolean needRefresh = false;
        for (SystemConfigRefreshMatcher matcher : systemConfigRefreshMatchers) {
            if (matcher.needRefresh()) {
                needRefresh = true;
            }
        }
        return needRefresh;
    }

    @SkipAuthorize("public readonly resource")
    @Transactional(rollbackFor = Exception.class)
    public void upsert(@NotNull List<SystemConfigEntity> entities) {
        entities.forEach(entity -> systemConfigDAO.upsert(entity));
    }

}
