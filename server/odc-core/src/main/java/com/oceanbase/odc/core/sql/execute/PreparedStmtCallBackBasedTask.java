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
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;

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
public class PreparedStmtCallBackBasedTask<T> extends BaseSqlExecuteCallable<T> {

    private final PreparedStatementCallback<T> statementCallback;
    private final PreparedStatementCreator creator;

    public PreparedStmtCallBackBasedTask(@NonNull DataSource dataSource,
            @NonNull CloneableDataSourceFactory dataSourceFactory,
            @NonNull PreparedStatementCreator creator,
            @NonNull PreparedStatementCallback<T> statementCallback,
            @NonNull SessionOperations sessionOperations) {
        super(dataSource, dataSourceFactory, sessionOperations);
        this.statementCallback = statementCallback;
        this.creator = creator;
    }

    @Override
    public T doCall(Connection connection) throws Exception {
        try (PreparedStatement statement = this.creator.createPreparedStatement(connection)) {
            return this.statementCallback.doInPreparedStatement(statement);
        }
    }

}

