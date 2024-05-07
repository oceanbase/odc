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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.ConflictException;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Single connection {@link javax.sql.DataSource}, only one database connection will be initialized
 * when the data source is created. The usage scenario of this data source is to execute SQL in the
 * {@code ODC} console. This scenario requires the data source to maintain a long connection. If the
 * {@link Connection} is reset, there needs to be a mechanism to notify the external caller. At the
 * same time, some business settings of {@code ODC} need to be considered when initializing the
 * {@link Connection}.
 *
 * @author yh263208
 * @date 2021-11-08 16:20
 * @since ODC_release_3.2.2
 */
@Slf4j
public class SingleConnectionDataSource extends BaseClassBasedDataSource implements AutoCloseable {
    @Setter
    @Getter
    private Boolean autoCommit;
    @Getter
    private final boolean autoReconnect;
    @Setter
    private EventPublisher eventPublisher;
    protected volatile Connection connection;
    private final List<ConnectionInitializer> initializerList = new LinkedList<>();
    private Lock lock;
    @Setter
    private long timeOutMillis = 10 * 1000;

    public SingleConnectionDataSource() {
        this(false);
    }

    public SingleConnectionDataSource(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (Objects.isNull(this.connection)) {
            return innerCreateConnection();
        }
        if (!tryLock()) {
            throw new ConflictException(ErrorCodes.ConnectionOccupied, new Object[] {},
                    "Connection is occupied, waited " + this.timeOutMillis + " millis");
        }
        try {
            if (this.connection.isClosed() || !this.connection.isValid(getLoginTimeout())) {
                if (!autoReconnect) {
                    throw new SQLException("Connection was closed or not valid");
                }
                resetConnection();
            }
            return getConnectionProxy(connection);
        } finally {
            log.info("Get connection unlock, hashcode=" + this.lock.hashCode());
            this.lock.unlock();
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (Objects.equals(username, getUsername()) && Objects.equals(getPassword(), password)) {
            return getConnection();
        }
        throw new SQLException("Invalid username or password");
    }

    /**
     * Reset a {@code Connection}
     */
    public synchronized void resetConnection() throws SQLException {
        log.info("The connection will be reset soon");
        close();
        this.connection = null;
        this.lock = null;
        try (Connection conn = innerCreateConnection()) {
            onConnectionReset(conn);
        }
    }

    public void addInitializer(@NonNull ConnectionInitializer connectionInitializer) {
        this.initializerList.add(connectionInitializer);
    }

    @Override
    public void close() {
        if (!Objects.isNull(this.connection)) {
            try {
                this.connection.close();
            } catch (Throwable throwable) {
                log.error("Failed to close the connection", throwable);
            }
        }
    }

    protected void prepareConnection(Connection con) throws SQLException {
        Boolean autoCommit = getAutoCommit();
        if (autoCommit != null && con.getAutoCommit() != autoCommit) {
            con.setAutoCommit(autoCommit);
        }
    }

    private boolean tryLock() {
        try {
            boolean locked = this.lock.tryLock(timeOutMillis, TimeUnit.MILLISECONDS);
            if (locked) {
                log.info("Get connection lock success, lock={}", this.lock.hashCode());
            }
            return locked;
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private Connection getConnectionProxy(@NonNull Connection connection) {
        if (!tryLock()) {
            throw new ConflictException(ErrorCodes.ConnectionOccupied, new Object[] {},
                    "Connection is occupied, waited " + this.timeOutMillis + " millis");
        }
        try {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class[] {Connection.class},
                    new CloseIgnoreInvocationHandler(connection, this.lock));
        } catch (Exception e) {
            log.warn("Get connection error unlock, hashcode=" + this.lock.hashCode());
            this.lock.unlock();
            throw e;
        }
    }

    @Override
    protected void initConnection(Connection connection) throws SQLException {
        for (ConnectionInitializer initializer : this.initializerList) {
            try {
                initializer.init(connection);
            } catch (Exception e) {
                log.warn("Failed to initialize connection", e);
                throw new SQLException(e);
            }
        }
    }

    @Override
    protected void onConnectionCreated(Connection connection) {
        if (log.isDebugEnabled()) {
            log.debug("Connection is created");
        }
    }

    private void onConnectionReset(Connection connection) {
        if (eventPublisher == null) {
            return;
        }
        try {
            this.eventPublisher.publishEvent(new ConnectionResetEvent(connection));
        } catch (Exception e) {
            log.warn("Failed to publish event", e);
        }
    }

    private synchronized Connection innerCreateConnection() throws SQLException {
        if (this.connection != null) {
            throw new IllegalStateException("Connection is not null");
        }
        try {
            this.connection = newConnectionFromDriver(getUsername(), getPassword());
            this.lock = new ReentrantLock();
            log.info("Established shared JDBC Connection,lock=" + this.lock.hashCode());
            prepareConnection(this.connection);
            return getConnectionProxy(this.connection);
        } catch (Throwable e) {
            throw new SQLException(e);
        }
    }

    /**
     * Invocation handler that suppresses close calls on JDBC Connections.
     *
     * @author yh263208
     * @date 2021-11-09 17:54
     * @since ODC_release_3.2.2
     * @see java.lang.reflect.InvocationHandler
     */
    public static class CloseIgnoreInvocationHandler implements InvocationHandler {
        private final Connection target;
        private final Lock lock;

        public CloseIgnoreInvocationHandler(Connection target, Lock lock) {
            this.target = target;
            this.lock = lock;
        }

        @Override
        @SuppressWarnings("all")
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("equals".equals(method.getName())) {
                return (proxy == args[0]);
            } else if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            } else if ("unwrap".equals(method.getName())) {
                if (((Class<?>) args[0]).isInstance(proxy)) {
                    return proxy;
                }
            } else if ("isWrapperFor".equals(method.getName())) {
                if (((Class<?>) args[0]).isInstance(proxy)) {
                    return true;
                }
            } else if ("close".equals(method.getName())) {
                log.info("Get connection unlock, hashcode=" + this.lock.hashCode());
                lock.unlock();
                return null;
            } else if ("isClosed".equals(method.getName())) {
                return false;
            }
            try {
                return method.invoke(this.target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

}

