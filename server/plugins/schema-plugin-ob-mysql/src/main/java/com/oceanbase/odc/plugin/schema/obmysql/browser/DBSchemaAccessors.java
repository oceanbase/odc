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
package com.oceanbase.odc.plugin.schema.obmysql.browser;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween220And225XSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween2260And2276SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween2277And3XSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLNoGreaterThan1479SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLSchemaAccessor;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/28
 * @since 4.2.0
 */
public class DBSchemaAccessors {

    public static DBSchemaAccessor create(@NonNull JdbcOperations jdbcOperations, @NonNull String dbVersion) {
        return create(jdbcOperations, dbVersion, null);
    }

    public static DBSchemaAccessor create(@NonNull JdbcOperations jdbcOperations,
            @NonNull String dbVersion, String tenantName) {
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "4.0.0")) {
            // OB 版本 >= 4.0.0
            return new OBMySQLSchemaAccessor(jdbcOperations);
        } else if (VersionUtils.isGreaterThan(dbVersion, "2.2.76")) {
            // OB 版本为 [2.2.77, 4.0.0)
            return new OBMySQLBetween2277And3XSchemaAccessor(jdbcOperations);
        } else if (VersionUtils.isGreaterThan(dbVersion, "2.2.60")) {
            // OB 版本为 [2.2.60, 2.2.77)
            return new OBMySQLBetween2260And2276SchemaAccessor(jdbcOperations);
        } else if (VersionUtils.isGreaterThan(dbVersion, "1.4.79")) {
            // OB 版本为 (1.4.79, 2.2.60)
            return new OBMySQLBetween220And225XSchemaAccessor(jdbcOperations);
        } else {
            // OB 版本 <= 1.4.79
            // sysJdbcOperations and tenantName is only used in getPartition method, this method will not be
            // called in plugin, so we just set it null here.
            return new OBMySQLNoGreaterThan1479SchemaAccessor(jdbcOperations, null, tenantName);
        }
    }

}
