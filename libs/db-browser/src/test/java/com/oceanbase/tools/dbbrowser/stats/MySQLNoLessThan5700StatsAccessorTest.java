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
import com.oceanbase.tools.dbbrowser.stats.mysql.MySQLNoLessThan5700StatsAccessor;

/**
 * @author jingtian
 * @date 2023/6/7
 */
public class MySQLNoLessThan5700StatsAccessorTest extends BaseTestEnv {

    @Test
    public void listAllSessions() {
        DBStatsAccessor accessor = new MySQLNoLessThan5700StatsAccessor(new JdbcTemplate(getMySQLDataSource()));
        List<DBSession> session = accessor.listAllSessions();
        Assert.assertTrue(session.size() > 0);
    }

    @Test
    public void currentSession() {
        DBStatsAccessor accessor = new MySQLNoLessThan5700StatsAccessor(new JdbcTemplate(getMySQLDataSource()));
        DBSession session = accessor.currentSession();
        Assert.assertEquals(DBTransState.UNKNOWN, session.getTransState());
    }
}
