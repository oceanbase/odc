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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import com.oceanbase.odc.common.util.ApplicationPidLoader;

/**
 * @author keyang
 * @date 2025/04/01
 * @since 4.3.3
 */
public class CustomLoggingSystemProperties extends LoggingSystemProperties {
    public CustomLoggingSystemProperties(Environment environment) {
        super(environment);
    }

    public CustomLoggingSystemProperties(Environment environment, BiConsumer<String, String> setter) {
        super(environment, setter);
    }

    @Override
    protected void apply(LogFile logFile, PropertyResolver resolver) {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            setSystemProperty(PID_KEY, ApplicationPidLoader.INSTANCE.getApplicationPid());
        });
        setSystemProperty(resolver, EXCEPTION_CONVERSION_WORD, "logging.exception-conversion-word");
        setSystemProperty(resolver, CONSOLE_LOG_PATTERN, "logging.pattern.console");
        setSystemProperty(resolver, CONSOLE_LOG_CHARSET, "logging.charset.console", getDefaultCharset().name());
        setSystemProperty(resolver, LOG_DATEFORMAT_PATTERN, "logging.pattern.dateformat");
        setSystemProperty(resolver, FILE_LOG_PATTERN, "logging.pattern.file");
        setSystemProperty(resolver, FILE_LOG_CHARSET, "logging.charset.file", getDefaultCharset().name());
        setSystemProperty(resolver, LOG_LEVEL_PATTERN, "logging.pattern.level");
        if (logFile != null) {
            logFile.applyToSystemProperties();
        }
    }
}
