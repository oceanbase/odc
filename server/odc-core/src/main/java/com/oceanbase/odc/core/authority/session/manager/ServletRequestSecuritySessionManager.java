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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.session.EmptySecuritySessionEventListener;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionManager;
import com.oceanbase.odc.core.authority.session.factory.SecuritySessionFactory;
import com.oceanbase.odc.core.authority.session.validate.ExpiredSecuritySessionValidator;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

import lombok.NonNull;

/**
 * The {@link ServletRequestSecuritySessionManager} based on {@link ServletRequest} delegates the
 * {@link SecuritySession} management of the security framework to the {@link ServletRequest} of the
 * web layer to complete.
 *
 * The {@link ServletRequestSecuritySessionManager} itself does not perform any
 * {@link SecuritySession} management operations
 *
 * @author yh263208
 * @date 2021-07-16 15:38
 * @see SecuritySessionManager
 * @since ODC_release_3.2.0
 */
public class ServletRequestSecuritySessionManager extends BaseValidatedSecuritySessionManager
        implements ServletBaseSecuritySessionManager {

    private static final ThreadLocal<HttpSession> HTTP_SESSION_THREAD_LOCAL = new ThreadLocal<>();
    private final boolean persistHttpSession;
    private long minPersistenceIntervalSecs = 0;

    public ServletRequestSecuritySessionManager(boolean persistHttpSession, SecuritySessionFactory sessionFactory) {
        super(sessionFactory);
        addSessionValidator(new ExpiredSecuritySessionValidator());
        addListener(new EmptySecuritySessionEventListener());
        this.persistHttpSession = persistHttpSession;
    }

    public ServletRequestSecuritySessionManager(SecuritySessionFactory sessionFactory) {
        this(false, sessionFactory);
    }

    public void setMinPersistenceIntervalSecs(@NonNull long interval, @NonNull TimeUnit timeUnit) {
        this.minPersistenceIntervalSecs = TimeUnit.SECONDS.convert(interval, timeUnit);
    }

    public static void removeHttpSession() {
        HTTP_SESSION_THREAD_LOCAL.remove();
    }

    @Override
    protected void doStoreSession(SecuritySession session, Map<String, Object> context) {
        Object value = context.get(SecurityConstants.CONTEXT_SERVLET_REQUEST_KEY);
        if (!(value instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("HttpServletRequest is necessary");
        }
        HttpServletRequest request = (HttpServletRequest) value;
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) {
            httpSession = request.getSession(true);
        } else {
            httpSession.removeAttribute(SecurityConstants.HTTPSESSION_SECURITY_SESSION_KEY);
        }
        if (persistHttpSession) {
            setHttpSession(httpSession);
        }
        httpSession.setAttribute(SecurityConstants.HTTPSESSION_SECURITY_SESSION_KEY, session);
    }

    @Override
    protected Serializable getSessionId(Serializable key) {
        return key;
    }

    @Override
    @SuppressWarnings("all")
    protected SecuritySession retrieveSession(Serializable key) throws InvalidSessionException {
        if (!(key instanceof Map)) {
            throw new IllegalArgumentException("Context is necessary");
        }
        Map<String, Object> context = (Map<String, Object>) key;
        Object value = context.get(SecurityConstants.CONTEXT_SERVLET_REQUEST_KEY);
        if (!(value instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("HttpServletRequest is necessary");
        }
        HttpServletRequest request = (HttpServletRequest) value;
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) {
            return null;
        }
        value = httpSession.getAttribute(SecurityConstants.HTTPSESSION_SECURITY_SESSION_KEY);
        if (!(value instanceof SecuritySession)) {
            return null;
        }
        SecuritySession securitySession = (SecuritySession) value;
        if (persistHttpSession) {
            setHttpSession(httpSession);
            persistentSettings(securitySession, false);
        }
        return securitySession;
    }

    /**
     * Since the {@link ServletRequestSecuritySessionManager} delegates all session management
     * capabilities to {@link ServletRequest}
     *
     * It cannot provide the ability to delete sessions and enumerate all sessions
     */
    @Override
    public synchronized void enableAsyncRefreshSessionManager() {
        throw new UnsupportedOperationException("Not support");
    }

    /**
     * Since the {@link ServletRequestSecuritySessionManager} delegates all session management
     * capabilities to {@link ServletRequest}
     *
     * It cannot provide the ability to delete sessions and enumerate all sessions
     */
    @Override
    protected void doRemoveCertainSession(SecuritySession session) {
        throw new UnsupportedOperationException("Not support");
    }

    /**
     * Since the {@link ServletRequestSecuritySessionManager} delegates all session management
     * capabilities to {@link ServletRequest}
     *
     * It cannot provide the ability to delete sessions and enumerate all sessions
     */
    @Override
    public Collection<SecuritySession> retrieveAllSessions() {
        throw new UnsupportedOperationException("Not support");
    }

    /**
     * Start a {@link SecuritySession} by {@link ServletRequest}
     *
     * @param request {@link HttpServletRequest}
     * @return Created {@link SecuritySession}
     */
    @Override
    public SecuritySession start(@NonNull ServletRequest request, ServletResponse response,
            Map<String, Object> context) {
        if (context == null) {
            context = new HashMap<>();
        }
        context.put(SecurityConstants.CONTEXT_SESSION_HOST_KEY, request.getRemoteHost());
        context.put(SecurityConstants.CONTEXT_SERVLET_REQUEST_KEY, request);
        return start(context);
    }

    /**
     * Get a {@link SecuritySession} by {@link ServletRequest}
     *
     * @param request {@link HttpServletRequest}
     * @return {@link SecuritySession}
     */
    @Override
    public SecuritySession getSession(@NonNull ServletRequest request, ServletResponse response) {
        Map<String, Object> context = new HashMap<>();
        context.put(SecurityConstants.CONTEXT_SESSION_HOST_KEY, request.getRemoteHost());
        context.put(SecurityConstants.CONTEXT_SERVLET_REQUEST_KEY, request);
        return getSession((Serializable) context);
    }

    @Override
    public void touch(@NonNull SecuritySession session) throws InvalidSessionException {
        super.touch(session);
        persistentSettings(session, true);
    }

    @Override
    public void expire(@NonNull SecuritySession session) {
        try {
            session.expire();
            persistentSettings(session, true);
            onExpiredSucceed(session.getId(), session);
        } catch (Throwable e) {
            onExpiredFailed(session.getId(), session, e);
        }
    }

    @Override
    public void setAttribute(@NonNull SecuritySession session, Object key, Object value)
            throws InvalidSessionException {
        if (value instanceof HttpSession && persistHttpSession) {
            throw new IllegalArgumentException("Http session can't be set when a persistent session is opened");
        }
        super.setAttribute(session, key, value);
        persistentSettings(session, true);
    }

    @Override
    public Object removeAttribute(@NonNull SecuritySession session, Object key) throws InvalidSessionException {
        Object returnVal = super.removeAttribute(session, key);
        persistentSettings(session, true);
        return returnVal;
    }

    private void persistentSettings(SecuritySession session, boolean flush) throws InvalidSessionException {
        if (!this.persistHttpSession) {
            return;
        }
        if (!flush && getIntervalSecsFromNow(session.getLastAccessTime()) < minPersistenceIntervalSecs) {
            return;
        }
        HttpSession httpSession = HTTP_SESSION_THREAD_LOCAL.get();
        if (httpSession == null) {
            throw new IllegalStateException("Can not find http session");
        }
        session.touch();
        httpSession.setAttribute(SecurityConstants.HTTPSESSION_SECURITY_SESSION_KEY, session);
    }

    private long getIntervalSecsFromNow(Date date) {
        long current = System.currentTimeMillis();
        long target = date.getTime();
        if (target > current) {
            throw new IllegalArgumentException("Future time is not acceptable");
        }
        return TimeUnit.SECONDS.convert(current - target, TimeUnit.MILLISECONDS);
    }

    private static void setHttpSession(@NonNull HttpSession httpSession) {
        HTTP_SESSION_THREAD_LOCAL.remove();
        HTTP_SESSION_THREAD_LOCAL.set(httpSession);
    }

}
