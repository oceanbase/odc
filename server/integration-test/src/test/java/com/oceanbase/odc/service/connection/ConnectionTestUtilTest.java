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
package com.oceanbase.odc.service.connection;

import java.sql.SQLException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.model.ConnectionConstants;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;

/**
 * {@link ConnectionTestUtilTest}
 *
 * @author yh263208
 * @date 2022-09-29 17:53
 * @since ODC_release_3.5.0
 */
public class ConnectionTestUtilTest {

    /**
     * 这个 test 比较慢，可以考虑手工执行
     */
    @Test
    @Ignore
    public void test_hostIsUnreachable_returnHostUnreachable() throws SQLException {
        ConnectionTestResult actual = testConnectExtensionPoint(ConnectType.OB_MYSQL,
                "jdbc:oceanbase://1.1.1.1", "", "");
        ConnectionTestResult expect = new ConnectionTestResult(TestResult.hostUnreachable("1.1.1.1"), null);
        Assert.assertEquals(expect, actual);
    }

    /**
     * 这个 test 比较慢，可以考虑手工执行
     */
    @Test
    @Ignore
    public void test_hostIsUnknown_returnHostunknown() throws SQLException {
        ConnectionTestResult actual = testConnectExtensionPoint(ConnectType.OB_MYSQL,
                "jdbc:oceanbase://unknown.host", "", "");
        ConnectionTestResult expect = new ConnectionTestResult(TestResult.unknownHost("unknown.host"), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    @Ignore("TODO: fix this test")
    public void test_portIsUnreachable_returnPortUnreachable() throws SQLException {
        ConnectionConfig config = getConnectionConfig(ConnectType.OB_MYSQL);
        String url = String.format("jdbc:oceanbase://%s:%d", config.getHost(), 4321);
        ConnectionTestResult actual =
                testConnectExtensionPoint(ConnectType.OB_MYSQL, url, "", "");
        ConnectionTestResult expect = new ConnectionTestResult(TestResult.unknownPort(4321), null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void test_wrongPassword_returnAccessDenied() throws SQLException {
        ConnectionConfig config = getConnectionConfig(ConnectType.OB_MYSQL);
        String user = generateUser(config.getUsername(), config.getTenantName(), config.getClusterName());
        String jdbcUrl = String.format("jdbc:oceanbase://%s:%d", config.getHost(), config.getPort());
        ConnectionTestResult actual =
                testConnectExtensionPoint(ConnectType.OB_MYSQL, jdbcUrl, user, "4321");
        Assert.assertEquals(ErrorCodes.ObAccessDenied, actual.getErrorCode());
    }

    @Test
    public void test_wrongUsername_returnAccessDenied() throws SQLException {
        ConnectionConfig config = getConnectionConfig(ConnectType.OB_ORACLE);
        String user = config.getUsername() + "@sss" + config.getTenantName();
        String jdbcUrl = String.format("jdbc:oceanbase://%s:%d", config.getHost(), config.getPort());
        ConnectionTestResult actual =
                testConnectExtensionPoint(ConnectType.OB_ORACLE, jdbcUrl, user, config.getPassword());
        Assert.assertEquals(ErrorCodes.ObAccessDenied, actual.getErrorCode());
    }

    @Test
    public void test_testObMysqlSucceed_returnSucceed() throws SQLException {
        ConnectionConfig config = getConnectionConfig(ConnectType.OB_MYSQL);
        String user = generateUser(config.getUsername(), config.getTenantName(), config.getClusterName());
        String jdbcUrl = String.format("jdbc:oceanbase://%s:%d", config.getHost(), config.getPort());
        ConnectionTestResult actual =
                testConnectExtensionPoint(ConnectType.OB_MYSQL, jdbcUrl, user, config.getPassword());
        ConnectionTestResult expect = ConnectionTestResult.success(ConnectType.OB_MYSQL);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void test_testObOracleSucceed_returnSucceed() throws SQLException {
        ConnectionConfig config = getConnectionConfig(ConnectType.OB_ORACLE);
        String user = generateUser(config.getUsername(), config.getTenantName(), config.getClusterName());
        String jdbcUrl = String.format("jdbc:oceanbase://%s:%d", config.getHost(), config.getPort());
        ConnectionTestResult actual =
                testConnectExtensionPoint(ConnectType.OB_ORACLE, jdbcUrl, user, config.getPassword());
        ConnectionTestResult expect = ConnectionTestResult.success(ConnectType.OB_ORACLE);
        Assert.assertEquals(expect, actual);
    }

    private String generateUser(String username, String tenant, String cluster) {
        String user = username;
        if (StringUtils.isNotBlank(tenant)) {
            user = user + "@" + tenant;
        }
        if (StringUtils.isNotBlank(cluster)) {
            user = user + "#" + cluster;
        }
        return user;
    }

    private ConnectionConfig getConnectionConfig(ConnectType type) {
        return TestConnectionUtil.getTestConnectionConfig(type);
    }

    public ConnectionTestResult testConnectExtensionPoint(ConnectType type,
            String jdbcUrl, String username, String password) {
        Properties properties = new Properties();
        properties.put(ConnectionConstants.USER, username);
        properties.put(ConnectionConstants.PASSWORD, password);
        TestResult result = ConnectionPluginUtil.getConnectionExtension(type.getDialectType())
                .test(jdbcUrl, properties, -1);
        if (result.isActive()) {
            return ConnectionTestResult.success(type);
        } else {
            return new ConnectionTestResult(result, null);
        }
    }
}
