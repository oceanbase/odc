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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.model.ConfigurationMeta;

/**
 * @Author: Lebie
 * @Date: 2025/5/21 15:20
 * @Description: []
 */
public class UserConfigServiceTest extends ServiceTestEnv {
    @Autowired
    private UserConfigService userConfigService;

    @Autowired
    private UserConfigMetaService metaService;

    @Test
    public void testListDefaultUserConfigurations_WithoutConsumers_Success() {
        Map<String, String> actual = userConfigService.listDefaultUserConfigurations().stream().collect(
                Collectors.toMap(config -> config.getKey(), config -> config.getValue()));
        List<ConfigurationMeta> expected = metaService.listAllConfigMetas();
        Assert.assertEquals(expected.size(), actual.size());
        for (ConfigurationMeta meta : expected) {
            Assert.assertTrue(actual.containsKey(meta.getKey()));
            Assert.assertEquals(meta.getDefaultValue(), actual.get(meta.getKey()));
        }
    }

    @Test
    public void testListDefaultUserConfigurations_WithConsumers_Success() {
        userConfigService.addDefaultConfigurationConsumer(this::consumeUserConfig);
        Map<String, String> actual = userConfigService.listDefaultUserConfigurations().stream().collect(
                Collectors.toMap(config -> config.getKey(), config -> config.getValue()));
        Assert.assertTrue(actual.containsKey("odc.editor.shortcut.executeStatement"));
        Assert.assertEquals("5,3", actual.get("odc.editor.shortcut.executeStatement"));

    }

    private void consumeUserConfig(Configuration configuration) {
        String key = configuration.getKey();
        if (StringUtils.equals("odc.editor.shortcut.executeStatement", key)) {
            configuration.setValue("5,3");
        }
    }
}
