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

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBSession;

/**
 * @author jingtian
 * @date 2023/11/17
 * @since
 */
public class OracleStatsAccessor extends BaseOBOracleStatsAccessor {

    private static final String LIST_ALL_SESSIONS = "SELECT \n"
            + "a.SID || ', ' || a.SERIAL# AS ID,\n"
            + "a.USERNAME, \n"
            + "a.SCHEMANAME as DATABASE_NAME, \n"
            + "a.COMMAND, \n"
            + "a.STATUS as STATE, \n"
            + "a.MACHINE as HOST, \n"
            + "a.PORT, \n"
            + "a.SQL_ID, \n"
            + "b.SQL_TEXT as LATEST_QUERIES,\n"
            + "b.ELAPSED_TIME as EXECUTE_TIME\n"
            + "FROM V$SESSION a left join V$SQL b on a.SQL_ID = b.SQL_ID";

    private static final String GET_CURRENT_SESSION = LIST_ALL_SESSIONS
            + "  WHERE a.SID = SYS_CONTEXT('USERENV', 'SID') and a.AUDSID=SYS_CONTEXT('USERENV', 'SESSIONID') and a.status = 'ACTIVE'";

    public OracleStatsAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public DBSession currentSession() {
        return jdbcOperations.queryForObject(GET_CURRENT_SESSION,
                new BeanPropertyRowMapper<>(DBSession.class));
    }

    @Override
    public List<DBSession> listAllSessions() {
        return jdbcOperations.query(LIST_ALL_SESSIONS, new BeanPropertyRowMapper<>(DBSession.class));
    }
}
