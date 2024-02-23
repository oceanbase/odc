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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;

@Service
public class UserConfigMetaService {
    private Map<String, ConfigurationMeta> keyToConfigMetaMap = new HashMap<>();

    @PostConstruct
    public void init() {
        List<ConfigurationMeta> configurations =
                YamlUtils.fromYamlList("user-config-meta.yml", ConfigurationMeta.class);
        for (ConfigurationMeta configuration : configurations) {
            keyToConfigMetaMap.put(configuration.getKey(), configuration);
        }
    }

    public List<ConfigurationMeta> listAllConfigMetas() {
        return new ArrayList<>(keyToConfigMetaMap.values());
    }

    ConfigurationMeta getConfigMeta(String key) {
        ConfigurationMeta configurationMeta = keyToConfigMetaMap.get(key);
        if (configurationMeta == null) {
            throw new IllegalArgumentException("Invalid configuration key: " + key);
        }
        return configurationMeta;
    }

}
