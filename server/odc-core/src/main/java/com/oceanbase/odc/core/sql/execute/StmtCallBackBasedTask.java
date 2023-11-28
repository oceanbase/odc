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
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Asynchronous SQL execution task based on statement callback
 *
 * @author yh263208
 * @date 2021-11-13 00:07
 * @since ODC_release_3.2.2
 */
@Slf4j
public class StmtCallBackBasedTask<T> extends BaseSqlExecuteCallable<T> {

    private final StatementCallback<T> statementCallback;

    public StmtCallBackBasedTask(@NonNull DataSource dataSource,
            @NonNull CloneableDataSourceFactory dataSourceFactory,
            @NonNull StatementCallback<T> statementCallback,
            @NonNull SessionOperations sessionOperations) {
        super(dataSource, dataSourceFactory, sessionOperations);
        this.statementCallback = statementCallback;
    }

    @Override
    protected T doCall(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            return this.statementCallback.doInStatement(statement);
        }
    }

}

