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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionValidateManager;
import com.oceanbase.odc.core.authority.session.factory.SecuritySessionFactory;
import com.oceanbase.odc.core.authority.session.validate.SecuritySessionValidateTask;
import com.oceanbase.odc.core.authority.util.SecurityConstants;
import com.oceanbase.odc.core.task.DefaultTaskManager;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * A session manager with verification function, the manager will verify all sessions, and the
 * specific verification logic is encapsulated in the {@link Predicate< SecuritySession >}
 *
 * @author yh263208
 * @date 2021-07-15 11:35
 * @see BaseSecuritySessionManager
 * @see SecuritySessionValidateManager
 * @since ODC_release_3.2.0
 */
@Slf4j
public abstract class BaseValidatedSecuritySessionManager extends BaseSecuritySessionManager
        implements SecuritySessionValidateManager, AutoCloseable {
    /**
     * Thread pool for validation task
     */
    private final DefaultTaskManager taskManager;
    private final List<Predicate<SecuritySession>> validators = new LinkedList<>();
    private volatile boolean asynTaskHasBeenStarted = false;
    private long sessionScanIntervalMillis = SecurityConstants.DEFAULT_SCAN_INTERVAL_MILLIS;

    public BaseValidatedSecuritySessionManager(SecuritySessionFactory sessionFactory) {
        super(sessionFactory);
        this.taskManager = new DefaultTaskManager("security-session-manager-validate");
    }

    public synchronized void enableAsyncRefreshSessionManager() {
        Validate.notEmpty(this.validators, "Validators can not be empty");
        if (asynTaskHasBeenStarted) {
            return;
        }
        asynTaskHasBeenStarted = true;
        this.taskManager.submit(new SecuritySessionValidateTask(this, validators, sessionScanIntervalMillis));
    }

    public void addSessionValidator(@NonNull Predicate<SecuritySession> validator) {
        this.validators.add(validator);
    }

    public void setScanInterval(long interval, @NonNull TimeUnit timeUnit) {
        Validate.isTrue(interval > 0, "Interval can not be negative");
        this.sessionScanIntervalMillis = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
    }

    @Override
    protected SecuritySession doGetSession(Serializable key) throws InvalidSessionException {
        SecuritySession session = retrieveSession(key);
        if (session == null) {
            return null;
        }
        if (this.validators.stream().anyMatch(p -> !p.test(session))) {
            return null;
        }
        return session;
    }

    protected abstract SecuritySession retrieveSession(Serializable key) throws InvalidSessionException;

    @Override
    public void removeCertainSession(@NonNull SecuritySession session) {
        try {
            doRemoveCertainSession(session);
            onDeleteSucceed(session.getId(), session);
        } catch (Throwable e) {
            onDeleteFailed(session.getId(), e);
            throw new IllegalStateException(e);
        }
    }

    protected abstract void doRemoveCertainSession(SecuritySession session);

    @Override
    public void close() {
        try {
            this.taskManager.close();
        } catch (Exception e) {
            log.warn("Failed to close task manager", e);
        }
        Collection<SecuritySession> sessions = retrieveAllSessions();
        for (SecuritySession session : sessions) {
            try {
                session.expire();
            } catch (Exception e) {
                log.warn("Failed expire a session, sessionId={}", session.getId(), e);
            }
        }
    }

}
