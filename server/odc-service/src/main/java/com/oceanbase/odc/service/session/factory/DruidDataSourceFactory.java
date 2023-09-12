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
import java.util.Map;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.session.initializer.SessionCreatedInitializer;

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

    private long queryTimeOut;

    public DruidDataSourceFactory(ConnectionConfig connectionConfig, ConnectionAccountType accountType,
            long queryTimeout) {
        super(connectionConfig, accountType, null);
        this.queryTimeOut = queryTimeout;
    }

    public DruidDataSourceFactory(ConnectionConfig connectionConfig, ConnectionAccountType accountType) {
        super(connectionConfig, accountType, null);
    }

    @Override
    public DataSource getDataSource() {
        String jdbcUrl = getJdbcUrl();
        String username = getUsername();
        String password = getPassword();
        DruidDataSource dataSource = new InnerDataSource(new SessionCreatedInitializer(connectionConfig));
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(connectionExtensionPoint.getDriverClassName());
        init(dataSource);
        dataSource.setSocketTimeout((int) queryTimeOut);
        return dataSource;
    }

    private void init(DruidDataSource dataSource) {
        String validationQuery = getConnectType().getDialectType().isMysql() ? "select 1" : "select 1 from dual";
        dataSource.setValidationQuery(validationQuery);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setDefaultAutoCommit(true);
        dataSource.setMaxActive(5);
        dataSource.setInitialSize(2);
        // wait for get available connection from connection pool
        dataSource.setMaxWait(10_000L);
    }

    @Override
    public CloneableDataSourceFactory deepCopy() {
        ConnectionMapper mapper = ConnectionMapper.INSTANCE;
        return new DruidDataSourceFactory(mapper.clone(connectionConfig), this.accountType, queryTimeOut);
    }

    static class InnerDataSource extends DruidDataSource {

        private final ConnectionInitializer initializer;

        private InnerDataSource(@NonNull ConnectionInitializer initializer) {
            this.initializer = initializer;
        }

        @Override
        public void initPhysicalConnection(Connection conn, Map<String, Object> variables,
                Map<String, Object> globalVariables) throws SQLException {
            try {
                super.initPhysicalConnection(conn, variables, globalVariables);
            } finally {
                try {
                    this.initializer.init(conn);
                } catch (Exception e) {
                    // eat exception
                }
            }
        }
    }

}
