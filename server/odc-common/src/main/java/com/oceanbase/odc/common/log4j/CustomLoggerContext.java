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
package com.oceanbase.odc.common.log4j;

import java.net.URI;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import com.oceanbase.odc.common.util.SystemUtils;

/**
 * @author keyang
 * @date 2025/04/01
 * @since 4.3.3
 */
public class CustomLoggerContext extends LoggerContext {
    public CustomLoggerContext(String name) {
        super(name);
    }

    public CustomLoggerContext(String name, Object externalContext) {
        super(name, externalContext);
    }

    public CustomLoggerContext(String name, Object externalContext, URI configLocn) {
        super(name, externalContext, configLocn);
    }

    public CustomLoggerContext(String name, Object externalContext, String configLocn) {
        super(name, externalContext, configLocn);
    }

    @Override
    public Configuration setConfiguration(final Configuration config) {
        if (config == null) {
            LOGGER.error("No configuration found for context.");
            return super.getConfiguration();
        }
        final ConcurrentMap<String, String> map = config.getComponent(Configuration.CONTEXT_PROPERTIES);
        map.putIfAbsent("hostName", "unknown");
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            map.putIfAbsent("hostName", SystemUtils.getHostName());
        });
        return super.setConfiguration(config);
    }
}
