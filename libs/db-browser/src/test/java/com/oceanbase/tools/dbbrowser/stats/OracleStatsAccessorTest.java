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
import com.oceanbase.tools.dbbrowser.model.DBTableStats;
import com.oceanbase.tools.dbbrowser.stats.oracle.OracleStatsAccessor;

/**
 * @author jingtian
 * @date 2023/11/24
 * @since ODC_release_4.2.4
 */
public class OracleStatsAccessorTest extends BaseTestEnv {
    private final DBStatsAccessor accessor = new OracleStatsAccessor(new JdbcTemplate(getOracleDataSource()));
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(getOracleDataSource());

    @Test
    public void listAllSessions() {
        List<DBSession> session = accessor.listAllSessions();
        Assert.assertTrue(session.size() > 0);
    }

    @Test
    public void currentSession() {
        DBSession session = accessor.currentSession();
        Assert.assertNotNull(session);
        Assert.assertNotNull(session.getId());
    }

    @Test
    public void getTableStats() {
        jdbcTemplate.execute("alter session set current_schema =  \"" + getOracleSchema() + "\"");
        jdbcTemplate.execute("create table TABLE_STATS_TEST (ID NUMBER, NAME VARCHAR(20))");
        jdbcTemplate.execute("insert into TABLE_STATS_TEST values (1, 'test1')");
        DBTableStats tableStats = accessor.getTableStats(getOracleSchema(), "TABLE_STATS_TEST");
        Assert.assertNotNull(tableStats);
        jdbcTemplate.execute("drop table TABLE_STATS_TEST");
    }
}
