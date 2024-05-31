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
package com.oceanbase.tools.dbbrowser.stats;

import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.AbstractDBBrowserFactory;
import com.oceanbase.tools.dbbrowser.stats.mysql.DorisStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.MySQLNoLessThan5700StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.OBMySQLNoLessThan400StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.OBMySQLStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.ODPOBMySQLStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.oracle.OBOracleLessThan2270StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.oracle.OBOracleNoLessThan2270StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.oracle.OBOracleNoLessThan400StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.oracle.OracleStatsAccessor;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DBStatsAccessorFactory extends AbstractDBBrowserFactory<DBStatsAccessor> {

    public static final String CONNECTION_ID_KEY = "connectionId";
    private String dbVersion;
    private JdbcOperations jdbcOperations;
    private Map<String, Object> properties;

    @Override
    public DBStatsAccessor buildForDoris() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "5.7.0")) {
            return new DorisStatsAccessor(this.jdbcOperations);
        } else {
            throw new UnsupportedOperationException(String.format("Doris version '%s' not supported", this.dbVersion));
        }
    }

    @Override
    public DBStatsAccessor buildForMySQL() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        return new MySQLNoLessThan5700StatsAccessor(this.jdbcOperations);
    }

    @Override
    public DBStatsAccessor buildForOBMySQL() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.0.0")) {
            // OB 版本 >= 4.0.0
            return new OBMySQLNoLessThan400StatsAccessor(this.jdbcOperations);
        } else {
            return new OBMySQLStatsAccessor(this.jdbcOperations);
        }
    }

    @Override
    public DBStatsAccessor buildForOBOracle() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.0.0")) {
            // OB version >= 4.0.0
            return new OBOracleNoLessThan400StatsAccessor(this.jdbcOperations);
        } else if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "2.2.70")) {
            // OB version between [2.2.70, 4.0.0)
            return new OBOracleNoLessThan2270StatsAccessor(this.jdbcOperations);
        } else {
            // OB version < 2.2.70
            return new OBOracleLessThan2270StatsAccessor(this.jdbcOperations);
        }
    }

    @Override
    public DBStatsAccessor buildForOracle() {
        Validate.notNull(this.jdbcOperations, "Datasource can not be null");
        return new OracleStatsAccessor(this.jdbcOperations);
    }

    @Override
    public DBStatsAccessor buildForOdpSharding() {
        Validate.notNull(this.properties, "Properties can not be null");
        Object connectionId = this.properties.get(CONNECTION_ID_KEY);
        Validate.isTrue(connectionId instanceof String, "ConnectionId can not be null");
        return new ODPOBMySQLStatsAccessor((String) connectionId);
    }

}
