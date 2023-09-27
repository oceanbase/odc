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
package com.oceanbase.tools.dbbrowser.env;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link BaseTestEnv}
 *
 * @author yh263208
 * @date 2023-02-21 15:06
 * @see BasePropertiesEnv
 * @since db-browser_1.0.0-SNAPSHOT
 */
@Slf4j
public abstract class BaseTestEnv extends BasePropertiesEnv {

    private static final String OB_MYSQL_COMMANDLINE_KEY = "odc.ob.default.mysql.commandline";
    private static final String OB_ORACLE_COMMANDLINE_KEY = "odc.ob.default.oracle.commandline";
    private static final String MYSQL_COMMANDLINE_KEY = "odc.mysql.default.commandline";
    private static final Map<String, SingleConnectionDataSource> DATASOURCE_MAP = new HashMap<>();
    private static final int MAX_HOST_NAME_LENGTH = 16;
    private static final String OB_MYSQL_DS_KEY = "OB_MYSQL_DATA_SOURCE";
    private static final String OB_ORACLE_DS_KEY = "OB_ORACLE_DATA_SOURCE";
    private static final String MYSQL_DS_KEY = "MYSQL_DATA_SOURCE";
    private static final String TEST_OB_MYSQL_DATABASE_NAME = generate().toLowerCase();
    private static final String TEST_OB_ORACLE_DATABASE_NAME = generate().toUpperCase();
    private static final String TEST_MYSQL_DATABASE_NAME = generate().toLowerCase();

