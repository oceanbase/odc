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
package com.oceanbase.odc.service.logger;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;

@ConfigurationProperties(prefix = "odc.server.web.log")
@Component
@Getter
public class LoggerProperty {

    // This is the directory path for storing temporary log files from scheduled tasks pods, defaulting
    // to the classpath if not explicitly set.
    private String tempScheduleTaskLogDir;

    private Long maxLimitedCount = 10000L;

    // unit：B
    private Long maxSizeCount = 1024L * 1024;
}
