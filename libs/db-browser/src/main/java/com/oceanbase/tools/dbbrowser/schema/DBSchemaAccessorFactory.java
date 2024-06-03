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

import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
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

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBSchemaAccessorFactory extends AbstractDBBrowserFactory<DBSchemaAccessor> {

    public static final String TENANT_NAME_KEY = "tenantName";
    public static final String SYS_OPERATIONS_KEY = "sysOperations";

    private String dbVersion;
    private JdbcOperations jdbcOperations;
    private Map<String, Object> properties;

    @Override
    public DBSchemaAccessor buildForMySQL() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "5.7.0")) {
            return new MySQLNoLessThan5700SchemaAccessor(this.jdbcOperations);
        } else if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "5.6.0")) {
            return new MySQLNoLessThan5600SchemaAccessor(this.jdbcOperations);
        } else {
            throw new UnsupportedOperationException(String.format("MySQL version '%s' not supported", this.dbVersion));
        }
    }

    @Override
    public DBSchemaAccessor buildForOBMySQL() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.0.0")) {
            // OB version >= 4.0.0
            return new OBMySQLSchemaAccessor(this.jdbcOperations);
        } else if (VersionUtils.isGreaterThan(this.dbVersion, "2.2.76")) {
            // OB version between [2.2.77, 4.0.0)
            return new OBMySQLBetween2277And3XSchemaAccessor(this.jdbcOperations);
        } else if (VersionUtils.isGreaterThan(this.dbVersion, "2.2.60")) {
            // OB version between [2.2.60, 2.2.77)
            return new OBMySQLBetween2260And2276SchemaAccessor(this.jdbcOperations);
        } else if (VersionUtils.isGreaterThan(this.dbVersion, "1.4.79")) {
            // OB version between (1.4.79, 2.2.60)
            return new OBMySQLBetween220And225XSchemaAccessor(this.jdbcOperations);
        } else {
            // OB version <= 1.4.79
            String tenantName = null;
            JdbcOperations sysJdbcOperations = null;
            if (this.properties != null) {
                Object value = this.properties.get(TENANT_NAME_KEY);
                if (value instanceof String) {
                    tenantName = (String) value;
                }
                value = this.properties.get(SYS_OPERATIONS_KEY);
                if (value instanceof JdbcOperations) {
                    sysJdbcOperations = (JdbcOperations) value;
                }
            }
            return new OBMySQLNoGreaterThan1479SchemaAccessor(this.jdbcOperations, sysJdbcOperations, tenantName);
        }
    }

    @Override
    public DBSchemaAccessor buildForOBOracle() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.1.0")) {
            // OB version >= 4.1.0
            return new OBOracleSchemaAccessor(this.jdbcOperations, new ALLDataDictTableNames());
        } else if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.0.0")) {
            // OB version between [4.0.0, 4.1.0)
            return new OBOracleBetween4000And4100SchemaAccessor(this.jdbcOperations, new ALLDataDictTableNames());
        } else if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "2.2.70")) {
            // OB version between [2.2.7, 4.0.0)
            return new OBOracleLessThan400SchemaAccessor(this.jdbcOperations, new ALLDataDictTableNames());
        } else {
            // OB version < 2.2.70
            return new OBOracleLessThan2270SchemaAccessor(this.jdbcOperations, new ALLDataDictTableNames());
        }
    }

    @Override
    public DBSchemaAccessor buildForOracle() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        return new OracleSchemaAccessor(this.jdbcOperations, new ALLDataDictTableNames());
    }

    @Override
    public DBSchemaAccessor buildForOdpSharding() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        return new ODPOBMySQLSchemaAccessor(this.jdbcOperations);
    }

    @Override
    public DBSchemaAccessor buildForDoris() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "5.7.0")) {
            return new DorisSchemaAccessor(this.jdbcOperations);
        } else {
            throw new UnsupportedOperationException(String.format("Doris version '%s' not supported", this.dbVersion));
        }
    }

}
