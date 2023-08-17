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
package com.oceanbase.odc.metadb.migrate;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.migrate.jdbc.common.V2422ConnectEmptyPasswordMigrate;

public class V2422ConnectSysPasswordMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "odc_user_info");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "odc_session_manager");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "odc_session_extended");
    }

    @Test
    public void migrate() {
        // prepare, sys user password use base64 in previous version
        jdbcTemplate.update("INSERT INTO `odc_user_info` (`id`, `name`, `email`, `password`, `cipher`) "
                + "VALUES (1, 'test1', 'test1', '123456', 'BCRYPT')");
        jdbcTemplate.update("INSERT INTO `odc_session_manager` ("
                + "`id`, `user_id`, `session_name`, `db_mode`, `host`, `port`, `cluster`, "
                + "`tenant`, `db_user`, `password`, `default_DBName`, `extend_info`, `salt`, `cipher`) "
                + " VALUES (1, '1', 'test1', 'OB_MYSQL', '127.1', '2881', '', 'mysql', 'root', '', 'mysql', '{}',"
                + " 'fakesaltvalues123', 'AES256SALT')");
        jdbcTemplate.update("INSERT INTO `odc_session_extended` ("
                + " `sid`, `session_timeout`, `sys_user`, `sys_user_password`) "
                + " VALUES ('1', '60', 'root', '')");

        // execute
        V2422ConnectEmptyPasswordMigrate migrate = new V2422ConnectEmptyPasswordMigrate();
        migrate.migrate(dataSource);

        // verify password
        int count1 =
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "odc_session_manager", "`password` <> ''");
        Assert.assertEquals(1, count1);

        // verify sys password
        int count2 =
                JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "odc_session_extended", "`sys_user_password` <> ''");
        Assert.assertEquals(1, count2);
    }
}
