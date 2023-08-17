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
package com.oceanbase.odc.core.sql.util;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.oceanbase.odc.core.shared.model.OdcDBSession;

public class OdcDBSessionRowMapper implements RowMapper<OdcDBSession> {

    @Override
    public OdcDBSession mapRow(ResultSet rs, int rowNum) throws SQLException {
        OdcDBSession sess = new OdcDBSession();
        sess.setSessionId(Long.parseLong(rs.getString("ID")));
        sess.setDbUser(rs.getString("USER"));
        sess.setSrcIp(rs.getString("HOST"));
        sess.setDatabase(rs.getString("DB"));
        sess.setCommand(rs.getString("COMMAND"));
        sess.setExecuteTime(Integer.parseInt(rs.getString("TIME")));
        sess.setStatus(rs.getString("STATE"));
        sess.setSql(rs.getString("INFO"));
        sess.setObproxyIp(rs.getString("HOST"));
        if (rs.getMetaData().getColumnCount() == 11) {
            sess.setSvrIp(rs.getString(10) + ":" + rs.getString(11));
        }
        return sess;
    }

}
