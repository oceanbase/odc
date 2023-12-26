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
package com.oceanbase.odc.core.sql.execute.tool;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.StatementCallback;

import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.ExpiredSessionException;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.AsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.FutureResult;
import com.oceanbase.odc.core.sql.execute.GeneralSyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;

import lombok.NonNull;

public class TestConnectionSession implements ConnectionSession {

    private Date lastAccessTime = new Date();
    private boolean expired;
    private final Date startTime = new Date();
    private final Boolean autoCommit;
    private final ConnectType connectType;
    private final Map<Object, Object> map = new HashMap<>();

    public TestConnectionSession(@NonNull Boolean autoCommit, @NonNull ConnectType connectType) {
        this.autoCommit = autoCommit;
        this.connectType = connectType;
        ConnectionSessionUtil.setBinaryDataManager(this, new TestBinaryDataManager());
    }

    @Override
    public String getId() {
        return 1000 + "";
    }

    @Override
    public void register(@NonNull String name,
            @NonNull DataSourceFactory dataSourceFactory) {

    }

    @Override
    public ConnectType getConnectType() {
        return this.connectType;
    }

    @Override
    public DialectType getDialectType() {
        return this.connectType.getDialectType();
    }

    @Override
    public boolean getDefaultAutoCommit() {
        return this.autoCommit;
    }

    @Override
    public Date getStartTime() {
        return this.startTime;
    }

    @Override
    public Date getLastAccessTime() {
        return this.lastAccessTime;
    }

    @Override
    public boolean isExpired() {
        return this.expired;
    }

    @Override
    public void expire() {
        this.expired = true;
    }

    @Override
    public void touch() throws ExpiredSessionException {
        this.lastAccessTime = new Date();
    }

    @Override
    public long getTimeoutMillis() {
        return TimeUnit.MILLISECONDS.convert(8, TimeUnit.HOURS);
    }

    @Override
    public Collection<Object> getAttributeKeys() throws ExpiredSessionException {
        return map.keySet();
    }

    @Override
    public Object getAttribute(Object key) throws ExpiredSessionException {
        return map.get(key);
    }

    @Override
    public void setAttribute(Object key, Object value) throws ExpiredSessionException {
        map.put(key, value);
    }

    @Override
    public Object removeAttribute(Object key) throws ExpiredSessionException {
        return map.put(key, null);
    }

    @Override
    public SyncJdbcExecutor getSyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        TestDataSourceFactory factory = new TestDataSourceFactory(this.connectType.getDialectType());
        return new GeneralSyncJdbcExecutor(factory.getDataSource());
    }

    @Override
    public AsyncJdbcExecutor getAsyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        TestDataSourceFactory factory = new TestDataSourceFactory(this.connectType.getDialectType());
        factory.setAutoCommit(autoCommit);
        return new EmptyAsyncJdbcExecutor(factory);
    }

    public static class EmptyAsyncJdbcExecutor implements AsyncJdbcExecutor {

        private JdbcTemplate jdbcTemplate;
        private final DataSourceFactory dataSourceFactory;

        public EmptyAsyncJdbcExecutor(@NonNull DataSourceFactory dataSourceFactory) {
            this.dataSourceFactory = dataSourceFactory;
        }

        @Override
        public <T> Future<T> execute(StatementCallback<T> statementCallback) {
            return FutureResult.successResult(getJdbcTemplate().execute(statementCallback));
        }

        @Override
        public <T> Future<T> execute(PreparedStatementCreator creator, PreparedStatementCallback<T> statementCallback) {
            return FutureResult.successResult(getJdbcTemplate().execute(creator, statementCallback));
        }

        @Override
        public <T> Future<T> execute(ConnectionCallback<T> action) {
            return FutureResult.successResult(getJdbcTemplate().execute(action));
        }

        synchronized JdbcTemplate getJdbcTemplate() {
            if (jdbcTemplate != null) {
                return jdbcTemplate;
            }
            jdbcTemplate = new JdbcTemplate(dataSourceFactory.getDataSource());
            return jdbcTemplate;
        }
    }

}
