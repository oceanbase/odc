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
package com.oceanbase.odc.core.datasource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.test.database.TestDBConfiguration;
import com.oceanbase.odc.test.database.TestDBConfigurations;

/**
 * Test case for {@link SingleConnectionDataSource}
 *
 * @author yh263208
 * @date 2021-11-09 19:34
 * @since ODC_release_3.2.2
 */
public class SingleConnectionDataSourceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetOracleConnection() throws Exception {
        SingleConnectionDataSource dataSource = getDataSource(DialectType.OB_ORACLE);
        checkConnection(dataSource);
    }

    @Test
    public void testGetMysqlConnection() throws SQLException {
        SingleConnectionDataSource dataSource = getDataSource(DialectType.OB_MYSQL);
        checkConnection(dataSource);
    }

    @Test
    public void testGetClosedConnection() throws Exception {
        SingleConnectionDataSource dataSource = getDataSource(DialectType.OB_MYSQL);
        checkConnection(dataSource);
        dataSource.close();

        thrown.expectMessage("Connection was closed or not valid");
        thrown.expect(SQLException.class);
        checkConnection(dataSource);
    }

    @Test
    public void testResetConnection() throws Exception {
        SingleConnectionDataSource dataSource = getDataSource(DialectType.OB_MYSQL);
        checkConnection(dataSource);
        dataSource.close();

        dataSource.resetConnection();
        checkConnection(dataSource);
    }

    @Test
    public void testGetConnectionWithUsername() throws SQLException {
        SingleConnectionDataSource dataSource = getDataSource(DialectType.OB_MYSQL);
        checkConnection(dataSource);

        Connection connection =
                dataSource.getConnection(getUsername(DialectType.OB_MYSQL), getPassword(DialectType.OB_MYSQL));
        Assert.assertNotNull(connection);
    }

    @Test
    public void testGetConnectionWithWrongUsername() throws SQLException {
        SingleConnectionDataSource dataSource = getDataSource(DialectType.OB_MYSQL);
        checkConnection(dataSource);

        thrown.expect(SQLException.class);
        thrown.expectMessage("Invalid username or password");
        dataSource.getConnection(getPassword(DialectType.OB_MYSQL), getUsername(DialectType.OB_MYSQL));
    }

    @Test
    public void testGetConnectionLock() throws Exception {
        SingleConnectionDataSource dataSource = getDataSource(DialectType.OB_MYSQL);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        Thread slowSql = new Thread(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet resultSet =
                            statement.executeQuery("select  /*+ QUERY_TIMEOUT (48000000) */  sleep(15)from dual ;")) {
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread quick = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            long start = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection()) {
                connection.isValid(0);
                long end = System.currentTimeMillis();
                System.out.println(end - start);
                if (end - start > 10 * 1000) {
                    throw new RuntimeException("Failed to acquire lock within 10 seconds");
                }
            } catch (ConflictException e) {
                exceptions.add(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        slowSql.start();
        quick.start();
        slowSql.join();
        Assert.assertFalse(exceptions.isEmpty());
        Assert.assertTrue(exceptions.get(0) instanceof ConflictException);
    }

    private void checkConnection(SingleConnectionDataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("select 1+4 from dual")) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals("5", resultSet.getString(1));
                }
            }
        }
    }

    private SingleConnectionDataSource getDataSource(DialectType dialectType) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setUrl(getUrl(dialectType));
        dataSource.setUsername(getUsername(dialectType));
        dataSource.setPassword(getPassword(dialectType));
        dataSource.setAutoCommit(true);
        return dataSource;
    }

    private String getUrl(DialectType dialectType) {
        String protocal = "oceanbase";
        if (DialectType.MYSQL.equals(dialectType)) {
            protocal = "mysql";
        }
        TestDBConfiguration config = getConfig(dialectType);
        if (StringUtils.isNotBlank(config.getDefaultDBName())) {
            return String.format("jdbc:%s://%s:%d/%s", protocal, config.getHost(),
                    config.getPort(), config.getDefaultDBName());
        }
        return String.format("jdbc:%s://%s:%d", protocal, config.getHost(), config.getPort());
    }

    private String getUsername(DialectType dialectType) {
        TestDBConfiguration config = getConfig(dialectType);
        StringBuilder stringBuilder = new StringBuilder(config.getUsername());
        if (StringUtils.isNotBlank(config.getTenant())) {
            stringBuilder.append("@").append(config.getTenant());
        }
        if (StringUtils.isNotBlank(config.getCluster())) {
            stringBuilder.append("#").append(config.getCluster());
        }
        return stringBuilder.toString();
    }

    private String getPassword(DialectType dialectType) {
        TestDBConfiguration config = getConfig(dialectType);
        return config.getPassword();
    }

    private TestDBConfiguration getConfig(DialectType dialectType) {
        TestDBConfigurations configurations = TestDBConfigurations.getInstance();
        if (dialectType.isMysql()) {
            return configurations.getTestOBMysqlConfiguration();
        }
        return configurations.getTestOBOracleConfiguration();
    }

}
