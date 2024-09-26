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
package com.oceanbase.odc.service.schedule;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "odc.schedule.log")
@Component
@RefreshScope
@Data
public class ScheduleLogProperties {
    /**
     * This is a root path that stores all flow task instance log file and schedule task log file
     */
    private String directory = "./log";

    /**
     * The limited of max lines can read from log file
     */
    private Long maxLines = 10000L;

    /**
     * unit：B
     */
    private Long maxSize = 1024L * 1024;
}
