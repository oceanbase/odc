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
import com.oceanbase.odc.plugin.connect.api.BaseConnectionPlugin;

class ConnectPluginFinder implements PluginFinder<DialectType> {

    private volatile Map<DialectType, String> dialect2PluginId;

    private final PluginManager pluginManager;

    public ConnectPluginFinder(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        init();
    }

    protected void init() {
        this.dialect2PluginId = pluginManager.getPlugins()
                .stream().filter(p -> p.getPlugin() instanceof BaseConnectionPlugin)
                .collect(Collectors.toMap(
                        p -> ((BaseConnectionPlugin) p.getPlugin()).getDialectType(),
                        PluginWrapper::getPluginId));
        if (dialect2PluginId.isEmpty()) {
            throw new IllegalStateException("BaseConnectionPlugin is empty.");
        }
    }

    @Override
    public String findPluginIdBy(DialectType dialectType) {
        if (DialectType.ODP_SHARDING_OB_MYSQL.equals(dialectType)) {
            return findPluginIdBy(DialectType.OB_MYSQL);
        }
        String pluginId = dialect2PluginId.get(dialectType);
        if (pluginId != null) {
            return pluginId;
        }
        throw new UnsupportedOperationException("Dialect type " + dialectType + " is not supported yet.");
    }

}
