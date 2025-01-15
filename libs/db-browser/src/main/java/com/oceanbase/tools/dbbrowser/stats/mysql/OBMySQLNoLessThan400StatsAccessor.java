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

import lombok.NonNull;

/**
 * {@link OBMySQLNoLessThan400StatsAccessor}
 * 
 * @author yh263208
 * @date 2023-02-27 20:32
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OBMySQLNoLessThan400StatsAccessor extends OBMySQLStatsAccessor {

    private static final String OB40_SESSION_COLUMNS = " `ID`, "
            + "  `USER` as USERNAME, "
            + "  `DB` as DATABASE_NAME, "
            + "  COMMAND, "
            + "  STATE, "
            + "  `USER_CLIENT_IP` as HOST, "
            + "  HOST as PROXY_HOST, "
            + "  CONCAT(SVR_IP, ':', SQL_PORT) AS SVR_IP, "
            + "  TIME as EXECUTE_TIME, "
            + "  CASE "
            + "    WHEN `TRANS_STATE` IS NULL OR `TRANS_STATE` IN ('', 'IDLE', 'IN_TERMINATE', 'ABORTED', "
            + "     'ROLLED_BACK', 'COMMITED') THEN 'IDLE' "
            + "    WHEN `TRANS_STATE` IN ('ACTIVE', 'IMPLICIT_ACTIVE', 'ROLLBACK_SAVEPOINT') THEN 'ACTIVE' "
            + "    WHEN `TRANS_STATE` IN ('COMMIT_TIMEOUT', 'COMMIT_UNKNOWN') THEN 'TIMEOUT' "
            + "    ELSE 'UNKNOWN' "
            + "  END AS TRANS_STATE, "
            + "  TRANS_ID, "
            + "  `SQL_ID`, "
            + "  `TRACE_ID`, "
            + "  LEFT(`INFO`, 200) AS LATEST_QUERIES ";
    private static final String OB40_QUERY_ALL_SESSIONS =
            "SELECT " + OB40_SESSION_COLUMNS + " FROM oceanbase.GV$OB_PROCESSLIST";
    private static final String OB40_QUERY_CURRENT_SESSION =
            "SELECT " + OB40_SESSION_COLUMNS + " FROM oceanbase.V$OB_PROCESSLIST WHERE `ID`=connection_id()";

    public OBMySQLNoLessThan400StatsAccessor(@NonNull JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public List<DBSession> listAllSessions() {
        return jdbcOperations.query(OB40_QUERY_ALL_SESSIONS, new BeanPropertyRowMapper<>(DBSession.class));
    }

    @Override
    public DBSession currentSession() {
        List<DBSession> sessions = jdbcOperations.query(OB40_QUERY_CURRENT_SESSION,
                new BeanPropertyRowMapper<>(DBSession.class));
        return CollectionUtils.isEmpty(sessions) ? DBSession.unknown() : sessions.get(0);
    }

}
