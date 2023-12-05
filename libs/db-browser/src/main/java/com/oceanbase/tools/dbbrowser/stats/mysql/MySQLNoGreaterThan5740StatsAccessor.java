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
package com.oceanbase.tools.dbbrowser.stats.mysql;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.model.DBTableStats;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link MySQLNoGreaterThan5740StatsAccessor}
 *
 * 适配 MySQL 版本：(~, 5.7.40]
 */
public class MySQLNoGreaterThan5740StatsAccessor implements DBStatsAccessor {
    private static final String QUERY_SESSION_FROM_PROCESSLIST = "SELECT `ID`, "
            + "`USER` as USERNAME, "
            + "`DB` as DATABASE_NAME, "
            + "`HOST`, "
            // 若通过 obproxy 连接则 HOST 为 obproxy 的 HOST 地址
            + "`HOST` as PROXY_HOST, "
            + "`TIME` as EXECUTE_TIME, "
            + "COMMAND, "
            + "STATE, "
            + "'UNKNOWN' AS `TRANS_STATE`, "
            + "null AS `TRANS_ID`, "
            + "null AS  `SQL_ID`, "
            + "null AS  `TRACE_ID`, "
            + "LEFT(`INFO`, 200) AS LATEST_QUERIES "
            + "FROM "
            + "  information_schema.PROCESSLIST ";
    private static final String QUERY_CURRENT_SESSION_FROM_PROCESSLIST =
            QUERY_SESSION_FROM_PROCESSLIST + " WHERE `ID` = connection_id()";

    final JdbcOperations jdbcOperations;

    public MySQLNoGreaterThan5740StatsAccessor(@NonNull JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public DBTableStats getTableStats(@NonNull String schema, @NonNull String tableName) {
        String sql = sqlBuilder().append("select TABLE_ROWS, DATA_LENGTH from `information_schema`.`tables`")
                .append(" where table_schema=").value(schema)
                .append(" and table_name=").value(tableName).toString();
        DBTableStats stats = new DBTableStats();
        jdbcOperations.query(sql, t -> {
            stats.setRowCount(t.getLong("TABLE_ROWS"));
            stats.setDataSizeInBytes(t.getLong("DATA_LENGTH"));
        });
        return stats;
    }

    @Override
    public List<DBSession> listAllSessions() {
        return jdbcOperations.query(QUERY_SESSION_FROM_PROCESSLIST,
                new BeanPropertyRowMapper<>(DBSession.class));
    }

    @Override
    public DBSession currentSession() {
        List<DBSession> sessions = jdbcOperations.query(QUERY_CURRENT_SESSION_FROM_PROCESSLIST,
                new BeanPropertyRowMapper<>(DBSession.class));
        return CollectionUtils.isEmpty(sessions) ? DBSession.unknown() : sessions.get(0);
    }

    protected SqlBuilder sqlBuilder() {
        return new MySQLSqlBuilder();
    }
}
