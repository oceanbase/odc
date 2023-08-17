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

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.AsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;

import lombok.NonNull;

/**
 * Delegate {@link ConnectionSession}
 *
 * @author yh263208
 * @date 2021-11-15 17:20
 * @since ODC_release_3.2.2
 * @see ConnectionSession
 */
class DelegateConnectionSession implements ConnectionSession {

    private final BaseConnectionSessionManager sessionManager;
    /**
     * Delgate <code>Session</code> instance
     */
    private final ConnectionSession delegate;

    /**
     * Default Constructor
     *
     * @param sessionManager manager for {@link ConnectionSession}
     * @param session {@link ConnectionSession} to be delgated
     */
    public DelegateConnectionSession(@NonNull BaseConnectionSessionManager sessionManager,
            @NonNull ConnectionSession session) {
        this.sessionManager = sessionManager;
        this.delegate = session;
    }

    @Override
    public String getId() {
        return this.sessionManager.getId(this.delegate);
    }

    @Override
    public void register(@NonNull String name, @NonNull DataSourceFactory dataSourceFactory) {
        this.sessionManager.register(this.delegate, name, dataSourceFactory);
    }

    @Override
    public ConnectType getConnectType() {
        return this.sessionManager.getConnectType(this.delegate);
    }

    @Override
    public DialectType getDialectType() {
        return this.sessionManager.getDialectType(this.delegate);
    }

    @Override
    public boolean getDefaultAutoCommit() {
        return this.sessionManager.getDefaultAutoCommit(this.delegate);
    }

    @Override
    public Date getStartTime() {
        return this.sessionManager.getStartTime(this.delegate);
    }

    @Override
    public Date getLastAccessTime() {
        return this.sessionManager.getLastAccessTime(this.delegate);
    }

    @Override
    public boolean isExpired() {
        return this.sessionManager.isExpired(this.delegate);
    }

    @Override
    public void expire() {
        this.sessionManager.expire(this.delegate);
    }

    @Override
    public void touch() throws ExpiredSessionException {
        this.sessionManager.touch(this.delegate);
    }

    @Override
    public long getTimeoutMillis() {
        return this.sessionManager.getTimeoutMillis(this.delegate);
    }

    @Override
    public Collection<Object> getAttributeKeys() throws ExpiredSessionException {
        return this.sessionManager.getAttributeKeys(this.delegate);
    }

    @Override
    public Object getAttribute(Object key) throws ExpiredSessionException {
        return this.sessionManager.getAttribute(this.delegate, key);
    }

    @Override
    public void setAttribute(Object key, Object value) throws ExpiredSessionException {
        this.sessionManager.setAttribute(this.delegate, key, value);
    }

    @Override
    public Object removeAttribute(Object key) throws ExpiredSessionException {
        return this.sessionManager.removeAttribute(this.delegate, key);
    }

    @Override
    public SyncJdbcExecutor getSyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        return this.sessionManager.getSyncJdbcExecutor(this.delegate, dataSourceName);
    }

    @Override
    public AsyncJdbcExecutor getAsyncJdbcExecutor(String dataSourceName) throws ExpiredSessionException {
        return this.sessionManager.getAsyncJdbcExecutor(this.delegate, dataSourceName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof DelegateConnectionSession) {
            DelegateConnectionSession other = (DelegateConnectionSession) obj;
            Serializable thisId = this.delegate.getId();
            Serializable otherId = other.delegate.getId();
            return thisId.equals(otherId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.delegate.getId().hashCode();
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

}
