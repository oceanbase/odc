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
package com.oceanbase.odc.migrate.jdbc.common;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;

public class V3309SqlScriptMigrateTest extends ServiceTestEnv {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        cleanTables();
    }

    @After
    public void tearDown() {
        cleanTables();
    }

    @Test
    public void migrate() {
        jdbcTemplate.update(
                "INSERT INTO `odc_sql_script`(`id`, `user_id`, `script_name`, `script_text_old`, `script_type`, "
                        + "`gmt_create`, `gmt_modify`, `script_text`) VALUES("
                        + "1, 1, 'fake_script.sql', 'whatever_content', 'sql', '1993-01-31 01:54:59', '1993-01-31 01:54:59', 'whatever_content');");

        jdbcTemplate.update(
                "INSERT INTO `odc_sql_script`(`id`, `user_id`, `script_name`, `script_text_old`, `script_type`, "
                        + "`gmt_create`, `gmt_modify`, `script_text`) VALUES("
                        + "2, 1, 'fake_script_without_extension', '', '', null, null, "
                        + "'whatever_content');");

        jdbcTemplate.update(
                "INSERT INTO `odc_sql_script`(`id`, `user_id`, `script_name`, `script_text_old`, `script_type`, "
                        + "`gmt_create`, `gmt_modify`, `script_text`) VALUES("
                        + "3, 2, '', 'whatever_content', 'sql', null, null, "
                        + "'whatever_content');");

        V3309SqlScriptMigrate v3309SqlScriptMigrate = new V3309SqlScriptMigrate();
        v3309SqlScriptMigrate.migrate(dataSource);
        // verify odc_sql_script
        int count1 =
                JdbcTestUtils.countRowsInTable(jdbcTemplate, "odc_sql_script");
        Assert.assertEquals(3, count1);

        // verify script_meta
        int count2 =
                JdbcTestUtils.countRowsInTable(jdbcTemplate, "script_meta");
        Assert.assertEquals(3, count2);

        // verify objectstorage_object_metadata
        int count3 =
                JdbcTestUtils.countRowsInTable(jdbcTemplate, "objectstorage_object_metadata");
        Assert.assertEquals(3, count3);


        // 再插入一条后，重跑，应该只迁移一条记录
        jdbcTemplate.update(
                "INSERT INTO `odc_sql_script`(`id`, `user_id`, `script_name`, `script_text_old`, `script_type`, "
                        + "`gmt_create`, `gmt_modify`, `script_text`) VALUES("
                        + "4, 2, '', 'whatever_content', 'sql', null, null, "
                        + "'whatever_content');");
        v3309SqlScriptMigrate.migrate(dataSource);

        // verify script_meta
        int count4 =
                JdbcTestUtils.countRowsInTable(jdbcTemplate, "script_meta");
        Assert.assertEquals(4, count4);

        // verify objectstorage_object_metadata
        int count5 =
                JdbcTestUtils.countRowsInTable(jdbcTemplate, "objectstorage_object_metadata");
        Assert.assertEquals(4, count5);
    }

    private void cleanTables() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "odc_sql_script");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "script_meta");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "objectstorage_object_metadata");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "objectstorage_object_block");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "objectstorage_bucket");
    }
}
