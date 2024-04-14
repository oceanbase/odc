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

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class UserConfigFacadeImpl implements UserConfigFacade {
    @Autowired
    private UserConfigService userConfigService;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Override
    public String getUserConfig(String key) {
        long currentUserId = authenticationFacade.currentUserId();
        Map<String, Configuration> userIdToConfigurations =
                userConfigService.getUserConfigurationsFromCache(currentUserId);
        Configuration configuration = userIdToConfigurations.get(key);
        if (Objects.isNull(configuration)) {
            throw new IllegalStateException("User configuration not found: " + key);
        }
        return configuration.getValue();
    }

    @Override
    public String getDefaultDelimiter() {
        return getUserConfig(UserConfigKeys.DEFAULT_DELIMITER);
    }

    @Override
    public String getMysqlAutoCommitMode() {
        return getUserConfig(UserConfigKeys.DEFAULT_MYSQL_AUTO_COMMIT_MODE);
    }

    @Override
    public String getOracleAutoCommitMode() {
        return getUserConfig(UserConfigKeys.DEFAULT_ORACLE_AUTO_COMMIT_MODE);
    }

    @Override
    public Integer getDefaultQueryLimit() {
        return Integer.parseInt(getUserConfig(UserConfigKeys.DEFAULT_QUERY_LIMIT));
    }

    @Override
    public boolean isFullLinkTraceEnabled() {
        return getUserConfig(UserConfigKeys.DEFAULT_FULL_LINK_TRACE_ENABLED).equalsIgnoreCase("true");
    }

    @Override
    public boolean isContinueExecutionOnError() {
        return getUserConfig(UserConfigKeys.DEFAULT_CONTINUE_EXECUTION_ON_ERROR).equalsIgnoreCase("true");
    }

}
