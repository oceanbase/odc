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

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.config.model.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserConfigService {

    public List<Configuration> listDefaultUserConfigurations() {
        return Collections.emptyList();
    }

    public List<Configuration> listUserConfigurations(Long userId) {
        return Collections.emptyList();
    }

    public Configuration getUserConfig(Long userId, String key) {
        return null;
    }

    public List<Configuration> updateUserConfigurations(Long userId, List<Configuration> configurations) {
        return Collections.emptyList();
    }

    public Configuration updateUserConfiguration(Long userId, Configuration configuration) {
        return configuration;
    }
}
