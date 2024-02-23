/*
 * Copyright (c) 2024 OceanBase.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

@Service
public class UserConfigFacadeImpl implements UserConfigFacade {
    @Autowired
    private UserConfigService userConfigService;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Override
    public String getUserConfig(String key) {
        return userConfigService.getUserConfig(authenticationFacade.currentUserId(), key).getValue();
    }

    @Override
    public String getDefaultDelimiter() {
        return null;
    }

    @Override
    public String getMysqlAutoCommitMode() {
        return null;
    }

    @Override
    public String getOracleAutoCommitMode() {
        return null;
    }

    @Override
    public Integer getDefaultQueryLimit() {
        return null;
    }
}
