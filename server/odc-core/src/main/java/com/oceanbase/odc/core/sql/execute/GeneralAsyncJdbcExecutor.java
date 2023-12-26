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

import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;
import com.oceanbase.odc.core.sql.execute.task.SqlExecuteTaskManager;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Universal asynchronous SQL executor
 *
 * @author yh263208
 * @date 2021-11-13 23:33
 * @since ODC_release_3.2.2
 */
@Slf4j
public class GeneralAsyncJdbcExecutor implements AsyncJdbcExecutor {

    private final SqlExecuteTaskManager taskManager;
    private final DataSource dataSource;
    private final CloneableDataSourceFactory dataSourceFactory;
    private final SessionOperations sessionOperations;

    public GeneralAsyncJdbcExecutor(
            @NonNull DataSource dataSource,
            @NonNull CloneableDataSourceFactory dataSourceFactory,
            @NonNull SqlExecuteTaskManager taskManager,
            @NonNull SessionOperations sessionOperations) {
        this.dataSource = dataSource;
        this.taskManager = taskManager;
        this.dataSourceFactory = dataSourceFactory;
        this.sessionOperations = sessionOperations;
    }

    @Override
    public <T> Future<T> execute(StatementCallback<T> statementCallback) {
        return this.taskManager.submit(new StmtCallBackBasedTask<>(this.dataSource,
                this.dataSourceFactory, statementCallback, this.sessionOperations));
    }

    @Override
    public <T> Future<T> execute(PreparedStatementCreator creator,
            PreparedStatementCallback<T> statementCallback) {
        return this.taskManager.submit(new PreparedStmtCallBackBasedTask<>(this.dataSource,
                this.dataSourceFactory, creator, statementCallback, this.sessionOperations));
    }

    @Override
    public <T> Future<T> execute(ConnectionCallback<T> action) {
        return this.taskManager.submit(new ConnectionCallBackBasedTask<>(this.dataSource,
                this.dataSourceFactory, action, this.sessionOperations));
    }

}
