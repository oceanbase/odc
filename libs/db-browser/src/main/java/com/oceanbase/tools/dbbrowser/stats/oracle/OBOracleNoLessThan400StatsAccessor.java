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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;

import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.model.DBSession.DBTransState;
import com.oceanbase.tools.dbbrowser.util.HostUtils;

/**
 * {@link OBOracleNoLessThan400StatsAccessor}
 * 
 * @author yh263208
 * @date 2023-02-27 20:49
 * @since db-browser_1.0.0_SNAPSHOT
 */
public class OBOracleNoLessThan400StatsAccessor extends BaseOBOracleStatsAccessor {

    private static final String OB_PROCESSLIST_SESSION_COLUMNS = " ID, "
            + "  USER as USERNAME, "
            + "  DB as DATABASE_NAME, "
            + "  COMMAND, "
            + "  STATE, "
            + "  USER_CLIENT_IP as HOST, "
            + "  HOST as PROXY_HOST, "
            + "  SVR_IP, "
            + "  SQL_PORT, "
            + "  TIME as EXECUTE_TIME, "
            + "  CASE "
            + "    WHEN TRANS_STATE IS NULL OR TRANS_STATE IN ('', 'IDLE', 'IN_TERMINATE', 'ABORTED', "
            + "     'ROLLED_BACK', 'COMMITED') THEN 'IDLE' "
            + "    WHEN TRANS_STATE IN ('ACTIVE', 'IMPLICIT_ACTIVE', 'ROLLBACK_SAVEPOINT') THEN 'ACTIVE' "
            + "    WHEN TRANS_STATE IN ('COMMIT_TIMEOUT', 'COMMIT_UNKNOWN') THEN 'TIMEOUT' "
            + "    ELSE 'UNKNOWN' "
            + "  END AS TRANS_STATE, "
            + "  TRANS_ID, "
            + "  SQL_ID, "
            + "  TRACE_ID, "
            + "  SUBSTR(INFO, 1, 200) AS LATEST_QUERIES ";
    private static final String OB40_QUERY_ALL_SESSIONS =
            "SELECT " + OB_PROCESSLIST_SESSION_COLUMNS + " FROM GV$OB_PROCESSLIST";
    private static final String OB40_QUERY_CURRENT_SESSION =
            "SELECT " + OB_PROCESSLIST_SESSION_COLUMNS + " FROM V$OB_PROCESSLIST WHERE ID=userenv('sessionid')";

    public OBOracleNoLessThan400StatsAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public List<DBSession> listAllSessions() {
        return jdbcOperations.query(OB40_QUERY_ALL_SESSIONS, new DBSessionRowMapper());
    }

    @Override
    public DBSession currentSession() {
        return jdbcOperations.queryForObject(OB40_QUERY_CURRENT_SESSION, new DBSessionRowMapper());
    }


    private static class DBSessionRowMapper implements RowMapper<DBSession> {
        @Override
        public DBSession mapRow(ResultSet rs, int rowNum) throws SQLException {
            DBSession session = new DBSession();
            session.setId(rs.getString("ID"));
            session.setUsername(rs.getString("USERNAME"));
            session.setDatabaseName(rs.getString("DATABASE_NAME"));
            session.setCommand(rs.getString("COMMAND"));
            session.setState(rs.getString("STATE"));
            session.setHost(rs.getString("HOST"));
            session.setProxyHost(rs.getString("PROXY_HOST"));

            // 处理SVR_IP，使用HostUtils添加IPv6方括号
            String svrIp = rs.getString("SVR_IP");
            String sqlPort = rs.getString("SQL_PORT");
            if (svrIp != null && sqlPort != null) {
                String processedIp = HostUtils.addBracketsToIpv6AddressIfNeed(svrIp);
                session.setSvrIp(processedIp + ":" + sqlPort);
            }

            session.setExecuteTime(rs.getInt("EXECUTE_TIME"));
            session.setTransState(DBTransState.valueOf(rs.getString("TRANS_STATE")));
            session.setTransId(rs.getString("TRANS_ID"));
            session.setSqlId(rs.getString("SQL_ID"));
            session.setTraceId(rs.getString("TRACE_ID"));
            session.setLatestQueries(rs.getString("LATEST_QUERIES"));

            return session;
        }
    }

}
