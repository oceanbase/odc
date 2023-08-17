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

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.model.DBSession.DBTransState;

/**
 * {@link OBOracleLessThan2270StatsAccessor}
 * 
 * @author yh263208
 * @date 2023-02-27 21:06
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OBOracleLessThan2270StatsAccessor extends BaseOBOracleStatsAccessor {

    public OBOracleLessThan2270StatsAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public DBSession currentSession() {
        String sessionId = jdbcOperations.execute((StatementCallback<String>) s -> {
            OceanBaseConnection conn = (OceanBaseConnection) s.getConnection();
            return conn.getServerThreadId() + "";
        });
        // for oracle mode, there is not processlist like view before ob4.0
        DBSession session = new DBSession();
        session.setId(sessionId);
        session.setTransState(DBTransState.UNKNOWN);
        return session;
    }

}
