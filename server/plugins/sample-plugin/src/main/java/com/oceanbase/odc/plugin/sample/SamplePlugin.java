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
package com.oceanbase.odc.plugin.sample;

import org.pf4j.Plugin;
import org.pf4j.PluginManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SamplePlugin extends Plugin {

    /**
     * This method is called by the application when the plugin is started. See
     * {@link PluginManager#startPlugin(String)}.
     */
    @Override
    public void start() {
        log.info("Sample plugin is started");
    }

    /**
     * This method is called by the application when the plugin is stopped. See
     * {@link PluginManager#stopPlugin(String)}.
     */
    @Override
    public void stop() {
        log.info("Sample plugin is stopped");
    }

    /**
     * This method is called by the application when the plugin is deleted. See
     * {@link PluginManager#deletePlugin(String)}.
     */
    @Override
    public void delete() {
        log.info("Sample plugin is deleted");
    }

}
