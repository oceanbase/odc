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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Data source proxy, mainly used to proxy data source methods. The main application scenario is to
 * intercept the {@code #getConnection} method and set the schema after the user modifies the
 * current database schema
 *
 * @author yh263208
 * @date 2021-11-19 11:16
 * @since ODC_release_3.2.2
 */
@Slf4j
public class ProxyDataSource implements DataSource, AutoCloseable {
    @Setter
    private ConnectionInitializer initializer;
    private final DataSource dataSource;

    public ProxyDataSource(@NonNull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = this.dataSource.getConnection();
        preHandle(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = this.dataSource.getConnection(username, password);
        preHandle(connection);
        return connection;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.dataSource.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return this.dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this.dataSource.getParentLogger();
    }

    @Override
    public void close() throws Exception {
        if (this.dataSource instanceof AutoCloseable) {
            ((AutoCloseable) this.dataSource).close();
        }
    }

    private void preHandle(Connection connection) {
        if (this.initializer == null) {
            return;
        }
        try {
            this.initializer.init(connection);
        } catch (Exception e) {
            log.warn("Failed to initialize connection", e);
        }
    }

}
