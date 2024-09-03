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

    @Test
    public void getUsername_noTenant_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username");
        Assert.assertEquals("username", connectionConfig.getUsername());
    }

    @Test
    public void getUsername_nullUsername_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername(null);
        Assert.assertNull(connectionConfig.getUsername());
    }

    @Test
    public void getUsername_containsTenant_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName");
        Assert.assertEquals("username", connectionConfig.getUsername());
    }

    @Test
    public void getTenantName_userNameDoesNotContainTenant_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username");
        connectionConfig.setTenantName("tenantName");
        Assert.assertEquals("tenantName", connectionConfig.getTenantName());
    }

    @Test
    public void getTenantName_userNameContainsTenant_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName");
        Assert.assertEquals("tenantName", connectionConfig.getTenantName());
    }

    @Test
    public void getTenantName_userNameContainsTenantCluster_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#cluster");
        Assert.assertEquals("tenantName", connectionConfig.getTenantName());
    }

    @Test
    public void getTenantName_tenantNameExistsAndUserNameContainsTenantCluster_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#cluster");
        connectionConfig.setTenantName("tenantName");
        Assert.assertEquals("tenantName", connectionConfig.getTenantName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTenantName_tenantNameExistsAndUserNameContainsTenantCluster_expThrown() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#cluster");
        connectionConfig.setTenantName("tenantName1");
        Assert.assertEquals("tenantName", connectionConfig.getTenantName());
    }

    @Test
    public void getOBTenantName_userNameDoesNotContainTenant_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username");
        connectionConfig.setOBTenantName("tenantName");
        Assert.assertEquals("tenantName", connectionConfig.getOBTenantName());
    }

    @Test
    public void getOBTenantName_userNameContainsTenant_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName");
        Assert.assertEquals("tenantName", connectionConfig.getOBTenantName());
    }

    @Test
    public void getOBTenantName_userNameContainsTenantCluster_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#cluster");
        Assert.assertEquals("tenantName", connectionConfig.getOBTenantName());
    }

    @Test
    public void getOBTenantName_tenantNameExistsAndUserNameContainsTenantCluster_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#cluster");
        connectionConfig.setOBTenantName("tenantName");
        Assert.assertEquals("tenantName", connectionConfig.getOBTenantName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getOBTenantName_tenantNameExistsAndUserNameContainsTenantCluster_expThrown() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#cluster");
        connectionConfig.setOBTenantName("tenantName1");
        Assert.assertEquals("tenantName", connectionConfig.getOBTenantName());
    }

    @Test
    public void getClusterName_userNameDoesNotContainCluster_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username");
        connectionConfig.setClusterName("clusterName");
        Assert.assertEquals("clusterName", connectionConfig.getClusterName());
    }

    @Test
    public void getClusterName_userNameContainsCluster_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#clusterName");
        Assert.assertEquals("clusterName", connectionConfig.getClusterName());
    }

    @Test
    public void getClusterName_tenantNameExistsAndUserNameContainsTenantCluster_getSucceed() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#cluster");
        connectionConfig.setClusterName("cluster");
        Assert.assertEquals("cluster", connectionConfig.getClusterName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getClusterName_tenantNameExistsAndUserNameContainsTenantCluster_expThrown() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setUsername("username@tenantName#cluster");
        connectionConfig.setClusterName("cluster1");
        Assert.assertEquals("cluster", connectionConfig.getClusterName());
    }

}
