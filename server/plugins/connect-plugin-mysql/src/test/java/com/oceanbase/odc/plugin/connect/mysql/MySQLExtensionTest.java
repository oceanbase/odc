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
package com.oceanbase.odc.plugin.connect.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.model.SqlExplain;
import com.oceanbase.odc.core.sql.execute.model.SqlExecTime;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.TestResult;
import com.oceanbase.odc.plugin.connect.api.TraceExtensionPoint;
import com.oceanbase.odc.plugin.connect.model.ConnectionPropertiesBuilder;
import com.oceanbase.odc.plugin.connect.model.JdbcUrlProperty;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/5/30
 * @since ODC_release_4.2.0
 */
@Slf4j
public class MySQLExtensionTest extends BaseExtensionPointTest {
    private static ConnectionExtensionPoint connectionExtensionPoint;
    private static SessionExtensionPoint sessionExtensionPoint;
    private static InformationExtensionPoint informationExtensionPoint;
    private static TraceExtensionPoint traceExtensionPoint;
    private static MySQLDiagnoseExtensionPoint diagnoseExtensionPoint;
    private static TestDBConfiguration configuration;
    private static HashMap parameter = new HashMap();

    @BeforeClass
    public static void init() {
        configuration = TestDBConfigurations.getInstance().getTestMysqlConfiguration();
        connectionExtensionPoint = getInstance(MySQLConnectionExtension.class);
        sessionExtensionPoint = getInstance(MySQLSessionExtension.class);
        informationExtensionPoint = getInstance(MySQLInformationExtension.class);
        traceExtensionPoint = getInstance(MySQLTraceExtension.class);
        diagnoseExtensionPoint = getInstance(MySQLDiagnoseExtensionPoint.class);
        parameter.put("useSSL", "false");
    }

    private JdbcUrlProperty getJdbcProperties() {
        return new JdbcUrlProperty(configuration.getHost(), configuration.getPort(), configuration.getDefaultDBName(),
                parameter);
    }

    private Properties getTestConnectionProperties() {
        return ConnectionPropertiesBuilder.getBuilder().user(configuration.getUsername())
                .password(configuration.getPassword())
                .build();
    }

