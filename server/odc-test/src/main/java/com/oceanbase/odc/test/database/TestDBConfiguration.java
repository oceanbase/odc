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

import java.util.Properties;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.odc.test.cli.ConnectionParseResult;
import com.oceanbase.odc.test.cli.MysqlClientArgsParser;
import com.oceanbase.odc.test.util.JdbcUtil;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gaoda.xy
 * @date 2023/2/17 10:47
 */
@Data
@NoArgsConstructor
public class TestDBConfiguration {
    private DataSource dataSource;
    private TestDBType type;
    private String host;
    private Integer port;
    private String cluster;
    private String tenant;
    private String username;
    private String password;
    private String sysUsername;
    private String sysPassword;
    private String defaultDBName;
    /**
     * For oracle mode only
     */
    private String role;
    private String SID;
    private String serviceName;

    public static final String DB_TYPE_KEY = "dbType";
    public static final String DB_COMMANDLINE_KEY = "commandline";
    public static final String DB_SYS_USERNAME_KEY = "sysUsername";
    public static final String DB_SYS_PASSWORD_KEY = "sysPassword";
    public static final String DB_ORACLE_HOST_KEY = "odc.oracle.default.host";
    public static final String DB_ORACLE_PORT_KEY = "odc.oracle.default.port";
    public static final String DB_ORACLE_USER_KEY = "odc.oracle.default.username";
    public static final String DB_ORACLE_PASSWORD_KEY = "odc.oracle.default.password";
    public static final String DB_ORACLE_SID_KEY = "odc.oracle.default.sid";
    public static final String DB_ORACLE_SERVICE_NAME_KEY = "odc.oracle.default.serviceName";
    public static final String DB_ORACLE_ROLE_KEY = "odc.oracle.default.role";
    public static final String DB_ORACLE_OWNER_KEY = "defaultOwner";



    public TestDBConfiguration(Properties properties) {
        this.type = TestDBType.valueOf(properties.getProperty(DB_TYPE_KEY));
        if (TestDBType.ORACLE.name().equals(type.name())) {
            this.host = properties.getProperty(DB_ORACLE_HOST_KEY);
            this.port = Integer.valueOf(properties.getProperty(DB_ORACLE_PORT_KEY));
            this.username = properties.getProperty(DB_ORACLE_USER_KEY);
            this.password = properties.getProperty(DB_ORACLE_PASSWORD_KEY);
            this.SID = properties.getProperty(DB_ORACLE_SID_KEY);
            this.serviceName = properties.getProperty(DB_ORACLE_SERVICE_NAME_KEY);
            this.role = properties.getProperty(DB_ORACLE_ROLE_KEY);
            this.defaultDBName = properties.getProperty(DB_ORACLE_OWNER_KEY);
            initOracleDataSource();
            return;
        }
        String commandLine = properties.getProperty(DB_COMMANDLINE_KEY);
        ConnectionParseResult parseResult = MysqlClientArgsParser.parse(commandLine);
        this.host = parseResult.getHost();
        this.port = parseResult.getPort();
        this.cluster = parseResult.getCluster();
        this.tenant = parseResult.getTenant();
        this.username = parseResult.getUsername();
        this.password = parseResult.getPassword();
        this.defaultDBName = parseResult.getDefaultDBName();
        this.sysUsername = properties.getProperty(DB_SYS_USERNAME_KEY);
        this.sysPassword = properties.getProperty(DB_SYS_PASSWORD_KEY);
        initDataSource();
    }

    public void initDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(JdbcUtil.buildUrl(host, port, defaultDBName, type));
        dataSource.setUsername(JdbcUtil.buildUser(username, tenant, cluster));
        dataSource.setPassword(password);
        String validationQuery =
                type == TestDBType.OB_MYSQL || type == TestDBType.DORIS ? "select 1" : "select 1 from dual";
        dataSource.setValidationQuery(validationQuery);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setDefaultAutoCommit(true);
        dataSource.setMaxActive(5);
        dataSource.setInitialSize(2);
        this.dataSource = dataSource;
    }

    private void initOracleDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        dataSource.setUrl(JdbcUtil.buildUrl(host, port, SID, serviceName));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        Properties properties = new Properties();
        properties.setProperty("internal_logon", role);
        dataSource.setConnectProperties(properties);
        this.dataSource = dataSource;
    }
}
