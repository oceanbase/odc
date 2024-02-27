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
package com.oceanbase.odc.plugin.connect.model;

import java.util.Map;

import lombok.Data;
import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/2/2
 */
@Data
public class JdbcUrlProperty {
    private String host;
    private Integer port;
    private String defaultSchema;
    private Map<String, String> jdbcParameters;
    /**
     * For oracle only
     */
    private String sid;
    private String serviceName;

    public JdbcUrlProperty(@NonNull String host, @NonNull Integer port, String defaultSchema,
            Map<String, String> jdbcParameters,
            String sid,
            String serviceName) {
        this.host = host;
        this.port = port;
        this.defaultSchema = defaultSchema;
        this.jdbcParameters = jdbcParameters;
        this.sid = sid;
        this.serviceName = serviceName;
    }

    public JdbcUrlProperty(@NonNull String host, @NonNull Integer port, String defaultSchema,
            Map<String, String> jdbcParameters) {
        this.host = host;
        this.port = port;
        this.defaultSchema = defaultSchema;
        this.jdbcParameters = jdbcParameters;
    }
}
