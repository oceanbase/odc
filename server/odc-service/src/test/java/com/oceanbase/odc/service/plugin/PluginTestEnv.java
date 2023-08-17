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

import java.nio.file.Paths;

public class PluginTestEnv {

    static {
        String pluginPath = Paths.get("").toAbsolutePath().getParent().getParent()
                .resolve("distribution/plugins").toFile().getAbsolutePath();
        System.setProperty(PluginProperties.PLUGIN_DIR_KEY, pluginPath);
    }

}
