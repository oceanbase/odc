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
import com.oceanbase.tools.dbbrowser.stats.oracle.OBOracleNoLessThan2270StatsAccessor;

/**
 * {@link OBOracleNoLessThan2270StatsAccessorTest}
 *
 * @author yh263208
 * @date 2023-02-27 21:24
 * @since db-browser_1.0.0-SNAPSHOT
 */
public class OBOracleNoLessThan2270StatsAccessorTest extends BaseTestEnv {

    @Test
    public void currentSession_versionLessThan2230_getSessionSucceed() {
        DBStatsAccessor accessor = new OBOracleNoLessThan2270StatsAccessor(new JdbcTemplate(getOBOracleDataSource()));
        DBSession acutal = accessor.currentSession();
        DBSession expect = new DBSession();
        expect.setId(acutal.getId());
        expect.setTransState(DBTransState.UNKNOWN);
        Assert.assertEquals(expect, acutal);
    }

    @Test
    public void listAllSessions_Succeed() {
        DBStatsAccessor accessor = new OBOracleNoLessThan2270StatsAccessor(new JdbcTemplate(getOBOracleDataSource()));
        List<DBSession> sessions = accessor.listAllSessions();
        Assert.assertTrue(sessions.size() > 0);
        sessions.stream().forEach(s -> Assert.assertNotNull(s.getSvrIp()));
    }
}
