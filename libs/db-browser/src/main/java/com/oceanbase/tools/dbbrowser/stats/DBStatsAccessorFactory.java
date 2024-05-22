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

import org.springframework.jdbc.core.JdbcOperations;

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

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/5/22
 * @since ODC_release_4.3.0
 */
public class DBStatsAccessorFactory {

    public static DBStatsAccessor createForOBOracle(@NonNull JdbcOperations jdbcOperations, @NonNull String dbVersion) {
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "4.0.0")) {
            // OB version >= 4.0.0
            return new OBOracleNoLessThan400StatsAccessor(jdbcOperations);
        } else if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "2.2.70")) {
            // OB version between [2.2.70, 4.0.0)
            return new OBOracleNoLessThan2270StatsAccessor(jdbcOperations);
        } else {
            // OB version < 2.2.70
            return new OBOracleLessThan2270StatsAccessor(jdbcOperations);
        }
    }

    public static DBStatsAccessor createForOBMySQL(@NonNull JdbcOperations jdbcOperations, @NonNull String dbVersion) {
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "4.0.0")) {
            // OB 版本 >= 4.0.0
            return new OBMySQLNoLessThan400StatsAccessor(jdbcOperations);
        } else {
            return new OBMySQLStatsAccessor(jdbcOperations);
        }
    }

    public static DBStatsAccessor createForMySQL(@NonNull JdbcOperations jdbcOperations, String dbVersion) {
        return new MySQLNoLessThan5700StatsAccessor(jdbcOperations);
    }

    public static DBStatsAccessor createForDoris(@NonNull JdbcOperations jdbcOperations, String dbVersion) {
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "5.7.0")) {
            return new DorisStatsAccessor(jdbcOperations);
        } else {
            throw new UnsupportedOperationException(String.format("Doris version '%s' not supported", dbVersion));
        }
    }

    public static DBStatsAccessor createForODPOBMySQL(String connectionId) {
        return new ODPOBMySQLStatsAccessor(connectionId);
    }

    public static DBStatsAccessor createForOracle(@NonNull JdbcOperations jdbcOperations, String dbVersion) {
        return new OracleStatsAccessor(jdbcOperations);
    }
}
