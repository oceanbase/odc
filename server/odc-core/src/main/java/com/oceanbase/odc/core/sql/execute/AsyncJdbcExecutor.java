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

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.StatementCallback;

/**
 * Asynchronous process execution interface, mainly used for asynchronous execution logic, which can
 * cover asynchronous execution methods, asynchronous execution of SQL, etc.
 *
 * @author yh263208
 * @date 2021-11-12 20:17
 * @since ODC_release_3.2.2
 */
public interface AsyncJdbcExecutor {
    /**
     * Execute sql script asynchronously
     *
     * @param statementCallback call back method for {@link java.sql.Statement}
     * @return control handler
     */
    <T> Future<T> execute(StatementCallback<T> statementCallback);

    /**
     * Execute sql script asynchronously
     *
     * @param creator {@link java.sql.PreparedStatement} constructor
     * @param statementCallback call back method for {@link java.sql.PreparedStatement}
     * @return control handler
     */
    <T> Future<T> execute(PreparedStatementCreator creator, PreparedStatementCallback<T> statementCallback);

    /**
     * Execute sql script asynchronously
     *
     * @param action a callback object that specifies the action
     * @return control handler
     */
    <T> Future<T> execute(ConnectionCallback<T> action);

}

