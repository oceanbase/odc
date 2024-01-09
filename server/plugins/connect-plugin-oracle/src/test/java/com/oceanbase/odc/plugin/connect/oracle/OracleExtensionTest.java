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

package com.oceanbase.odc.plugin.connect.oracle;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.api.TraceExtensionPoint;
import com.oceanbase.odc.plugin.connect.model.ConnectionConstants;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/11/9
 * @since
 */
@Slf4j
public class OracleExtensionTest extends BaseExtensionPointTest {
    private static ConnectionExtensionPoint connectionExtensionPoint;
    private static InformationExtensionPoint informationExtensionPoint;
    private static SessionExtensionPoint sessionExtensionPoint;
    private static TraceExtensionPoint traceExtensionPoint;


    private static TestDBConfiguration configuration =
            TestDBConfigurations.getInstance().getTestOracleConfiguration();
    private static JdbcTemplate jdbcTemplate = new JdbcTemplate(configuration.getDataSource());

    @BeforeClass
    public static void init() {
        connectionExtensionPoint = getInstance(OracleConnectionExtension.class);
        informationExtensionPoint = getInstance(OracleInformationExtension.class);
        sessionExtensionPoint = getInstance(OracleSessionExtension.class);
        traceExtensionPoint = getInstance(OracleTraceExtension.class);
    }

    private Properties getJdbcProperties() {
        Properties properties = new Properties();
        properties.put(ConnectionConstants.HOST, configuration.getHost());
        properties.put(ConnectionConstants.PORT, configuration.getPort());
        return properties;
    }

    private Properties getTestConnectionProperties() {
        Properties properties = new Properties();
        properties.put(ConnectionConstants.USER, configuration.getUsername());
        properties.put(ConnectionConstants.PASSWORD, configuration.getPassword());
        return properties;
    }

    @Test
    public void test_oracle_connect_driver() {
        Assert.assertEquals(
                connectionExtensionPoint.getDriverClassName(), OdcConstants.ORACLE_DRIVER_CLASS_NAME);
    }

    @Test
    public void test_oracle_connect_with_sid_success() {
        Properties jdbcProperties = getJdbcProperties();
        jdbcProperties.put(ConnectionConstants.SID, configuration.getSID());
        String url = connectionExtensionPoint.generateJdbcUrl(jdbcProperties, null);
        Properties testConnectionProperties = getTestConnectionProperties();
        testConnectionProperties.put(ConnectionConstants.USER_ROLE, configuration.getRole());
        TestResult result = connectionExtensionPoint.test(url, testConnectionProperties, 30);
        Assert.assertTrue(result.isActive());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void test_oracle_connect_with_serviceName_success() {
        Properties jdbcProperties = getJdbcProperties();
        jdbcProperties.put(ConnectionConstants.SERVICE_NAME, configuration.getServiceName());
        String url = connectionExtensionPoint.generateJdbcUrl(jdbcProperties, null);
        Properties testConnectionProperties = getTestConnectionProperties();
        testConnectionProperties.put(ConnectionConstants.USER_ROLE, configuration.getRole());
        TestResult result = connectionExtensionPoint.test(url, testConnectionProperties, 30);
        Assert.assertTrue(result.isActive());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void test_oracle_connect_invalid_password() {
        Properties jdbcProperties = getJdbcProperties();
        jdbcProperties.put(ConnectionConstants.SID, configuration.getSID());
        String url = connectionExtensionPoint.generateJdbcUrl(jdbcProperties, null);

        Properties testConnectionProperties = getTestConnectionProperties();
        testConnectionProperties.put(ConnectionConstants.PASSWORD, "error");
        testConnectionProperties.put(ConnectionConstants.USER_ROLE, "sysdba");

        TestResult result = connectionExtensionPoint.test(url, testConnectionProperties, 30);
        Assert.assertFalse(result.isActive());
    }

    @Test
    public void test_oracle_connect_invalid_port() {
        Properties jdbcProperties = getJdbcProperties();
        jdbcProperties.put(ConnectionConstants.SID, configuration.getSID());
        jdbcProperties.put(ConnectionConstants.PORT, configuration.getPort() + 100);
        String url = connectionExtensionPoint.generateJdbcUrl(jdbcProperties, null);

        Properties testConnectionProperties = getTestConnectionProperties();
        testConnectionProperties.put(ConnectionConstants.USER_ROLE, configuration.getRole());

        TestResult result = connectionExtensionPoint.test(url, testConnectionProperties, 30);

        Assert.assertFalse(result.isActive());
    }

    @Test
    public void test_oracle_connect_invalid_host() {
        Properties jdbcProperties = getJdbcProperties();
        jdbcProperties.put(ConnectionConstants.SID, configuration.getSID());
        jdbcProperties.put(ConnectionConstants.HOST, UUID.randomUUID().toString());
        String url = connectionExtensionPoint.generateJdbcUrl(jdbcProperties, null);

        Properties testConnectionProperties = getTestConnectionProperties();
        testConnectionProperties.put(ConnectionConstants.USER_ROLE, configuration.getRole());
        TestResult result = connectionExtensionPoint.test(url, testConnectionProperties, 30);

        Assert.assertFalse(result.isActive());
    }

    @Test
    public void test_oracle_information_get_db_version() throws SQLException {
        try (Connection connection = getConnection()) {
            String version = informationExtensionPoint.getDBVersion(connection);
            Assert.assertNotNull("Version is null", version);
        }
    }

    @Test
    public void test_oracle_session_switch_schema() {
        String targetSchema = configuration.getDefaultDBName();
        try (Connection connection = getConnection()) {
            String originSchema = sessionExtensionPoint.getCurrentSchema(connection);
            sessionExtensionPoint.switchSchema(connection, targetSchema);
            String changedSchema = sessionExtensionPoint.getCurrentSchema(connection);
            Assert.assertEquals(targetSchema, changedSchema);
            sessionExtensionPoint.switchSchema(connection, originSchema);
        } catch (SQLException e) {
            Assert.assertNull("SwitchSchema occur exception " + e.getMessage(), e);
        }
    }

    @Test
    public void test_ob_oracle_getVariable() throws SQLException {
        try (Connection connection = getConnection()) {
            String waitTimeout = sessionExtensionPoint.getVariable(connection, "nls_timestamp_tz_format");
            Assert.assertNotNull(waitTimeout);
        }
    }

    @Test
    public void test_oracle_session_get_connect_id() throws SQLException {
        try (Connection connection = getConnection()) {
            String connectId = sessionExtensionPoint.getConnectionId(connection);
            Assert.assertNotNull("connectId is null", connectId);
        }
    }

    @Test
    public void test_oracle_getExecuteDetail() throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = "select 1 from dual";
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                SqlExecTime sqlExecTime = traceExtensionPoint.getExecuteDetail(statement, null);
                Assert.assertNotNull(sqlExecTime);
                Assert.assertNotNull(sqlExecTime.getExecuteMicroseconds());
            }
        }
    }

}
