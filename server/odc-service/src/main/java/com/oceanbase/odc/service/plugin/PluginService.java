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

import java.nio.file.Path;
import java.util.stream.Collectors;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.DialectType;

/**
 * @author yaobin
 * @date 2023-04-21
 * @since 4.2.0
 */
@Service
@SkipAuthorize
public class PluginService {

    private static final PluginManager PLUGIN_MANAGER;
    private static final OdcPluginManager<DialectType> CONNECTION_PLUGIN_MANAGER;
    private static final OdcPluginManager<DialectType> SCHEMA_PLUGIN_MANAGER;
    private static final OdcPluginManager<DialectType> TASK_PLUGIN_MANAGER;


    static {
        PluginProperties properties = new PluginProperties();
        PLUGIN_MANAGER = new DefaultPluginManager(properties.getPluginDirs());
        PLUGIN_MANAGER.loadPlugins();
        if (PLUGIN_MANAGER.getResolvedPlugins().isEmpty()) {
            throw new IllegalStateException(
                    "No plugins been found in rootPaths: " +
                            properties.getPluginDirs().stream().map(Path::toString).collect(Collectors.joining(",")));
        }
        PLUGIN_MANAGER.startPlugins();
        CONNECTION_PLUGIN_MANAGER = createConnectPluginManager();
        SCHEMA_PLUGIN_MANAGER = createSchemaPluginManager();
        TASK_PLUGIN_MANAGER = createTaskPluginManager();
    }

    public PluginManager getPluginManager() {
        return PLUGIN_MANAGER;
    }

    public OdcPluginManager<DialectType> getConnectionPluginManager() {
        return CONNECTION_PLUGIN_MANAGER;
    }

    public OdcPluginManager<DialectType> getSchemaPluginManager() {
        return SCHEMA_PLUGIN_MANAGER;
    }

    public OdcPluginManager<DialectType> getTaskPluginManager() {
        return TASK_PLUGIN_MANAGER;
    }

    private static OdcPluginManager<DialectType> createConnectPluginManager() {
        ConnectPluginFinder finder = new ConnectPluginFinder(PLUGIN_MANAGER);
        return new OdcPluginManager<>(PLUGIN_MANAGER, finder);
    }

    private static OdcPluginManager<DialectType> createSchemaPluginManager() {
        SchemaPluginFinder finder = new SchemaPluginFinder(PLUGIN_MANAGER);
        return new OdcPluginManager<>(PLUGIN_MANAGER, finder);
    }

    private static OdcPluginManager<DialectType> createTaskPluginManager() {
        TaskPluginFinder finder = new TaskPluginFinder(PLUGIN_MANAGER);
        return new OdcPluginManager<>(PLUGIN_MANAGER, finder);
    }

}
