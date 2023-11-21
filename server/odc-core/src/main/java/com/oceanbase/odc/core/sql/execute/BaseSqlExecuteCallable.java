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
package com.oceanbase.odc.core.sql.execute;

import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.sql.execute.task.SqlExecuteCallable;

import lombok.NonNull;

/**
 * {@link BaseSqlExecuteCallable}
 *
 * @author yh263208
 * @date 2022-10-11 20:24
 * @since ODC_release_3.5.0
 * @see SqlExecuteCallable
 */
public abstract class BaseSqlExecuteCallable<T> implements SqlExecuteCallable<T> {

    private final CloneableDataSourceFactory dataSourceFactory;
    private final DataSource dataSource;
    private final ConnectionIdSupplier connectionIdSupplier = new ConnectionIdSupplier();
    private final SessionOperations sessionOperations;

    public BaseSqlExecuteCallable(@NonNull DataSource dataSource,
            @NonNull CloneableDataSourceFactory dataSourceFactory,
            @NonNull SessionOperations sessionOperations) {
        this.dataSource = dataSource;
        this.dataSourceFactory = dataSourceFactory;
        this.sessionOperations = sessionOperations;
    }

    @Override
    public T call() throws Exception {
        try (Connection connection = this.dataSource.getConnection()) {
            this.connectionIdSupplier.supply(() -> sessionOperations.getConnectionId(connection));
            Verify.notNull(this.connectionIdSupplier.get(), "ConnectionId");
            return doCall(connection);
        }
    }

    protected abstract T doCall(Connection connection) throws Exception;

    @Override
    public String tryGetConnectionId() throws Exception {
        if (this.connectionIdSupplier.isDone()) {
            return this.connectionIdSupplier.get();
        }
        return this.connectionIdSupplier.get(3, TimeUnit.SECONDS);
    }

    @Override
    public SessionOperations getSessionOperations() {
        return this.sessionOperations;
    }

    @Override
    public CloneableDataSourceFactory getDataSourceFactory() {
        return this.dataSourceFactory;
    }

    /**
     * {@link ConnectionIdSupplier} to hold connection id
     *
     * @author yh263208
     * @date 2022-10-11 20:10
     * @since ODC_release_3.5.0
     */
    static class ConnectionIdSupplier {

        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile String value;
        private volatile Exception thrown = null;

        public String get() throws Exception {
            this.latch.await();
            if (this.thrown != null) {
                throw this.thrown;
            }
            return this.value;
        }

        public String get(long timeout, TimeUnit timeUnit) throws Exception {
            this.latch.await(timeout, timeUnit);
            if (this.thrown != null) {
                throw this.thrown;
            }
            return this.value;
        }

        public boolean isDone() {
            return this.latch.getCount() < 1;
        }

        public void supply(Callable<String> callable) {
            try {
                this.value = callable.call();
            } catch (Exception e) {
                this.thrown = e;
            }
            this.latch.countDown();
        }
    }

}
