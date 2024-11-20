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

import com.oceanbase.odc.common.util.StringUtils;

/**
 * @auther kuiseng.zhb
 */
public enum DialectType {

    OB_MYSQL,
    OB_ORACLE,
    ORACLE,
    MYSQL,
    ODP_SHARDING_OB_MYSQL,
    DORIS,
    POSTGRESQL,
    FILE_SYSTEM,
    UNKNOWN,
    ;

    public static DialectType fromValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return valueOf(value);
    }

    public String getDBBrowserDialectTypeName() {
        return this.name();
    }

    public boolean isMysql() {
        return OB_MYSQL == this || MYSQL == this || ODP_SHARDING_OB_MYSQL == this;
    }

    public boolean isOBMysql() {
        return OB_MYSQL == this || ODP_SHARDING_OB_MYSQL == this;
    }

    public boolean isOracle() {
        return OB_ORACLE == this || ORACLE == this;
    }

    public boolean isDoris() {
        return DORIS == this;
    }

    public boolean isOceanbase() {
        return OB_MYSQL == this || OB_ORACLE == this || ODP_SHARDING_OB_MYSQL == this;
    }

    public boolean isPostgreSql() {
        return POSTGRESQL == this;
    }

}
