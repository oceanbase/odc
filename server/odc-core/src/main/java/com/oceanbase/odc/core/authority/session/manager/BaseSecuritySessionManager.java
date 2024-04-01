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
package com.oceanbase.odc.core.authority.session.manager;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.session.DelegateSecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionEventListener;
import com.oceanbase.odc.core.authority.session.SecuritySessionManager;
import com.oceanbase.odc.core.authority.session.factory.SecuritySessionFactory;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract implementation of session manager, used to encapsulate some general capabilities of
 * session management
 *
 * @author yh263208
 * @date 2021-07-14 17:54
 * @see SecuritySessionManager
 * @since ODC_release_3.2.0
 */
@Slf4j
public abstract class BaseSecuritySessionManager implements SecuritySessionManager {

    @Getter
    private long defaultSessionTimeoutMillis = SecurityConstants.DEFAULT_SESSION_TIMEOUT_MILLIS;
    private final SecuritySessionFactory sessionFactory;
    private final List<SecuritySessionEventListener> listeners = new LinkedList<>();

    public BaseSecuritySessionManager(@NonNull SecuritySessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public SecuritySession start(Map<String, Object> context) {
        return new DelegateSecuritySession(this, doStartSession(context));
    }

    /**
     * Create a {@link SecuritySession} object method
     *
     * @param session {@link SecuritySession} that will be created
     * @param context context for {@link SecuritySession} create
     */
    protected abstract void doStoreSession(SecuritySession session, Map<String, Object> context);

    protected SecuritySession doStartSession(Map<String, Object> context) {
        if (context == null) {
            context = new HashMap<>();
        }
        context.computeIfAbsent(SecurityConstants.CONTEXT_SESSION_TIMEOUT_KEY, s -> getDefaultSessionTimeoutMillis());
        SecuritySession session = this.sessionFactory.createSession(context);
        try {
            doStoreSession(session, context);
        } catch (Throwable e) {
            onCreateFailed(session, context, e);
            return null;
        }
        onCreateSucceed(session, context);
        return session;
    }

    /**
     * Get a session from the session manager
     *
     * @param key Keyword used to store session objects
     * @return session object
     */
    @Override
    public SecuritySession getSession(Serializable key) {
        SecuritySession session;
        try {
            Serializable tempKey = getSessionId(key);
            session = doGetSession(tempKey);
            if (session == null) {
                return null;
            }
            session.touch();
        } catch (Throwable e) {
            onGetFailed(key, e);
            return null;
        }
        onGetSucceed(key, session);
        return new DelegateSecuritySession(this, session);
    }

    protected abstract Serializable getSessionId(Serializable key);

    /**
     * Get a {@link SecuritySession} object method
     *
     * @param key {@link SecuritySession} key
     * @return {@link SecuritySession}
     */
    protected abstract SecuritySession doGetSession(Serializable key) throws InvalidSessionException;

    public void setSessionTimeout(long timeout, @NonNull TimeUnit timeUnit) {
        Validate.isTrue(timeout > 0, "Timeout can not be negative");
        this.defaultSessionTimeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
    }

    public Serializable getId(@NonNull SecuritySession session) {
        return session.getId();
    }

    public Date getStartTime(@NonNull SecuritySession session) {
        return session.getStartTime();
    }

    public Date getLastAccessTime(@NonNull SecuritySession session) {
        return session.getLastAccessTime();
    }

    public long getTimeoutMillis(@NonNull SecuritySession session) {
        return session.getTimeoutMillis();
    }

    public String getHost(@NonNull SecuritySession session) {
        return session.getHost();
    }

    public void touch(@NonNull SecuritySession session) throws InvalidSessionException {
        session.touch();
    }

    public void expire(@NonNull SecuritySession session) {
        try {
            session.expire();
            onExpiredSucceed(session.getId(), session);
        } catch (Throwable e) {
            onExpiredFailed(session.getId(), session, e);
        }
    }

    public boolean isExpired(SecuritySession session) {
        return session.isExpired();
    }

    public Collection<Object> getAttributeKeys(@NonNull SecuritySession session) throws InvalidSessionException {
        return session.getAttributeKeys();
    }

    public Object getAttribute(@NonNull SecuritySession session, Object key) throws InvalidSessionException {
        return session.getAttribute(key);
    }

    public void setAttribute(@NonNull SecuritySession session, Object key, Object value)
            throws InvalidSessionException {
        session.setAttribute(key, value);
    }

    public Object removeAttribute(@NonNull SecuritySession session, Object key) throws InvalidSessionException {
        return session.removeAttribute(key);
    }

    public void addListener(@NonNull SecuritySessionEventListener listener) {
        this.listeners.add(listener);
    }

    protected void onCreateSucceed(@NonNull SecuritySession session, Map<String, Object> context) {
        forEachListener(listener -> listener.onCreateEventSucceed(session, context));
    }

    protected void onDeleteSucceed(Serializable id, @NonNull SecuritySession session) {
        forEachListener(listener -> listener.onDeleteEventSucceed(id, session));
    }

    protected void onUpdateSucceed(Serializable id, @NonNull SecuritySession session) {
        forEachListener(listener -> listener.onUpdateEventSucceed(id, session));
    }

    protected void onGetSucceed(Serializable id, @NonNull SecuritySession session) {
        forEachListener(listener -> listener.onGetEventSucceed(id, session));
    }

    protected void onExpiredSucceed(Serializable id, @NonNull SecuritySession session) {
        forEachListener(listener -> listener.onExpiredEventSucceed(id, session));
    }

    protected void onCreateFailed(@NonNull SecuritySession session, Map<String, Object> context, Throwable e) {
        forEachListener(listener -> listener.onCreateEventFailed(session, context, e));
    }

    protected void onDeleteFailed(Serializable id, Throwable e) {
        forEachListener(listener -> listener.onDeleteEventFailed(id, e));
    }

    protected void onUpdateFailed(Serializable id, @NonNull SecuritySession session, Throwable e) {
        forEachListener(listener -> listener.onUpdateEventFailed(id, session, e));
    }

    protected void onGetFailed(Serializable id, Throwable e) {
        forEachListener(listener -> listener.onGetEventFailed(id, e));
    }

    protected void onExpiredFailed(Serializable id, @NonNull SecuritySession session, Throwable e) {
        forEachListener(listener -> listener.onExpiredEventFailed(id, session, e));
    }

    private void forEachListener(Consumer<SecuritySessionEventListener> consumer) {
        if (consumer == null) {
            return;
        }
        for (SecuritySessionEventListener listener : this.listeners) {
            try {
                consumer.accept(listener);
            } catch (Throwable e) {
                StackTraceElement element = Thread.currentThread().getStackTrace()[2];
                log.warn("Failed to call listener {}#{}, error={}", listener.getClass().getName(),
                        element.getMethodName(), ExceptionUtils.getRootCauseReason(e));
            }
        }
    }

}
