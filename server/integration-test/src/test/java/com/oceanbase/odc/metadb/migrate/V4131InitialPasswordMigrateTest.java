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
import com.oceanbase.odc.migrate.jdbc.web.V4131InitialPasswordMigrate;

public class V4131InitialPasswordMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.update("delete from connect_connection where VISIBLE_SCOPE='PRIVATE' AND owner_id=1");
        MigrateTestUtils.clearUserAndOrganization(jdbcTemplate);
        MigrateTestUtils.initUserAndOrganization(jdbcTemplate, "DEFAULT", "admin");
        jdbcTemplate.update("update iam_user set is_active=0 where account_name='admin'");
    }

    @Test(expected = IllegalArgumentException.class)
    public void migrate_WeakPassword_IllegalArgumentException() {
        System.setProperty("ODC_ADMIN_INITIAL_PASSWORD", "weak");
        V4131InitialPasswordMigrate migrate = new V4131InitialPasswordMigrate();
        migrate.migrate(dataSource);
    }

    @Test
    public void migrate_StrongPassword_Success() {
        System.setProperty("ODC_ADMIN_INITIAL_PASSWORD", "STrong00.@");
        V4131InitialPasswordMigrate migrate = new V4131InitialPasswordMigrate();
        migrate.migrate(dataSource);

        int count = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
                "iam_user", "is_active=1 AND account_name='admin'");
        Assert.assertEquals(1, count);
    }
}
