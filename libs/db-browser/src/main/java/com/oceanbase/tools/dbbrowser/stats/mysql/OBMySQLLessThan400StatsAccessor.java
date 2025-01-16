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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2025/1/16 11:23
 * @Description: []
 */
public class OBMySQLLessThan400StatsAccessor extends MySQLNoLessThan5700StatsAccessor {
    private static final String LIST_SESSIONS_BY_SHOW_PROCESSLIST = "SHOW FULL PROCESSLIST";

    public OBMySQLLessThan400StatsAccessor(@NonNull JdbcOperations jdbcOperations) {
        super(jdbcOperations);
    }

    @Override
    public List<DBSession> listAllSessions() {
        List<DBSession> sessions = super.listAllSessions();
        Map<String, String> sessionId2SvrIp = new HashMap<>();
        jdbcOperations.query(LIST_SESSIONS_BY_SHOW_PROCESSLIST, rs -> {
            if (rs.getMetaData().getColumnCount() == 11) {
                String id = rs.getString("Id");
                String svrIp = StringUtils.join(rs.getString("Ip"), ":", rs.getString("Port"));
                sessionId2SvrIp.put(id, svrIp);
            }
        });
        sessions.forEach(session -> session.setSvrIp(sessionId2SvrIp.get(session.getId())));
        return sessions;
    }
}
