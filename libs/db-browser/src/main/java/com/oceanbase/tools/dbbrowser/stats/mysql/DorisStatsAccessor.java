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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.model.DBSession.DBTransState;

import lombok.NonNull;

/**
 * ClassName: DorisStatsAccessor Package: com.oceanbase.tools.dbbrowser.stats.mysql Description:
 *
 * @Author: fenghao
 * @Create 2024/2/5 16:23
 * @Version 1.0
 */
public class DorisStatsAccessor extends MySQLNoLessThan5700StatsAccessor {

    private static final String QUERY_SESSION_FROM_PROCESSLIST = "show full processlist";

    public DorisStatsAccessor(@NonNull JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public List<DBSession> listAllSessions() {
        return this.jdbcOperations.query(QUERY_SESSION_FROM_PROCESSLIST, new DorisDBSessionMapper());
    }

    @Override
    public DBSession currentSession() {
        String connectionId = this.jdbcOperations.queryForObject(
                "select connection_id()", (rs, rowNum) -> rs.getString(1));
        List<DBSession> sessions = listAllSessions().stream()
                .filter(s -> Objects.equals(connectionId, s.getId())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sessions)) {
            return DBSession.unknown();
        } else if (sessions.size() != 1) {
            throw new IllegalStateException("Failed to locate the session by id, " + connectionId);
        }
        return sessions.get(0);
    }

    private static class DorisDBSessionMapper implements RowMapper<DBSession> {

        @Override
        public DBSession mapRow(ResultSet rs, int rowNum) throws SQLException {
            DBSession session = new DBSession();
            session.setId(rs.getString("Id"));
            session.setUsername(rs.getString("User"));
            session.setHost(rs.getString("Host"));
            session.setDatabaseName(rs.getString("Db"));
            session.setCommand(rs.getString("Command"));
            session.setExecuteTime(rs.getInt("Time"));
            session.setState(rs.getString("State"));
            session.setTransState(DBTransState.UNKNOWN);
            session.setProxyHost(session.getHost());
            return session;
        }
    }

}
