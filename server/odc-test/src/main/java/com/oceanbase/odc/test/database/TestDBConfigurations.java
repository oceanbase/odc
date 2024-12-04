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
package com.oceanbase.odc.test.database;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.odc.test.tool.IsolatedNameGenerator;
import com.oceanbase.odc.test.util.JdbcUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/2/17 15:32
 */
@Slf4j
public class TestDBConfigurations {
    private final Map<TestDBType, TestDBConfiguration> connectType2ConfigurationMap = new HashMap<>();
    private static volatile TestDBConfigurations instance;
    private static final String TEST_OB_MYSQL_DATABASE_NAME = IsolatedNameGenerator.generateLowerCase("ODC");
    private static final String TEST_OB_ORACLE_DATABASE_NAME = IsolatedNameGenerator.generateUpperCase("ODC");
    private static final String TEST_MYSQL_DATABASE_NAME = IsolatedNameGenerator.generateUpperCase("ODC");
    private static final String TEST_DORIS_DATABASE_NAME = IsolatedNameGenerator.generateUpperCase("ODC");
    private static final String TEST_ORACLE_DATABASE_NAME = IsolatedNameGenerator.generateUpperCase("ODC");

    private TestDBConfigurations() {
        Thread shutdownHookThread = new Thread(this::expireResources);
        shutdownHookThread.setDaemon(true);
        shutdownHookThread.setName("thread-odc-unit-test-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
        log.info("TestDBConfigurations initialized");
    }

    public static TestDBConfigurations getInstance() {
        if (instance == null) {
            synchronized (TestDBConfigurations.class) {
                if (instance == null) {
                    instance = new TestDBConfigurations();
                }
            }
        }
        return instance;
    }

    public TestDBConfiguration getTestOBMysqlConfiguration() {
        return getTestDBConfiguration(TestDBType.OB_MYSQL);
    }

    public TestDBConfiguration getTestOBOracleConfiguration() {
        return getTestDBConfiguration(TestDBType.OB_ORACLE);
    }

    public TestDBConfiguration getTestMysqlConfiguration() {
        return getTestDBConfiguration(TestDBType.MYSQL);
    }

    public TestDBConfiguration getTestDorisConfiguration() {
        return getTestDBConfiguration(TestDBType.DORIS);
    }

    public TestDBConfiguration getTestOracleConfiguration() {
        return getTestDBConfiguration(TestDBType.ORACLE);
    }

    private TestDBConfiguration getTestDBConfiguration(TestDBType type) {
        if (!connectType2ConfigurationMap.containsKey(type)) {
            TestDBConfiguration configuration = new TestDBConfiguration(getTestDBProperties(type));
            dropTestDatabases(type, configuration);
            createTestDatabasesAndUpdateConfig(type, configuration);
            connectType2ConfigurationMap.put(type, configuration);
        }
        return connectType2ConfigurationMap.get(type);
    }

    private void createTestDatabasesAndUpdateConfig(TestDBType type, TestDBConfiguration configuration) {
        try (Connection conn = configuration.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            if (type == TestDBType.OB_MYSQL) {
                String database = TEST_OB_MYSQL_DATABASE_NAME;
                String sql = String.format("CREATE DATABASE %s;", database);
                stmt.executeUpdate(sql);
                log.info("create test database for OB mysql mode, database name: {}", database);
                configuration.setDefaultDBName(database);
                DataSource newSource = createNewDataSource(configuration);
                configuration.setDataSource(newSource);
            } else if (type == TestDBType.OB_ORACLE) {
                String username = TEST_OB_ORACLE_DATABASE_NAME;
                StringBuilder sql = new StringBuilder("CREATE USER " + username);
                if (StringUtils.isNotEmpty(configuration.getPassword())) {
                    sql.append(" IDENTIFIED BY \"").append(configuration.getPassword()).append("\"");
                }
                sql.append(";");
                stmt.executeUpdate(sql.toString());
                log.info("create test user for OB oracle mode, username: {}", username);
                sql = new StringBuilder("GRANT ALL PRIVILEGES TO ").append(username).append(";");
                stmt.executeUpdate(sql.toString());
                sql = new StringBuilder("GRANT SELECT ANY DICTIONARY TO ").append(username).append(";");
                stmt.execute(sql.toString());
                log.info("grant all privileges to new created user, username: {}", username);
                configuration.setDefaultDBName(username);
                configuration.setUsername(username);
                DataSource newSource = createNewDataSource(configuration);
                configuration.setDataSource(newSource);
            } else if (type == TestDBType.MYSQL) {
                String database = TEST_MYSQL_DATABASE_NAME;
                String sql = String.format("CREATE DATABASE %s;", database);
                stmt.executeUpdate(sql);
                log.info("create test database for mysql, database name: {}", database);
                configuration.setDefaultDBName(database);
                DataSource newSource = createNewDataSource(configuration);
                configuration.setDataSource(newSource);
            } else if (type == TestDBType.DORIS) {
                String database = TEST_DORIS_DATABASE_NAME;
                String sql = String.format("CREATE DATABASE %s;", database);
                stmt.executeUpdate(sql);
                log.info("create test database for doris, database name: {}", database);
                configuration.setDefaultDBName(database);
                DataSource newSource = createNewDataSource(configuration);
                configuration.setDataSource(newSource);
            } else if (type == TestDBType.ORACLE) {
                String username = TEST_ORACLE_DATABASE_NAME;
                StringBuilder sql = new StringBuilder("CREATE USER " + username);
                if (StringUtils.isNotEmpty(configuration.getPassword())) {
                    sql.append(" IDENTIFIED BY \"").append(configuration.getPassword()).append("\"");
                }
                stmt.executeUpdate(sql.toString());
                log.info("create test user for oracle, username: {}", username);
                sql = new StringBuilder("GRANT SYSDBA, RESOURCE, CREATE SESSION TO ").append(username);
                stmt.execute(sql.toString());
                log.info("grant sysdba to new created user, username: {}", username);
                // Although the above code has granted sysdba role to the new user, the connection created with the
                // new user does not use the sysdba role, so the following grant statement is required
                sql = new StringBuilder("GRANT ALL PRIVILEGES TO ").append(username);
                stmt.execute(sql.toString());
                log.info("grant all privileges to new created user, username: {}", username);
                configuration.setDefaultDBName(username);
                configuration.setUsername(username);
            }
        } catch (Exception e) {
            log.error("create test database/user failed, connectType={}", type, e);
            throw new RuntimeException("create test database/user failed", e);
        }
    }

    private void dropTestDatabases(TestDBType type, TestDBConfiguration configuration) {
        try (Connection conn = configuration.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
            if (type == TestDBType.OB_MYSQL) {
                String database = TEST_OB_MYSQL_DATABASE_NAME;
                String sql = String.format("DROP DATABASE IF EXISTS %s;", database);
                stmt.executeUpdate(sql);
                log.info("drop test database for OB mysql mode, database name: {}", database);
            } else if (type == TestDBType.OB_ORACLE) {
                String username = TEST_OB_ORACLE_DATABASE_NAME;
                String sql = String.format("DROP USER %s CASCADE;", username);
                stmt.executeUpdate(sql);
                log.info("drop test user for OB oracle mode, username: {}", username);
            } else if (type == TestDBType.MYSQL) {
                String database = TEST_MYSQL_DATABASE_NAME;
                String sql = String.format("DROP DATABASE IF EXISTS %s;", database);
                stmt.executeUpdate(sql);
                log.info("drop test database for mysql, database name: {}", database);
            } else if (type == TestDBType.ORACLE) {
                String username = TEST_ORACLE_DATABASE_NAME;
                String sql = String.format("DROP USER %s CASCADE", username);
                stmt.executeUpdate(sql);
                log.info("drop test user for oracle, username: {}", username);
            } else if (type == TestDBType.DORIS) {
                String database = TEST_DORIS_DATABASE_NAME;
                String sql = String.format("DROP DATABASE IF EXISTS %s;", database);
                stmt.executeUpdate(sql);
                log.info("drop test database for doris, database name: {}", database);
            }
        } catch (Exception e) {
            log.warn("drop test database/user failed, may the database/user is not exists, connectType={}", type);
        }
    }

    private DataSource createNewDataSource(TestDBConfiguration config) {
        DruidDataSource deprecatedSource = (DruidDataSource) config.getDataSource();
        deprecatedSource.close();
        String jdbcUrl =
                JdbcUtil.buildUrl(config.getHost(), config.getPort(), config.getDefaultDBName(), config.getType());
        String username = JdbcUtil.buildUser(config.getUsername(), config.getTenant(), config.getCluster());
        String password = config.getPassword();
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        String validationQuery =
                config.getType() == TestDBType.OB_MYSQL || config.getType() == TestDBType.DORIS ? "select 1"
                        : "select 1 from dual";
        dataSource.setValidationQuery(validationQuery);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setDefaultAutoCommit(true);
        dataSource.setMaxActive(5);
        dataSource.setInitialSize(2);
        return dataSource;
    }

    private void expireResources() {
        connectType2ConfigurationMap.forEach(this::dropTestDatabases);
        connectType2ConfigurationMap.forEach((k, v) -> {
            if (Objects.nonNull(v) && Objects.nonNull(v.getDataSource())) {
                DruidDataSource datasource = (DruidDataSource) v.getDataSource();
                datasource.close();
            }
        });
    }

    private Properties getTestDBProperties(TestDBType type) {
        Properties properties = new Properties();
        properties.setProperty(TestDBConfiguration.DB_TYPE_KEY, type.toString());
        if (TestDBType.ORACLE.name().equals(type.toString())) {
            properties.setProperty(TestDBConfiguration.DB_ORACLE_HOST_KEY,
                    TestProperties.getProperty(type.commandlineKey[0]));
            properties.setProperty(TestDBConfiguration.DB_ORACLE_PORT_KEY,
                    TestProperties.getProperty(type.commandlineKey[1]));
            properties.setProperty(TestDBConfiguration.DB_ORACLE_USER_KEY,
                    TestProperties.getProperty(type.commandlineKey[2]));
            properties.setProperty(TestDBConfiguration.DB_ORACLE_PASSWORD_KEY,
                    TestProperties.getProperty(type.commandlineKey[3]));
            properties.setProperty(TestDBConfiguration.DB_ORACLE_SID_KEY,
                    TestProperties.getProperty(type.commandlineKey[4]));
            properties.setProperty(TestDBConfiguration.DB_ORACLE_SERVICE_NAME_KEY,
                    TestProperties.getProperty(type.commandlineKey[5]));
            properties.setProperty(TestDBConfiguration.DB_ORACLE_ROLE_KEY,
                    TestProperties.getProperty(type.commandlineKey[6]));
            properties.setProperty(TestDBConfiguration.DB_ORACLE_OWNER_KEY, TEST_ORACLE_DATABASE_NAME);
        } else {
            properties.setProperty(TestDBConfiguration.DB_COMMANDLINE_KEY,
                    TestProperties.getProperty(type.commandlineKey[0]));
            String sysUserNameKey = TestProperties.getProperty(type.sysUserNameKey);
            if (sysUserNameKey != null) {
                properties.setProperty(TestDBConfiguration.DB_SYS_USERNAME_KEY, sysUserNameKey);
            }
            String sysUserPasswordKey = TestProperties.getProperty(type.sysUserPasswordKey);
            if (sysUserPasswordKey != null) {
                properties.setProperty(TestDBConfiguration.DB_SYS_PASSWORD_KEY, sysUserPasswordKey);
            }
        }
        return properties;
    }

}
