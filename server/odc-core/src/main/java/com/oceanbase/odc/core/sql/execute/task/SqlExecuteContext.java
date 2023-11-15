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
package com.oceanbase.odc.core.sql.execute.task;

import java.sql.Connection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.sql.execute.SessionOperations;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SqlExecuteContext}
 *
 * @author yh263208
 * @date 2022-10-11 14:30
 * @since ODC_release_3.5.0
 * @see Future
 */
@Slf4j
public class SqlExecuteContext<T> implements Future<T> {

    private final Future<T> taskFuture;
    private final CloneableDataSourceFactory dataSourceFactory;
    private final SqlExecuteCallable<T> callable;
    private volatile boolean cancelled;

    public SqlExecuteContext(@NonNull Future<T> taskFuture, @NonNull SqlExecuteCallable<T> callable) {
        this.taskFuture = taskFuture;
        this.callable = callable;
        this.dataSourceFactory = callable.getDataSourceFactory();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = this.taskFuture.cancel(mayInterruptIfRunning);
        try {
            this.cancelled = killQuery();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return result && this.cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.taskFuture.isCancelled() && this.cancelled;
    }

    @Override
    public boolean isDone() {
        return this.taskFuture.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return this.taskFuture.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.taskFuture.get(timeout, unit);
    }

    private boolean killQuery() throws Exception {
        String connectionId = callable.tryGetConnectionId();
        SessionOperations extensionExecutor = callable.getSessionOperations();
        DataSource dataSource = dataSourceFactory.getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            extensionExecutor.killQuery(connection, connectionId);
            log.info("Kill query succeed, connectionId={}", connectionId);
        } catch (Exception e) {
            if (callable.getDataSourceFactory().getDialectType().isOceanbase()) {
                ConnectionSessionUtil.killQueryByDirectConnect(connectionId, dataSourceFactory);
                log.info("Kill query by direct connect succeed, connectionId={}", connectionId);
            } else {
                log.warn("Kill query occur error, connectionId={}", connectionId, e);
                return false;
            }
        } finally {
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        }
        return true;
    }

}
