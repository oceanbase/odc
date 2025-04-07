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

import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author keyang
 * @date 2025/04/01
 * @since 4.3.3
 */
public class CustomLoggingSystem extends Log4J2LoggingSystem {
    public CustomLoggingSystem(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public LoggingSystemProperties getSystemProperties(ConfigurableEnvironment environment) {
        return new CustomLoggingSystemProperties(environment);
    }
}
