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

package com.oceanbase.odc.test.database;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/9/27 13:53
 */
public enum TestDBType {

    OB_MYSQL("4.1.0.2", "odc.ob.default.mysql.commandline",
            "odc.ob.default.mysql.sysUsername", "odc.ob.default.mysql.sysPassword"),
    OB_ORACLE("4.1.0.2", "odc.ob.default.oracle.commandline",
            "odc.ob.default.oracle.sysUsername", "odc.ob.default.oracle.sysPassword"),
    MYSQL("5.7", "odc.mysql.default.commandline", null, null);

    /**
     * Test database version
     */
    public final String version;

    /**
     * Test database commandline key
     */
    public final String commandlineKey;

    /**
     * Test database system tenant username key (only used for OB_MYSQL and OB_ORACLE)
     */
    public final String sysUserNameKey;

    /**
     * Test database system tenant user password key (only used for OB_MYSQL and OB_ORACLE)
     */
    public final String sysUserPasswordKey;

    TestDBType(@NonNull String version, @NonNull String commandlineKey, String sysUserNameKey,
            String sysUserPasswordKey) {
        this.version = version;
        this.commandlineKey = commandlineKey;
        this.sysUserNameKey = sysUserNameKey;
        this.sysUserPasswordKey = sysUserPasswordKey;
    }

    public boolean isMySQLMode() {
        return this == OB_MYSQL || this == MYSQL;
    }

    public boolean isOracleMode() {
        return this == OB_ORACLE;
    }

    public boolean isOBMode() {
        return this == OB_MYSQL || this == OB_ORACLE;
    }

}
