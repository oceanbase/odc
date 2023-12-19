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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;

public class R4239AuditEventMetaMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void migrate() {
        R4239AuditEventMetaMigrate migrate = new R4239AuditEventMetaMigrate();
        migrate.migrate(dataSource);

        int count = JdbcTestUtils.countRowsInTableWhere(
                jdbcTemplate, "audit_event_meta", "`id` = 1");
        Assert.assertEquals(1, count);
    }
}
