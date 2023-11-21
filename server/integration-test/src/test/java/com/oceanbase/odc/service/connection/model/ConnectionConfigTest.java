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
package com.oceanbase.odc.service.connection.model;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionStatus;
import com.oceanbase.odc.service.connection.ConnectionStatusManager.CheckState;
import com.oceanbase.odc.test.tool.TestRandom;

public class ConnectionConfigTest {

    @Test
    public void testToString_NoPassword() {
        ConnectionConfig connection = TestRandom.nextObject(ConnectionConfig.class);
        String s = connection.toString();
        Assert.assertFalse(StringUtils.contains(s, "Password"));
    }

    @Test
    public void testJson_NoPassword() {
        ConnectionConfig connection = TestRandom.nextObject(ConnectionConfig.class);
        connection.setStatus(CheckState.of(ConnectionStatus.ACTIVE));
        String s = JsonUtils.toJson(connection);
        Assert.assertFalse(StringUtils.contains(s, "Password"));
    }

    @Test
    public void defaultSchema_OracleWithoutQuotes_UPPERCASE() {
        ConnectionConfig connection = new ConnectionConfig();
        connection.setType(ConnectType.OB_ORACLE);
        connection.setUsername("user1");

        String defaultSchema = connection.getDefaultSchema();

        Assert.assertEquals("USER1", defaultSchema);
    }

    @Test
    public void defaultSchema_OracleWithQuotes_QuotesRemoved() {
        ConnectionConfig connection = new ConnectionConfig();
        connection.setType(ConnectType.OB_ORACLE);
        connection.setUsername("\"user1\"");

        String defaultSchema = connection.getDefaultSchema();

        Assert.assertEquals("user1", defaultSchema);
    }

    @Test
    public void defaultSchema_OracleDefaultSchemaNull_UsernameInstead() {
        ConnectionConfig connection = new ConnectionConfig();
        connection.setType(ConnectType.OB_ORACLE);
        connection.setDefaultSchema(null);
        connection.setUsername("\"user1\"");

        String defaultSchema = connection.getDefaultSchema();

        Assert.assertEquals("user1", defaultSchema);
    }

    @Test
    public void defaultSchema_Mysql_SAME() {
        ConnectionConfig connection = new ConnectionConfig();
        connection.setType(ConnectType.OB_MYSQL);
        connection.setDefaultSchema("user1");

        String defaultSchema = connection.getDefaultSchema();

        Assert.assertEquals("user1", defaultSchema);
    }
}
