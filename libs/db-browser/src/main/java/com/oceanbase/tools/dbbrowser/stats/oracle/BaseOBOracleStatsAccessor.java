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
package com.oceanbase.tools.dbbrowser.stats.oracle;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBMySQLProcess;
import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.model.DBTableStats;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.util.ALLDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.USERDataDictTableNames;

import lombok.NonNull;

/**
 * {@link BaseOBOracleStatsAccessor}
 * 
 * @author yh263208
 * @date 2022-02-27 20:39
 * @since db-browser_1.0.0-SNAPSHOT
 */
public abstract class BaseOBOracleStatsAccessor implements DBStatsAccessor {

    private static final String QUERY_SESSION_FROM_PROCESSLIST = "SHOW FULL PROCESSLIST";
    final OracleDataDictTableNames dataDictTableNames;
    final JdbcOperations jdbcOperations;

    public BaseOBOracleStatsAccessor(JdbcOperations jdbcOperations) {
        // TODO: dataDictTableNames depends on username
        this.dataDictTableNames = new ALLDataDictTableNames();
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public DBTableStats getTableStats(@NonNull String schema, @NonNull String tableName) {
        DBTableStats stats = new DBTableStats();
        stats.setDataSizeInBytes(getTableSize(tableName));
        stats.setRowCount(getRowCount(schema, tableName));
        return stats;
    }

    @Override
    public List<DBSession> listAllSessions() {
        return jdbcOperations.query(QUERY_SESSION_FROM_PROCESSLIST, new BeanPropertyRowMapper<>(DBMySQLProcess.class))
                .stream().map(DBMySQLProcess::toDBSession).collect(Collectors.toList());
    }

    private Long getTableSize(String tableName) {
        OracleDataDictTableNames tableNames = new USERDataDictTableNames();
        String sql = sqlBuilder().append("SELECT BYTES FROM ")
                .append(tableNames.SEGMENTS())
                .append(" WHERE SEGMENT_NAME = ").value(tableName).toString();
        List<Long> segSizes = jdbcOperations.query(sql, (rs, rowNum) -> rs.getLong("BYTES"));
        return segSizes.stream().mapToLong(value -> value).sum();
    }

    private Long getRowCount(String schemaName, String tableName) {
        String sql = sqlBuilder().append("SELECT NUM_ROWS FROM ")
                .append(dataDictTableNames.TABLES())
                .append(" WHERE TABLE_NAME = ").value(tableName)
                .append(" AND OWNER = ").value(schemaName).toString();
        return jdbcOperations.query(sql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getLong("NUM_ROWS");
        });
    }

    protected SqlBuilder sqlBuilder() {
        return new OracleSqlBuilder();
    }

}
