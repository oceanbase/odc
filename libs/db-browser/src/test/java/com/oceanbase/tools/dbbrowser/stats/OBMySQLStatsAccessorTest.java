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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.tools.dbbrowser.env.BaseTestEnv;
import com.oceanbase.tools.dbbrowser.model.DBSession;
import com.oceanbase.tools.dbbrowser.model.DBSession.DBTransState;
import com.oceanbase.tools.dbbrowser.stats.mysql.OBMySQLStatsAccessor;

/**
 * {@link OBMySQLStatsAccessorTest}
 *
 * @author yh263208
 * @date 2023-02-27 21:14
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OBMySQLStatsAccessorTest extends BaseTestEnv {

    @Test
    public void currentSession() {
        DBStatsAccessor accessor = new OBMySQLStatsAccessor(new JdbcTemplate(getOBMySQLDataSource()));
        DBSession session = accessor.currentSession();
        Assert.assertEquals(DBTransState.UNKNOWN, session.getTransState());
    }

    @Test
    public void listAllSessions() {
        DBStatsAccessor accessor = new OBMySQLStatsAccessor(new JdbcTemplate(getOBMySQLDataSource()));
        List<DBSession> sessions = accessor.listAllSessions();
        Assert.assertTrue(sessions.size() > 0);
        sessions.stream().forEach(s -> Assert.assertNotNull(s.getSvrIp()));
    }

}