    static {
        String obMysqlCommandLine = get(OB_MYSQL_COMMANDLINE_KEY);
        ConnectionParseResult obMysqlParseResult = MySQLClientArgsParser.parse(obMysqlCommandLine);
        initDataSource(obMysqlParseResult, OB_MYSQL_DS_KEY);

        String obOracleCommandLine = get(OB_ORACLE_COMMANDLINE_KEY);
        ConnectionParseResult obOracleParseResult = MySQLClientArgsParser.parse(obOracleCommandLine);
        initDataSource(obOracleParseResult, OB_ORACLE_DS_KEY);

        String mysqlCommandLine = get(MYSQL_COMMANDLINE_KEY);
        ConnectionParseResult mysqlParseResult = MySQLClientArgsParser.parse(mysqlCommandLine);
        initDataSource(mysqlParseResult, MYSQL_DS_KEY);

        Thread shutdownHookThread = new Thread(() -> {
            clear(obMysqlParseResult, OB_MYSQL_DS_KEY);
            log.info("Clear OB MySQL database succeed, database={}", TEST_OB_MYSQL_DATABASE_NAME);
            clear(obOracleParseResult, OB_ORACLE_DS_KEY);
            log.info("Clear OB Oracle user succeed, user={}", TEST_OB_ORACLE_DATABASE_NAME);
            clear(mysqlParseResult, MYSQL_DS_KEY);
            log.info("Clear MySQL user succeed, user={}", TEST_MYSQL_DATABASE_NAME);
            DATASOURCE_MAP.values().forEach(SingleConnectionDataSource::destroy);
            log.info("Clear datasource succeed");
        });
        shutdownHookThread.setDaemon(true);
        shutdownHookThread.setName("thread-unit-test-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
    }

    protected static DataSource getOBMySQLDataSource() {
        return DATASOURCE_MAP.get(OB_MYSQL_DS_KEY);
    }

    protected static DataSource getOBOracleDataSource() {
        return DATASOURCE_MAP.get(OB_ORACLE_DS_KEY);
    }

    protected static DataSource getMySQLDataSource() {
        return DATASOURCE_MAP.get(MYSQL_DS_KEY);
    }

    protected static String getOBMySQLDataBaseName() {
        return TEST_OB_MYSQL_DATABASE_NAME;
    }

    protected static String getOBOracleSchema() {
        return TEST_OB_ORACLE_DATABASE_NAME;
    }

    protected static String getMySQLDataBaseName() {
        return TEST_MYSQL_DATABASE_NAME;
    }

    /**
     * 生成用于不同环境UT执行相互隔离的名称
     *
     * @return 本机使用的隔离名称
     */
    private static String generate() {
        String hostName = SystemUtils.getHostName();
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.warn("get localhost failed, message={}", e.getMessage());
            }
            if (hostName == null) {
                hostName = RandomStringUtils.randomAlphabetic(8).toLowerCase();
                log.warn("get host name failed, use random string instead");
            }
        }
        String removeSpecial = hostName.replaceAll("[.\\-\\s]", "");
        if (removeSpecial.length() > MAX_HOST_NAME_LENGTH) {
            hostName = removeSpecial.substring(0, 8) + removeSpecial.substring(removeSpecial.length() - 8);
        } else {
            hostName = removeSpecial;
        }
        log.info("hostName={}, removeSpecial={}, HOST_NAME={}", hostName, removeSpecial, hostName);
        long currentMillis = System.currentTimeMillis() % 1000000;
        return "dbbrowser_" + hostName + "_" + currentMillis;
    }

    private static void initDataSource(ConnectionParseResult parseResult, String dataSourceKey) {
        clear(parseResult, dataSourceKey);
        String jdbcUrl = buildOBJdbcUrl(parseResult);
        String username = buildUser(parseResult);
        String origin = parseResult.getDefaultDBName();
        if (OB_MYSQL_DS_KEY.equals(dataSourceKey)) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, parseResult.getPassword())) {
                try (Statement statement = connection.createStatement()) {
                    String sql = String.format("CREATE DATABASE %s;", TEST_OB_MYSQL_DATABASE_NAME);
                    statement.executeUpdate(sql);
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            parseResult.setDefaultDBName(TEST_OB_MYSQL_DATABASE_NAME);
            DATASOURCE_MAP.put(OB_MYSQL_DS_KEY, buildDataSource(parseResult, ConnectType.OB_MYSQL));
        } else if (OB_ORACLE_DS_KEY.equals(dataSourceKey)) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, parseResult.getPassword())) {
                try (Statement statement = connection.createStatement()) {
                    String sql = "CREATE USER " + TEST_OB_ORACLE_DATABASE_NAME;
                    if (StringUtils.isNotEmpty(parseResult.getPassword())) {
                        sql += " IDENTIFIED BY \"" + parseResult.getPassword() + "\"";
                    }
                    statement.executeUpdate(sql);
                    sql = "GRANT ALL PRIVILEGES TO " + TEST_OB_ORACLE_DATABASE_NAME;
                    statement.executeUpdate(sql);
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            parseResult.setDefaultDBName(TEST_OB_ORACLE_DATABASE_NAME);
            DATASOURCE_MAP.put(OB_ORACLE_DS_KEY, buildDataSource(parseResult, ConnectType.OB_ORACLE));
        } else if (MYSQL_DS_KEY.equals(dataSourceKey)) {
            jdbcUrl = buildMySQLJdbcUrl(parseResult);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, parseResult.getPassword())) {
                try (Statement statement = connection.createStatement()) {
                    String sql = String.format("CREATE DATABASE %s;", TEST_MYSQL_DATABASE_NAME);
                    statement.executeUpdate(sql);
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            parseResult.setDefaultDBName(TEST_MYSQL_DATABASE_NAME);
            DATASOURCE_MAP.put(MYSQL_DS_KEY, buildDataSource(parseResult, ConnectType.MYSQL));
        } else {
            throw new IllegalArgumentException("Invalid data source key, " + dataSourceKey);
        }
        parseResult.setDefaultDBName(origin);
    }

    private static void clear(ConnectionParseResult parseResult, String dataSourceKey) {
        String jdbcUrl = buildOBJdbcUrl(parseResult);
        String username = buildUser(parseResult);
        if (OB_MYSQL_DS_KEY.equals(dataSourceKey)) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, parseResult.getPassword())) {
                try (Statement statement = connection.createStatement()) {
                    String sql = String.format("DROP DATABASE IF EXISTS %s;", TEST_OB_MYSQL_DATABASE_NAME);
                    statement.executeUpdate(sql);
                }
            } catch (Exception e) {
                log.warn("Failed to drop database, errMsg={}", e.getMessage());
            }
        } else if (OB_ORACLE_DS_KEY.equals(dataSourceKey)) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, parseResult.getPassword())) {
                try (Statement statement = connection.createStatement()) {
                    String sql = String.format("DROP USER %s CASCADE;", TEST_OB_ORACLE_DATABASE_NAME);
                    statement.executeUpdate(sql);
                }
            } catch (Exception e) {
                log.warn("Failed to drop user, errMsg={}", e.getMessage());
            }
        } else if (MYSQL_DS_KEY.equals(dataSourceKey)) {
            jdbcUrl = buildMySQLJdbcUrl(parseResult);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, parseResult.getPassword())) {
                try (Statement statement = connection.createStatement()) {
                    String sql = String.format("DROP DATABASE IF EXISTS %s;", TEST_MYSQL_DATABASE_NAME);
                    statement.executeUpdate(sql);
                }
            } catch (Exception e) {
                log.warn("Failed to drop database, errMsg={}", e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Invalid data source key, " + dataSourceKey);
        }
    }

    private static String buildOBJdbcUrl(ConnectionParseResult parseResult) {
        StringBuilder builder =
                new StringBuilder(
                        String.format("jdbc:oceanbase://%s:%d", parseResult.getHost(), parseResult.getPort()));
        if (StringUtils.isNotBlank(parseResult.getDefaultDBName())) {
            builder.append(String.format("/%s", parseResult.getDefaultDBName()));
        }
        return builder.toString();
    }

    private static String buildMySQLJdbcUrl(ConnectionParseResult parseResult) {
        StringBuilder builder =
                new StringBuilder(
                        String.format("jdbc:mysql://%s:%d", parseResult.getHost(), parseResult.getPort()));
        if (StringUtils.isNotBlank(parseResult.getDefaultDBName())) {
            builder.append(String.format("/%s", parseResult.getDefaultDBName()));
        }
        builder.append("?useSSL=false");
        return builder.toString();
    }

    private static String buildUser(ConnectionParseResult parseResult) {
        StringBuilder builder = new StringBuilder(parseResult.getUsername());
        if (StringUtils.isNotBlank(parseResult.getTenant())) {
            builder.append("@").append(parseResult.getTenant());
        }
        if (StringUtils.isNotBlank(parseResult.getCluster())) {
            builder.append("#").append(parseResult.getCluster());
        }
        return builder.toString();
    }

    private static SingleConnectionDataSource buildDataSource(ConnectionParseResult parseResult, ConnectType type) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        if (ConnectType.MYSQL.equals(type)) {
            dataSource.setUrl(buildMySQLJdbcUrl(parseResult));
        } else {
            dataSource.setUrl(buildOBJdbcUrl(parseResult));
        }
        dataSource.setUsername(buildUser(parseResult));
        dataSource.setPassword(parseResult.getPassword());
        dataSource.setAutoCommit(true);
        return dataSource;
    }

    private enum ConnectType {
        OB_MYSQL,
        OB_ORACLE,
        MYSQL,
    }

}
