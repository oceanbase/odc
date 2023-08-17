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
import com.oceanbase.odc.migrate.jdbc.common.V2412UserPasswordMigrate;

public class V2412UserPasswordMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "odc_user_info");
    }

    @Test
    public void migrate() {
        // prepare
        jdbcTemplate.update("INSERT INTO `odc_user_info` (`id`, `name`, `email`, `password`, `cipher`) "
                + "VALUES (1, 'test1', 'test1', '123456', 'RAW')");

        // execute
        V2412UserPasswordMigrate migrate = new V2412UserPasswordMigrate();
        migrate.migrate(dataSource);

        // verify
        int count = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "odc_user_info", "`password` <> '123456'");
        Assert.assertEquals(1, count);
    }
}
