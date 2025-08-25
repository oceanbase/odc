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
package com.oceanbase.odc.service.db.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/8/25 12:15
 * @since: 4.4.1
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "odc.external.resource")
public class DBExternalResourceProperties {

    private Long uploadBytesLimit = 512 * 1024 * 1024L;
    private Long downloadBytesLimit = 512 * 1024 * 1024L;
    private Long getContentBytesLimit = 1 * 1024 * 1024L;
    private Integer concurrencyNumberLimit = 16;
    private Long waitLockTimeoutMillSeconds = 10 * 1000L;

}
