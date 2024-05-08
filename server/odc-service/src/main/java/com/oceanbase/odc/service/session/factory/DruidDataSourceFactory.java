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
package com.oceanbase.odc.service.session.factory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.jdbc.JdbcUrlParser;
import com.oceanbase.odc.plugin.connect.model.ConnectionPropertiesBuilder;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.initializer.BackupInstanceInitializer;
import com.oceanbase.odc.service.session.initializer.DataSourceInitScriptInitializer;

import lombok.NonNull;

/**
 * {@link DruidDataSourceFactory} used to init {@link DruidDataSource}
 *
 * @author yh263208
 * @date 2021-11-17 11:49
 * @since ODC_release_3.2.2
 * @see DataSourceFactory
 * @see OBConsoleDataSourceFactory
 */
public class DruidDataSourceFactory extends OBConsoleDataSourceFactory {

    private static final int DEFAULT_TIMEOUT_MILLIS = 60000;

    public DruidDataSourceFactory(ConnectionConfig connectionConfig) {
        super(connectionConfig, null);
    }

    @Override
    public DataSource getDataSource() {
        String jdbcUrl = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();
        DruidDataSource dataSource = new InnerDataSource(Arrays.asList(new BackupInstanceInitializer(connectionConfig),
                new DataSourceInitScriptInitializer(connectionConfig)));
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        if (Objects.nonNull(this.userRole)) {
            dataSource.setConnectProperties(ConnectionPropertiesBuilder.getBuilder().userRole(this.userRole).build());
        }
        dataSource.setDriverClassName(connectionExtensionPoint.getDriverClassName());
        init(dataSource);
        return dataSource;
    }

    private void init(DruidDataSource dataSource) {
        String validationQuery =
                getConnectType().getDialectType().isMysql() || getConnectType().getDialectType().isDoris() ? "select 1"
                        : "select 1 from dual";
        dataSource.setValidationQuery(validationQuery);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setDefaultAutoCommit(true);
        dataSource.setMaxActive(5);
        dataSource.setInitialSize(2);
        // wait for get available connection from connection pool
        dataSource.setMaxWait(5000L);
        /**
         * {@link DruidDataSource#init()} will set these two properties to
         * {@link com.alibaba.druid.pool.DruidAbstractDataSource#DEFAULT_TIME_SOCKET_TIMEOUT_MILLIS} if we
         * don't set or set these two properties to zero. Further more, DruidDataSource will ignore these
         * two properties even we define them in jdbc url.
         *
         * so that we should give these two properties a big default value if user don't give a specific
         * value.
         */
        dataSource.setSocketTimeout(DEFAULT_TIMEOUT_MILLIS);
        dataSource.setConnectTimeout(DEFAULT_TIMEOUT_MILLIS);
        // fix arbitrary file reading vulnerability
        Properties properties = Optional.ofNullable(dataSource.getConnectProperties()).orElseGet(Properties::new);
        properties.setProperty("allowLoadLocalInfile", "false");
        properties.setProperty("allowUrlInLocalInfile", "false");
        properties.setProperty("allowLoadLocalInfileInPath", "");
        properties.setProperty("autoDeserialize", "false");
        dataSource.setConnectProperties(properties);
        try {
            setConnectAndSocketTimeoutFromJdbcUrl(dataSource);
        } catch (Exception e) {
            // eat exception
        }
    }

    @Override
    public CloneableDataSourceFactory deepCopy() {
        ConnectionMapper mapper = ConnectionMapper.INSTANCE;
        return new DruidDataSourceFactory(mapper.clone(connectionConfig));
    }

    private void setConnectAndSocketTimeoutFromJdbcUrl(DruidDataSource dataSource) throws SQLException {
        JdbcUrlParser jdbcUrlParser = ConnectionPluginUtil
                .getConnectionExtension(connectionConfig.getDialectType()).getConnectionInfo(getJdbcUrl(), null);
        Object socketTimeout = jdbcUrlParser.getParameters().get("socketTimeout");
        Object connectTimeout = jdbcUrlParser.getParameters().get("connectTimeout");
        if (socketTimeout != null) {
            try {
                dataSource.setSocketTimeout(Integer.parseInt(socketTimeout.toString()));
            } catch (Exception e) {
                // eat exception
            }
        }
        if (connectTimeout != null) {
            try {
                dataSource.setConnectTimeout(Integer.parseInt(connectTimeout.toString()));
            } catch (Exception e) {
                // eat exception
            }
        }
    }

    static class InnerDataSource extends DruidDataSource {

        private final List<ConnectionInitializer> initializers;

        private InnerDataSource(@NonNull List<ConnectionInitializer> initializers) {
            this.initializers = initializers;
        }

        @Override
        public void initPhysicalConnection(Connection conn, Map<String, Object> variables,
                Map<String, Object> globalVariables) throws SQLException {
            try {
                super.initPhysicalConnection(conn, variables, globalVariables);
            } finally {
                try {
                    for (ConnectionInitializer initializer : initializers) {
                        initializer.init(conn);
                    }
                } catch (Exception e) {
                    // eat exception
                }
            }
        }
    }

}
