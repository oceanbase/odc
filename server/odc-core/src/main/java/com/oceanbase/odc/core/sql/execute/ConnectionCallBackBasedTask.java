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

import javax.sql.DataSource;

import org.springframework.jdbc.core.ConnectionCallback;

import com.oceanbase.odc.core.datasource.CloneableDataSourceFactory;

import lombok.NonNull;

/**
 * {@link ConnectionCallBackBasedTask}
 *
 * @author yh263208
 * @date 2023-12-11 11:27
 * @since ODC_release_4.2.3
 */
public class ConnectionCallBackBasedTask<T> extends BaseSqlExecuteCallable<T> {

    private final ConnectionCallback<T> connectionCallback;

    public ConnectionCallBackBasedTask(@NonNull DataSource dataSource,
            @NonNull CloneableDataSourceFactory dataSourceFactory,
            @NonNull ConnectionCallback<T> connectionCallback,
            @NonNull SessionOperations sessionOperations) {
        super(dataSource, dataSourceFactory, sessionOperations);
        this.connectionCallback = connectionCallback;
    }

    @Override
    protected T doCall(Connection connection) throws Exception {
        return this.connectionCallback.doInConnection(connection);
    }

}
