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
package com.oceanbase.odc.service.plugin;

import java.util.Map;
import java.util.stream.Collectors;

import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.schema.api.BaseSchemaPlugin;

/**
 * @author jingtian
 * @date 2023/7/21
 * @since 4.2.0
 */
public class SchemaPluginFinder implements PluginFinder<DialectType> {
    private volatile Map<DialectType, String> dialect2PluginId;

    private final PluginManager pluginManager;

    public SchemaPluginFinder(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        init();
    }

    protected void init() {
        this.dialect2PluginId = pluginManager.getPlugins()
                .stream().filter(p -> p.getPlugin() instanceof BaseSchemaPlugin)
                .collect(Collectors.toMap(
                        p -> ((BaseSchemaPlugin) p.getPlugin()).getDialectType(),
                        PluginWrapper::getPluginId));
        if (dialect2PluginId.isEmpty()) {
            throw new IllegalStateException("BaseSchemaPlugin is empty.");
        }
    }

    @Override
    public String findPluginIdBy(DialectType dialectType) {
        String pluginId = dialect2PluginId.get(dialectType);
        if (pluginId != null) {
            return pluginId;
        }
        throw new UnsupportedOperationException("Dialect type " + dialectType + " is not supported yet.");
    }
}
