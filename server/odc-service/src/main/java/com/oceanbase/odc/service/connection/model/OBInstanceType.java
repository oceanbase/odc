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
package com.oceanbase.odc.service.connection.model;

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oceanbase.odc.common.i18n.Translatable;

import lombok.Getter;

/**
 * @Author: Lebie
 * @Date: 2022/10/25 下午4:56
 * @Description: [公有云/多云 实例类型，公有云支持集群实例、MySQL 租户实例 和 Oracle 租户实例；多云目前只有 集群实例]
 */
public enum OBInstanceType implements Translatable {
    CLUSTER("cluster"),
    MYSQL_TENANT("mtenant"),
    ORACLE_TENANT("otenant"),
    MYSQL_SERVERLESS("mtenant_serverless"),
    ORACLE_SERVERLESS("otenant_serverless");

    @Getter
    private String value;

    @JsonValue
    public String getName() {
        return this.name();
    }

    OBInstanceType(String value) {
        this.value = value;
    }

    @JsonCreator
    public static OBInstanceType fromValue(String value) {
        for (OBInstanceType instanceType : OBInstanceType.values()) {
            if (StringUtils.equalsIgnoreCase(instanceType.value, value)) {
                return instanceType;
            }
        }
        throw new IllegalArgumentException("OBInstanceType value not supported, given value '" + value + "'");
    }

    @Override
    public String code() {
        return name();
    }

}
