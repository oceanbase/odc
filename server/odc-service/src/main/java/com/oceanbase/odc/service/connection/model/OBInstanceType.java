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
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/10/25 下午4:56
 * @Description: [公有云/多云 实例类型，公有云支持集群实例、MySQL 租户实例 和 Oracle 租户实例；多云目前只有 集群实例、租户实例、AP实例]
 */
@Slf4j
public enum OBInstanceType implements Translatable {
    CLUSTER("cluster"),
    MYSQL_TENANT("MYSQL_TENANT", "mtenant"),
    ORACLE_TENANT("ORACLE_TENANT", "otenant"),
    MYSQL_SERVERLESS("mtenant_serverless"),
    ORACLE_SERVERLESS("otenant_serverless"),
    DEDICATED("DEDICATED"),
    SHARED("SHARED"),
    // K8s独占集群模式
    K8s_DEDICATED("K8s_DEDICATED"),
    // K8s共享集群模式
    K8s_SHARED("K8s_SHARED"),
    ANALYTICAL_CLUSTER("ANALYTICAL_CLUSTER"),
    KV_CLUSTER("KV_CLUSTER"),
    BACKUP("backup"),
    PRIMARY_STANDBY_CLUSTER("primary_standby_cluster"),
    UNKNOWN("UNKNOWN");
    ;

    @Getter
    private final String[] values;

    @JsonValue
    public String getName() {
        return this.name();
    }

    OBInstanceType(String... values) {
        this.values = values;
    }

    @JsonCreator
    public static OBInstanceType fromValue(String value) {
        for (OBInstanceType instanceType : OBInstanceType.values()) {
            for (String type : instanceType.values) {
                if (StringUtils.equalsIgnoreCase(type, value)) {
                    return instanceType;
                }
            }
        }
        log.warn("Unknown OBInstanceType: {}", value);
        return UNKNOWN;
    }

    @Override
    public String code() {
        return name();
    }

    public boolean isTenantInstance() {
        return this == MYSQL_TENANT || this == ORACLE_TENANT || this == MYSQL_SERVERLESS
                || this == ORACLE_SERVERLESS;
    }
}
