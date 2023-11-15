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
package com.oceanbase.odc.core.session.tool;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.DefaultConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.TestSessionOperations;
import com.oceanbase.odc.core.sql.execute.task.DefaultSqlExecuteTaskManager;
import com.oceanbase.odc.core.sql.execute.tool.TestDataSourceFactory;

import lombok.NonNull;
import lombok.Setter;

public class TestConnectionSessionFactory implements ConnectionSessionFactory {

    private static final AtomicLong sessionIdCounter = new AtomicLong(10000);
    private final Boolean autoCommit;
    private final ConnectType connectType;
    @Setter
    private long timeoutMillis;

    public TestConnectionSessionFactory(@NonNull ConnectType connectType,
            @NonNull Boolean autoCommit) {
        this.connectType = connectType;
        this.autoCommit = autoCommit;
        this.timeoutMillis =
                TimeUnit.MILLISECONDS.convert(ConnectionSessionConstants.SESSION_EXPIRATION_TIME_SECONDS,
                        TimeUnit.SECONDS);
    }

    @Override
    public ConnectionSession generateSession() {
        try {
            DataSourceFactory dataSourceFactory = new TestDataSourceFactory(connectType.getDialectType());
            ConnectionSession session = new DefaultConnectionSession(
                    () -> sessionIdCounter.incrementAndGet() + "",
                    () -> new DefaultSqlExecuteTaskManager(3, "sqlexecutor"),
                    timeoutMillis, connectType, autoCommit, new TestSessionOperations());
            session.register(ConnectionSessionConstants.CONSOLE_DS_KEY,
                    new TestDataSourceFactory(connectType.getDialectType()));
            session.register(ConnectionSessionConstants.SYS_DS_KEY, dataSourceFactory);
            session.register(ConnectionSessionConstants.BACKEND_DS_KEY, dataSourceFactory);
            session.setAttribute(ConnectionSessionConstants.OB_VERSION, "3.2.4");
            if (session.getDialectType() == DialectType.OB_ORACLE) {
                ConnectionSessionUtil.initConsoleSessionTimeZone(session, "Asia/Shanghai");
            }
            return session;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
