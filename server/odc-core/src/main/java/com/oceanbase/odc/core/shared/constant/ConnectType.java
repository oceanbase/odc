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
package com.oceanbase.odc.core.shared.constant;

/**
 * 连接类型
 * 
 * @since 3.3.0
 */
public enum ConnectType {

    // current supported
    OB_MYSQL(DialectType.OB_MYSQL),
    OB_ORACLE(DialectType.OB_ORACLE),
    CLOUD_OB_MYSQL(DialectType.OB_MYSQL),
    CLOUD_OB_ORACLE(DialectType.OB_ORACLE),
    ODP_SHARDING_OB_MYSQL(DialectType.ODP_SHARDING_OB_MYSQL),
    MYSQL(DialectType.MYSQL),
    DORIS(DialectType.DORIS),

    // reserved for future version
    ODP_SHARDING_OB_ORACLE(DialectType.OB_ORACLE),
    ORACLE(DialectType.ORACLE),
    UNKNOWN(DialectType.UNKNOWN),

    ;

    private final DialectType dialectType;

    ConnectType(DialectType dialectType) {
        this.dialectType = dialectType;
    }

    public DialectType getDialectType() {
        return this.dialectType;
    }

    public boolean isODPSharding() {
        return this == ODP_SHARDING_OB_MYSQL || this == ODP_SHARDING_OB_ORACLE;
    }

    public boolean isDefaultSchemaRequired() {
        return isODPSharding();
    }

    public static ConnectType from(DialectType dialectType) {
        return dialectType == null ? null : ConnectType.valueOf(dialectType.toString());
    }

    public boolean isCloud() {
        return this == CLOUD_OB_MYSQL || this == CLOUD_OB_ORACLE;
    }

}
