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
package com.oceanbase.odc.core.session;

import java.io.IOException;

import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.AsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/9 14:17
 * @Description: []
 */
@Slf4j
public class LogicalConnectionSession extends AbstractConnectionSession {
    public LogicalConnectionSession(@NonNull String id, @NonNull ConnectType connectType, long sessionTimeoutMillis)
            throws IOException {
        super(id, connectType, sessionTimeoutMillis);
    }

    @Override
    protected void closeDataSource() {}

    @Override
    protected void closeTaskManager() {}

    @Override
    protected void closeBinaryFileManager() {}

    @Override
    public void register(@NonNull String name, @NonNull DataSourceFactory dataSourceFactory) {
        throw new UnsupportedException("Logical connection session does not support this operation");
    }

    @Override
    public boolean getDefaultAutoCommit() {
        throw new UnsupportedException("Logical connection session does not support this operation");
    }

    @Override
    public SyncJdbcExecutor getSyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        throw new UnsupportedException("Logical connection session does not support this operation");
    }

    @Override
    public AsyncJdbcExecutor getAsyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        throw new UnsupportedException("Logical connection session does not support this operation");
    }
}
