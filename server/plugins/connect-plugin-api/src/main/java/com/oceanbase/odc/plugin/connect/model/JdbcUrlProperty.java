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
    // 数据库端口号
    private Integer port;
    // 默认模式
    private String defaultSchema;
    // JDBC参数
    private Map<String, String> jdbcParameters;
    /**
     * For oracle only
     */
    // Oracle System Identifier 在oracle8i以前，标识数据库的一个实例。sid是对内的，是实例级别的
    private String sid;
    // SERVICE_name是对外的，是数据库级别的一个名字，用来告诉外面的人，我数据库叫"SERVICE_NAME"
    private String serviceName;

    // 构造函数2，不包含sid和serviceName属性
    public JdbcUrlProperty(@NonNull String host, @NonNull Integer port, String defaultSchema,
            Map<String, String> jdbcParameters, String sid, String serviceName) {
        this.host = host;
        this.port = port;
        this.defaultSchema = defaultSchema;
        this.jdbcParameters = jdbcParameters;
        this.sid = sid;
        this.serviceName = serviceName;
    }

    // 构造函数2，不包含sid和serviceName属性
    public JdbcUrlProperty(@NonNull String host, @NonNull Integer port, String defaultSchema,
            Map<String, String> jdbcParameters) {
        this.host = host;
        this.port = port;
        this.defaultSchema = defaultSchema;
        this.jdbcParameters = jdbcParameters;
    }
}
