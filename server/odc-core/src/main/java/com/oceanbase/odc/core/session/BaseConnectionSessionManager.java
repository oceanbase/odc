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

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.oceanbase.odc.core.datasource.DataSourceFactory;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.AsyncJdbcExecutor;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The basic {@link ConnectionSession} manager provides basic management capabilities for
 * {@link ConnectionSession}, and all implementations are best based on this
 *
 * @author yh263208
 * @date 2021-11-15 17:10
 * @since ODC_release_3.2.2
 */
@Slf4j
public abstract class BaseConnectionSessionManager implements ConnectionSessionManager {

    private final List<ConnectionSessionEventListener> listeners = new LinkedList<>();

    /**
     * Start a session and use a keyword to persist the session into a buffer
     *
     * @param factory {@code ConnectionSessionFactory}
     * @return session object
     */
    @Override
    public ConnectionSession start(ConnectionSessionFactory factory) {
        ConnectionSession session = factory.generateSession();
        try {
            doStoreSession(session);
        } catch (Throwable e) {
            try {
                session.expire();
            } catch (Exception e1) {
                log.warn("Failed to expire session, session storage failed, sessId={}", session.getId(), e1);
            }
            log.warn("Failed to store a session, session={}", session, e);
            try {
                onCreateFailed(session, e);
            } catch (Throwable throwable) {
                log.warn("Failed to execute call back method", throwable);
            }
            return null;
        }
        try {
            onCreateSucceed(session);
        } catch (Throwable throwable) {
            log.warn("Failed to execute call back method", throwable);
        }
        return new DelegateConnectionSession(this, session);
    }

    /**
     * Get a session from the session manager
     *
     * @param id Keyword used to store session objects
     * @return session object
     */
    @Override
    public ConnectionSession getSession(String id) {
        ConnectionSession session = null;
        try {
            session = doGetSession(id);
            if (session == null) {
                return null;
            }
            session.touch();
        } catch (Throwable e) {
            log.warn("Failed to get a session, session={}", session, e);
            try {
                onGetFailed(id, e);
            } catch (Throwable throwable) {
                log.warn("Failed to execute call back method", throwable);
            }
            return null;
        }
        try {
            onGetSucceed(session);
        } catch (Throwable throwable) {
            log.warn("Failed to execute call back method", throwable);
        }
        return new DelegateConnectionSession(this, session);
    }

    /**
     * After the {@link ConnectionSession} is successfully created, some follow-up operations are
     * required, such as storing the {@link ConnectionSession}, etc., through the implementation of this
     * method to complete these business settings
     *
     * @param session Created {@link ConnectionSession}
     */
    abstract protected void doStoreSession(ConnectionSession session);

    /**
     * The abstract base class {@link BaseConnectionSessionManager} cannot undertake the complete
     * acquisition of the {@link ConnectionSession} logic, and must be implemented by subclasses
     *
     * @param id if for {@link ConnectionSession}
     * @return Getted {@link ConnectionSession}
     */
    abstract protected ConnectionSession doGetSession(String id);

    protected String getId(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getId();
    }

    protected ConnectType getConnectType(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getConnectType();
    }

    protected DialectType getDialectType(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getDialectType();
    }

    protected boolean getDefaultAutoCommit(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getDefaultAutoCommit();
    }

    protected Date getStartTime(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getStartTime();
    }

    protected Date getLastAccessTime(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getLastAccessTime();
    }

    protected boolean isExpired(@NonNull ConnectionSession connectionSession) {
        return connectionSession.isExpired();
    }

    protected void expire(@NonNull ConnectionSession connectionSession) {
        try {
            onExpire(connectionSession);
            connectionSession.expire();
            onExpireSucceed(connectionSession);
        } catch (Exception e) {
            log.warn("Failed to expire a session, session={}", connectionSession, e);
            onExpireFailed(connectionSession, e);
            throw e;
        }
    }

    protected void touch(@NonNull ConnectionSession connectionSession) throws ExpiredSessionException {
        connectionSession.touch();
    }

    protected long getTimeoutMillis(@NonNull ConnectionSession connectionSession) {
        return connectionSession.getTimeoutMillis();
    }

    protected Collection<Object> getAttributeKeys(@NonNull ConnectionSession connectionSession)
            throws ExpiredSessionException {
        return connectionSession.getAttributeKeys();
    }

    protected Object getAttribute(@NonNull ConnectionSession connectionSession, Object key)
            throws ExpiredSessionException {
        return connectionSession.getAttribute(key);
    }

    protected void setAttribute(@NonNull ConnectionSession connectionSession, Object key, Object value)
            throws ExpiredSessionException {
        connectionSession.setAttribute(key, value);
    }

    protected Object removeAttribute(@NonNull ConnectionSession connectionSession, Object key)
            throws ExpiredSessionException {
        return connectionSession.removeAttribute(key);
    }

    protected void register(@NonNull ConnectionSession connectionSession,
            String name, DataSourceFactory dataSourceFactory) {
        connectionSession.register(name, dataSourceFactory);
    }

    protected SyncJdbcExecutor getSyncJdbcExecutor(@NonNull ConnectionSession connectionSession,
            String dataSourceName) throws ExpiredSessionException {
        return connectionSession.getSyncJdbcExecutor(dataSourceName);
    }

    protected AsyncJdbcExecutor getAsyncJdbcExecutor(@NonNull ConnectionSession connectionSession,
            String dataSourceName) throws ExpiredSessionException {
        return connectionSession.getAsyncJdbcExecutor(dataSourceName);
    }

    /**
     * Add a <code>SessionEventListener</code>
     *
     * @param listener listener that will by added
     */
    public void addListener(@NonNull ConnectionSessionEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeListenersIf(@NonNull Predicate<ConnectionSessionEventListener> predicate) {
        this.listeners.removeIf(predicate);
    }

    protected void onCreateSucceed(@NonNull ConnectionSession session) {
        forEachListener(eventListener -> eventListener.onCreateSucceed(session));
    }

    protected void onCreateFailed(@NonNull ConnectionSession session, @NonNull Throwable e) {
        forEachListener(eventListener -> eventListener.onCreateFailed(session, e));
    }

    protected void onDeleteSucceed(@NonNull ConnectionSession session) {
        forEachListener(eventListener -> eventListener.onDeleteSucceed(session));
    }

    protected void onDeleteFailed(@NonNull String id, @NonNull Throwable e) {
        forEachListener(eventListener -> eventListener.onDeleteFailed(id, e));
    }

    protected void onGetSucceed(@NonNull ConnectionSession session) {
        forEachListener(eventListener -> eventListener.onGetSucceed(session));
    }

    protected void onGetFailed(@NonNull String id, @NonNull Throwable e) {
        forEachListener(eventListener -> eventListener.onGetFailed(id, e));
    }

    protected void onExpire(@NonNull ConnectionSession session) {
        forEachListener(eventListener -> eventListener.onExpire(session));
    }

    protected void onExpireSucceed(@NonNull ConnectionSession session) {
        forEachListener(eventListener -> eventListener.onExpireSucceed(session));
    }

    protected void onExpireFailed(@NonNull ConnectionSession session, @NonNull Throwable e) {
        forEachListener(eventListener -> eventListener.onExpireFailed(session, e));
    }

    private void forEachListener(Consumer<ConnectionSessionEventListener> consumer) {
        if (consumer == null) {
            return;
        }
        for (ConnectionSessionEventListener listener : this.listeners) {
            try {
                consumer.accept(listener);
            } catch (Throwable e) {
                log.warn("Failed to call listener's method", e);
            }
        }
    }

}
