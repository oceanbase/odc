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
package com.oceanbase.tools.dbbrowser.schema;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.schema.doris.DorisSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.MySQLNoLessThan5600SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.MySQLNoLessThan5700SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween220And225XSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween2260And2276SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween2277And3XSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLNoGreaterThan1479SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.ODPOBMySQLSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleBetween4000And4100SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleLessThan2270SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleLessThan400SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OracleSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.ALLDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/4/16
 */
public class DBSchemaAccessors {
    public static DBSchemaAccessor createForOBOracle(@NonNull JdbcOperations jdbcOperations,
            @NonNull String dbVersion) {
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "4.1.0")) {
            // OB 版本 >= 4.1.0
            return new OBOracleSchemaAccessor(jdbcOperations, new ALLDataDictTableNames());
        } else if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "4.0.0")) {
            // OB 版本为 [4.0.0, 4.1.0)
            return new OBOracleBetween4000And4100SchemaAccessor(jdbcOperations, new ALLDataDictTableNames());
        } else if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "2.2.70")) {
            // OB 版本为 [2.2.7, 4.0.0)
            return new OBOracleLessThan400SchemaAccessor(jdbcOperations, new ALLDataDictTableNames());
        } else {
            // OB 版本 < 2.2.70
            return new OBOracleLessThan2270SchemaAccessor(jdbcOperations, new ALLDataDictTableNames());
        }
    }

    public static DBSchemaAccessor createForOBMySQL(@NonNull JdbcOperations jdbcOperations,
            JdbcOperations sysJdbcOperations, @NonNull String dbVersion, String tenantName) {
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
            return new OBMySQLNoGreaterThan1479SchemaAccessor(jdbcOperations, sysJdbcOperations, tenantName);
        }
    }

    public static DBSchemaAccessor createForOracle(@NonNull JdbcOperations jdbcOperations) {
        return new OracleSchemaAccessor(jdbcOperations, new ALLDataDictTableNames());
    }

    public static DBSchemaAccessor createForMySQL(@NonNull JdbcOperations jdbcOperations, @NonNull String dbVersion) {
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "5.7.0")) {
            return new MySQLNoLessThan5700SchemaAccessor(jdbcOperations);
        } else if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "5.6.0")) {
            return new MySQLNoLessThan5600SchemaAccessor(jdbcOperations);
        } else {
            throw new UnsupportedOperationException(String.format("MySQL version '%s' not supported", dbVersion));
        }
    }

    public static DBSchemaAccessor createForDoris(@NonNull JdbcOperations jdbcOperations, @NonNull String dbVersion) {
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "5.7.0")) {
            return new DorisSchemaAccessor(jdbcOperations);
        } else {
            throw new UnsupportedOperationException(String.format("Doris version '%s' not supported", dbVersion));
        }
    }

    public static DBSchemaAccessor createForODPOBMySQL(@NonNull JdbcOperations jdbcOperations) {
        return new ODPOBMySQLSchemaAccessor(jdbcOperations);
    }
}
