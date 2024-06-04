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

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

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
    private DataSource dataSource;
    private JdbcOperations jdbcOperations;
    private Map<String, Object> properties;

    @Override
    public DBStatsAccessor buildForDoris() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "5.7.0")) {
            return new DorisStatsAccessor(getJdbcOperations());
        } else {
            throw new UnsupportedOperationException(String.format("Doris version '%s' not supported", this.dbVersion));
        }
    }

    @Override
    public DBStatsAccessor buildForMySQL() {
        return new MySQLNoLessThan5700StatsAccessor(getJdbcOperations());
    }

    @Override
    public DBStatsAccessor buildForOBMySQL() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.0.0")) {
            // OB 版本 >= 4.0.0
            return new OBMySQLNoLessThan400StatsAccessor(getJdbcOperations());
        } else {
            return new OBMySQLStatsAccessor(getJdbcOperations());
        }
    }

    @Override
    public DBStatsAccessor buildForOBOracle() {
        Validate.notNull(this.dbVersion, "DBVersion can not be null");
        if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "4.0.0")) {
            // OB version >= 4.0.0
            return new OBOracleNoLessThan400StatsAccessor(getJdbcOperations());
        } else if (VersionUtils.isGreaterThanOrEqualsTo(this.dbVersion, "2.2.70")) {
            // OB version between [2.2.70, 4.0.0)
            return new OBOracleNoLessThan2270StatsAccessor(getJdbcOperations());
        } else {
            // OB version < 2.2.70
            return new OBOracleLessThan2270StatsAccessor(getJdbcOperations());
        }
    }

    @Override
    public DBStatsAccessor buildForOracle() {
        return new OracleStatsAccessor(getJdbcOperations());
    }

    @Override
    public DBStatsAccessor buildForOdpSharding() {
        String connectionId = null;
        if (this.properties != null) {
            Object value = this.properties.get(CONNECTION_ID_KEY);
            if (value instanceof String) {
                connectionId = (String) value;
            }
        }
        return new ODPOBMySQLStatsAccessor(connectionId);
    }

    private JdbcOperations getJdbcOperations() {
        if (this.jdbcOperations != null) {
            return this.jdbcOperations;
        } else if (this.dataSource != null) {
            return new JdbcTemplate(this.dataSource);
        }
        throw new IllegalArgumentException("Datasource can not be null");
    }

}
