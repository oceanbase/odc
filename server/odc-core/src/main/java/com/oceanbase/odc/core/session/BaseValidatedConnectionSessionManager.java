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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.task.TaskManager;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The user's {@link ConnectionSession} needs {@code ODC} to manage the life cycle by itself, and
 * the {@link ConnectionSessionManager} is used for the life cycle management of the connection
 * session
 *
 * @author yh263208
 * @date 2021-11-15 21:12
 * @since ODC_release_3.2.2
 * @see BaseConnectionSessionManager
 */
@Slf4j
public abstract class BaseValidatedConnectionSessionManager extends BaseConnectionSessionManager
        implements ValidatedConnectionSessionManager, AutoCloseable {

    public static final long SESSION_MANAGER_SCAN_INTERVAL_MILLIS = 10000;
    private final TaskManager taskManager;
    @Getter
    private long scanIntervalMillis = SESSION_MANAGER_SCAN_INTERVAL_MILLIS;
    private volatile boolean asynTaskHasBeenStarted = false;
    private final List<Predicate<ConnectionSession>> validators = new LinkedList<>();

    public BaseValidatedConnectionSessionManager(@NonNull TaskManager taskManager) {
        super();
        this.taskManager = taskManager;
    }

    public void addSessionValidator(@NonNull Predicate<ConnectionSession> predicate) {
        this.validators.add(predicate);
    }

    public synchronized void enableAsyncRefreshSessionManager() {
        if (!asynTaskHasBeenStarted) {
            asynTaskHasBeenStarted = true;
            taskManager.submit(new ConnectionSessionValidateTask(this, this.validators, scanIntervalMillis));
        }
    }

    public void setScanInterval(long interval, @NonNull TimeUnit timeUnit) {
        Validate.isTrue(interval > 0, "Interval can not be negative");
        this.scanIntervalMillis = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
    }

    @Override
    protected ConnectionSession doGetSession(String key) {
        ConnectionSession session = retrieveSession(key);
        if (session == null) {
            return null;
        }
        if (this.validators.stream().anyMatch(p -> !p.test(session))) {
            log.info("Connection session verification failed, session={}", session);
            return null;
        }
        return session;
    }

    @Override
    public void removeSession(ConnectionSession session) throws Exception {
        try {
            doRemoveSession(session);
            try {
                onDeleteSucceed(session);
            } catch (Throwable throwable) {
                log.warn("Failed to execute call back method", throwable);
            }
        } catch (Throwable e) {
            log.warn("Failed to remove a session, session={}", session, e);
            try {
                onDeleteFailed(session.getId(), e);
            } catch (Throwable throwable) {
                log.warn("Failed to execute call back method", throwable);
            }
            throw new Exception(e);
        }
    }

    @Override
    public Collection<ConnectionSession> retrieveAllSessions() {
        Collection<ConnectionSession> sessions = doRetrieveAllSessions();
        if (sessions == null) {
            return Collections.emptyList();
        }
        BaseConnectionSessionManager that = this;
        return sessions.stream().map(session -> new DelegateConnectionSession(that, session))
                .collect(Collectors.toList());
    }

    abstract protected ConnectionSession retrieveSession(String key);

    abstract protected Collection<ConnectionSession> doRetrieveAllSessions();

    abstract protected void doRemoveSession(ConnectionSession session);

    @Override
    public void close() {
        try {
            this.taskManager.close();
        } catch (Exception e) {
            log.warn("Failed to close task manager", e);
        }
        Collection<ConnectionSession> sessions = retrieveAllSessions();
        for (ConnectionSession session : sessions) {
            try {
                session.expire();
            } catch (Exception e) {
                log.warn("Failed expire a session, sessionId={}", session.getId(), e);
            }
        }
    }

}
