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

    private TestDBConfigurations() {
        for (TestDBType type : TestDBType.values()) {
            connectType2ConfigurationMap.put(type, new TestDBConfiguration(getTestDBProperties(type)));
        }
        dropTestDatabases();
        createTestDatabasesAndUpdateConfig();
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
        return connectType2ConfigurationMap.get(TestDBType.OB_MYSQL);
    }

    public TestDBConfiguration getTestOBOracleConfiguration() {
        return connectType2ConfigurationMap.get(TestDBType.OB_ORACLE);
    }

    public TestDBConfiguration getTestMysqlConfiguration() {
        return connectType2ConfigurationMap.get(TestDBType.MYSQL);
    }

    private Properties getTestDBProperties(TestDBType type) {
        Properties properties = new Properties();
        properties.setProperty(TestDBConfiguration.DB_COMMANDLINE_KEY, TestProperties.getProperty(type.commandlineKey));
        String sysUserNameKey = TestProperties.getProperty(type.sysUserNameKey);
        if (sysUserNameKey != null) {
            properties.setProperty(TestDBConfiguration.DB_SYS_USERNAME_KEY, sysUserNameKey);
        }
        String sysUserPasswordKey = TestProperties.getProperty(type.sysUserPasswordKey);
        if (sysUserPasswordKey != null) {
            properties.setProperty(TestDBConfiguration.DB_SYS_PASSWORD_KEY, sysUserPasswordKey);
        }
        properties.setProperty(TestDBConfiguration.DB_TYPE_KEY, type.toString());
        return properties;
    }

    private void expireResources() {
        dropTestDatabases();
        connectType2ConfigurationMap.forEach((k, v) -> {
            if (Objects.nonNull(v) && Objects.nonNull(v.getDataSource())) {
                DruidDataSource datasource = (DruidDataSource) v.getDataSource();
                datasource.close();
            }
        });
    }

    private void createTestDatabasesAndUpdateConfig() {
        log.info("create test database/user start");
        connectType2ConfigurationMap.forEach((k, v) -> {
            try (Connection conn = v.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
                if (k == TestDBType.OB_MYSQL) {
                    String database = TEST_OB_MYSQL_DATABASE_NAME;
                    String sql = String.format("CREATE DATABASE %s;", database);
                    stmt.executeUpdate(sql);
                    log.info("create test database for OB mysql mode, database name: {}", database);
                    v.setDefaultDBName(database);
                    DataSource newSource = createNewDataSource(v);
                    v.setDataSource(newSource);
                } else if (k == TestDBType.OB_ORACLE) {
                    String username = TEST_OB_ORACLE_DATABASE_NAME;
                    StringBuilder sql = new StringBuilder("CREATE USER " + username);
                    if (StringUtils.isNotEmpty(v.getPassword())) {
                        sql.append(" IDENTIFIED BY \"").append(v.getPassword()).append("\"");
                    }
                    sql.append(";");
                    stmt.executeUpdate(sql.toString());
                    log.info("create test user for OB oracle mode, username: {}", username);
                    sql = new StringBuilder("GRANT ALL PRIVILEGES TO ").append(username).append(";");
                    stmt.executeUpdate(sql.toString());
                    sql = new StringBuilder("GRANT SELECT ANY DICTIONARY TO ").append(username).append(";");
                    stmt.execute(sql.toString());
                    log.info("grant all privileges to new created user, username: {}", username);
                    v.setDefaultDBName(username);
                    v.setUsername(username);
                    DataSource newSource = createNewDataSource(v);
                    v.setDataSource(newSource);
                } else if (k == TestDBType.MYSQL) {
                    String database = TEST_MYSQL_DATABASE_NAME;
                    String sql = String.format("CREATE DATABASE %s;", database);
                    stmt.executeUpdate(sql);
                    log.info("create test database for mysql mode, database name: {}", database);
                    v.setDefaultDBName(database);
                    DataSource newSource = createNewDataSource(v);
                    v.setDataSource(newSource);
                }
            } catch (Exception e) {
                log.error("create test database/user failed, connectType={}", k, e);
                throw new RuntimeException("create test database/user failed", e);
            }
        });
        log.info("create test database/user done");
    }

    private void dropTestDatabases() {
        log.info("drop test database/user start");
        connectType2ConfigurationMap.forEach((k, v) -> {
            try (Connection conn = v.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
                if (k == TestDBType.OB_MYSQL) {
                    String database = TEST_OB_MYSQL_DATABASE_NAME;
                    String sql = String.format("DROP DATABASE IF EXISTS %s;", database);
                    stmt.executeUpdate(sql);
                    log.info("drop test database for OB mysql mode, database name: {}", database);
                } else if (k == TestDBType.OB_ORACLE) {
                    String username = TEST_OB_ORACLE_DATABASE_NAME;
                    String sql = String.format("DROP USER %s CASCADE;", username);
                    stmt.executeUpdate(sql);
                    log.info("drop test user for OB oracle mode, username: {}", username);
                } else if (k == TestDBType.MYSQL) {
                    String database = TEST_MYSQL_DATABASE_NAME;
                    String sql = String.format("DROP DATABASE IF EXISTS %s;", database);
                    stmt.executeUpdate(sql);
                    log.info("drop test database for mysql mode, database name: {}", database);
                }
            } catch (Exception e) {
                log.warn("drop test database/user failed, may the user is not exists, connectType={}", k);
            }
        });
        log.info("drop test database/user done");
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
        String validationQuery = config.getType() == TestDBType.OB_MYSQL ? "select 1" : "select 1 from dual";
        dataSource.setValidationQuery(validationQuery);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setDefaultAutoCommit(true);
        dataSource.setMaxActive(5);
        dataSource.setInitialSize(2);
        return dataSource;
    }

}
