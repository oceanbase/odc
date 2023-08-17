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

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/2/17 10:47
 */
@Data
public class TestDBConfiguration {
    private DataSource dataSource;
    private String host;
    private Integer port;
    private String cluster;
    private String tenant;
    private String username;
    private String password;
    private String sysUsername;
    private String sysPassword;
    private String defaultDBName;
    private String type;

    private static final String OB_JDBC_PROTOCOL = "oceanbase";
    public static final String DB_TYPE_KEY = "dbType";
    public static final String DB_COMMANDLINE_KEY = "commandline";
    public static final String DB_SYS_USERNAME_KEY = "sysUsername";
    public static final String DB_SYS_PASSWORD_KEY = "sysPassword";

    public TestDBConfiguration(Properties properties) {
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
        this.type = properties.getProperty(DB_TYPE_KEY);
        initDataSource();
    }

    private void initDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(TestDBUtil.buildUrl(host, port, defaultDBName, getType()));
        dataSource.setUsername(TestDBUtil.buildUser(username, tenant, cluster));
        dataSource.setPassword(password);
        String validationQuery = "OB_MYSQL".equals(getType()) ? "select 1" : "select 1 from dual";
        dataSource.setValidationQuery(validationQuery);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setDefaultAutoCommit(true);
        dataSource.setMaxActive(5);
        dataSource.setInitialSize(2);
        this.dataSource = dataSource;
    }
}
