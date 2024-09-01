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
package com.oceanbase.odc.service.config;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;

public class SystemConfigBootstrapTest extends ServiceTestEnv {
    @Autowired
    private SystemConfigBootstrap systemConfigBootstrap;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "config_system_configuration", "`key` in ('key1', 'key2')");
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty("ODC_APP_EXTRA_ARGS");
    }

    @Test
    public void bootstrap() {
        System.setProperty("ODC_APP_EXTRA_ARGS", "--key1=value1 --key2=value2 --key3novalue noprefix");

        systemConfigBootstrap.bootstrap();

        int insertRows = JdbcTestUtils.countRowsInTableWhere(jdbcTemplate,
                "config_system_configuration", "`key` in ('key1', 'key2')");

        Assert.assertEquals(2, insertRows);
    }
}
