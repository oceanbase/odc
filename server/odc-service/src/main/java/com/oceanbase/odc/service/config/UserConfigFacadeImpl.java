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

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.config.UserConfigDO;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.OrganizationConfig;
import com.oceanbase.odc.service.config.model.UserConfig;
import com.oceanbase.odc.service.config.util.ConfigObjectUtil;
import com.oceanbase.odc.service.config.util.OrganizationConfigUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * Implement of OdcUserConfigFacade
 *
 * @author yh263208
 * @date 2021-05-31 19:27
 * @since ODC-release_2.4.2
 */
@Service
@Slf4j
@RefreshScope
@SkipAuthorize("personal resource")
public class UserConfigFacadeImpl implements UserConfigFacade {
    /**
     * user config service object
     */
    @Autowired
    private UserConfigService service;

    @Autowired
    private OrganizationConfigService organizationConfigService;

    @Autowired
    private AuthenticationFacade authenticationFacade;
    /**
     * Config Cache
     */
    private final LoadingCache<Long, UserConfig> configCache;

    public UserConfigFacadeImpl(
            @Value("${odc.config.userConfig.cacheRefreshTimeSeconds:60}") long refreshTimeInSeconds) {
        configCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofSeconds(refreshTimeInSeconds))
                .build(this::query);
    }

    /**
     * Query User config from thread local
     *
     * @return config object
     */
    @Override
    public UserConfig currentUserConfig() {
        return configCache.get(authenticationFacade.currentUserId());
    }

    /**
     * Query User config from Cache
     *
     * @param userId user id
     * @return config object
     */
    @Override
    public UserConfig queryByCache(Long userId) {
        return configCache.get(userId);
    }

    /**
     * get all personal config
     *
     * @param userId user id
     * @return config map
     */
    @Override
    public UserConfig query(Long userId) throws UnexpectedException {
        List<UserConfigDO> settingsInDb = service.query(userId);
        List<Configuration> organizationConfigDTO =
                organizationConfigService.internalQuery(authenticationFacade.currentOrganizationId());
        OrganizationConfig organizationConfig = OrganizationConfigUtil.convertToConfig(organizationConfigDTO);
        UserConfig userConfig = new UserConfig(organizationConfig);
        return ConfigObjectUtil.setConfigObjectFromDO(settingsInDb, userConfig);
    }

    /**
     * set a user config item
     *
     * @param userId user id
     * @param userConfig config object
     */
    @Override
    public UserConfig put(Long userId, UserConfig userConfig)
            throws UnexpectedException {
        log.info("Set a user config item, userConfig={}", userConfig);
        List<UserConfigDO> configDOList = ConfigObjectUtil.convertToDO(userConfig);
        for (UserConfigDO configDO : configDOList) {
            String valueInDb = service.query(userId, configDO.getKey());
            configDO.setUserId(userId);
            if (valueInDb != null) {
                if (!valueInDb.equals(configDO.getValue())) {
                    service.update(configDO);
                }
            } else {
                service.insert(configDO);
            }
        }
        configCache.invalidate(userId);
        log.info("Set a user config item successfully, userConfig={}", userConfig);
        return userConfig;
    }

    /**
     * Apply user config
     *
     * @param operator operator for user config
     * @return config which performed
     */
    @Override
    public UserConfig apply(UserConfigOperator operator) {
        UserConfig userConfig = currentUserConfig();
        if (userConfig == null) {
            log.warn("Fail to get UserConfig from session, userConfig==null");
            return null;
        }
        try {
            operator.perform(userConfig);
        } catch (Throwable e) {
            log.error("Some errors happened when performing config, userConfig={}", userConfig, e);
        }
        return userConfig;
    }
}