    @Test
    public void test_mysql_url_is_valid() {
        String url = connectionExtensionPoint.generateJdbcUrl(getJdbcProperties());
        TestResult result = connectionExtensionPoint.test(url, getTestConnectionProperties(), 30);
        Assert.assertTrue(result.isActive());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void test_mysql_connect_invalid_password() {
        String url = connectionExtensionPoint.generateJdbcUrl(getJdbcProperties());
        Properties testConnectionProperties = getTestConnectionProperties();
        testConnectionProperties.put(ConnectionPropertiesBuilder.PASSWORD, UUID.randomUUID().toString());
        TestResult result = connectionExtensionPoint.test(url, testConnectionProperties, 30);
        Assert.assertFalse(result.isActive());
        Assert.assertEquals(ErrorCodes.ObAccessDenied, result.getErrorCode());
    }

    @Test
    @Ignore("TODO: fix this test")
    public void test_mysql_connect_invalid_port() {
        JdbcUrlProperty jdbcProperties = getJdbcProperties();
        jdbcProperties.setPort(configuration.getPort() + 100);
        String url = connectionExtensionPoint.generateJdbcUrl(jdbcProperties);
        TestResult result = connectionExtensionPoint.test(url, getTestConnectionProperties(), 30);
        Assert.assertFalse(result.isActive());
        Assert.assertEquals(ErrorCodes.ConnectionUnknownPort, result.getErrorCode());
    }

    @Test
    public void test_mysql_connect_invalid_host() {
        JdbcUrlProperty jdbcProperties = getJdbcProperties();
        jdbcProperties.setHost(UUID.randomUUID().toString());
        String url = connectionExtensionPoint.generateJdbcUrl(jdbcProperties);
        TestResult result = connectionExtensionPoint.test(url, getTestConnectionProperties(), 30);

        Assert.assertFalse(result.isActive());
        Assert.assertEquals(ErrorCodes.ConnectionUnknownHost, result.getErrorCode());
    }

    @Test
    public void test_mysql_connect_driver() {
        Assert.assertEquals(
                connectionExtensionPoint.getDriverClassName(), OdcConstants.MYSQL_DRIVER_CLASS_NAME);
    }

    @Test
    public void test_mysql_connect_get_initializers() throws SQLException {
        List<ConnectionInitializer> connectionInitializers = connectionExtensionPoint.getConnectionInitializers();
        Assert.assertFalse(CollectionUtils.isEmpty(connectionInitializers));
        try (Connection connection = getConnection()) {
            connectionInitializers.forEach(initializer -> {
                try {
                    initializer.init(connection);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    public void test_mysql_session_get_connect_id() throws SQLException {
        try (Connection connection = getConnection()) {
            String connectId = sessionExtensionPoint.getConnectionId(connection);
            Assert.assertNotNull("connectId is null", connectId);
        }
    }

    @Test
    public void test_mysql_session_switch_schema() {
        String targetSchema = "information_schema";
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
    public void test_mysql_session_kill_query() throws SQLException {
        try (Connection connection = getConnection();
                Connection targetConnection = getConnection()) {
            String connectId = sessionExtensionPoint.getConnectionId(targetConnection);
            sessionExtensionPoint.killQuery(connection, connectId);
            Assert.assertTrue(targetConnection.isValid(3));
            Assert.assertFalse(targetConnection.isClosed());
        }
    }

    @Test(expected = Exception.class)
    public void test_mysql_session_kill_query_invalid_id() throws SQLException {
        try (Connection connection = getConnection()) {
            sessionExtensionPoint.killQuery(connection, "-1");
        }
    }

    @Test
    public void test_mysql_information_get_db_version() throws SQLException {
        try (Connection connection = getConnection()) {
            String version = informationExtensionPoint.getDBVersion(connection);
            Assert.assertNotNull("Version is null", version);
        }
    }

    @Test
    public void test_mysql_get_sql_execute_time() throws SQLException {
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            statement.execute("set session profiling=1;");
            statement.execute("select 1 from dual;");
            SqlExecTime detail = traceExtensionPoint.getExecuteDetail(statement, null);
            Assert.assertTrue(detail.getExecuteMicroseconds() > 0);
        }
    }

    @Test
    public void test_mysql_get_sql_execute_time_when_profiling_is_OFF() throws SQLException {
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            statement.execute("set session profiling=0;");
            statement.execute("select 1 from dual;");
            SqlExecTime detail = traceExtensionPoint.getExecuteDetail(statement, null);
            Assert.assertNull(detail.getExecuteMicroseconds());
        }
    }

    @Test
    public void test_mysql_get_sql_explain() throws SQLException {
        try (Connection connection = getConnection()) {
            Statement stmt = connection.createStatement();
            String tableDdl1 = "CREATE TABLE `tab1` (\n"
                    + "  `id` int(11) NOT NULL,\n"
                    + "  UNIQUE KEY `uq1` (`id`)\n"
                    + ");";
            String tableDdl2 = "CREATE TABLE `tab2` (\n"
                    + "  `id` int(11) NOT NULL,\n"
                    + "  UNIQUE KEY `uq2` (`id`)\n"
                    + ");";
            stmt.execute(tableDdl1);
            stmt.execute(tableDdl2);
            String sql = "select tab1.* from tab1 inner join tab2 where tab1.id=tab2.id;";
            SqlExplain explain = diagnoseExtensionPoint.getExplain(connection.createStatement(), sql);
            Assert.assertNotNull(explain.getOriginalText());
            stmt.execute("drop table if exists tab1");
            stmt.execute("drop table if exists tab2");
        }
    }
}
