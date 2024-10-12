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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.core.task.TaskManager;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Default connection session manager
 *
 * @author yh263208
 * @date 2021-11-15 22:09
 * @since ODC_release_3.2.2
 * @see BaseValidatedConnectionSessionManager
 */
@Slf4j
public class DefaultConnectionSessionManager extends BaseValidatedConnectionSessionManager {
    /**
     * Data Access Object for {@link ConnectionSession}, provide us the basic capability to query,
     * delete, update and insert {@link ConnectionSession} object
     */
    private final ConnectionSessionRepository repository;
    private final DelayQueue<DelayDeleteTaskAction> delayDeleteQueue = new DelayQueue<>();
    private final Map<String, ConnectionSession> delayDeleteSessionId2Session = new ConcurrentHashMap<>();

    /**
     * Default session event listener, Usedd to monitor the occurrence of session events
     *
     * @author yh263208
     * @date 2021-07-15 15:37
     * @since ODC_release_3.2.2
     * @see DefaultSessionEventListener
     */
    static class InnerSessionEventListener extends DefaultSessionEventListener {
        private final DefaultConnectionSessionManager sessionManager;

        public InnerSessionEventListener(@NonNull DefaultConnectionSessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        @Override
        public void onExpireSucceed(ConnectionSession session) {
            try {
                this.sessionManager.removeSession(session);
            } catch (Throwable e) {
                log.warn("Fail to delete an expired session, sessionId={}", session.getId(), e);
            }
        }

        @Override
        public void onExpireFailed(ConnectionSession session, Throwable e) {
            log.warn("Failed to expire a session, sessionId={}", session.getId(), e);
        }

    }

    public DefaultConnectionSessionManager(@NonNull TaskManager taskManager,
            @NonNull ConnectionSessionRepository repository) {
        super(taskManager);
        this.repository = repository;
        addListener(new InnerSessionEventListener(this));
        DelayDeleteTask deleteTask = new DelayDeleteTask(delayDeleteQueue);
        taskManager.submit(deleteTask);
    }

    public boolean expire(@NonNull ConnectionSession connectionSession, long delay, @NonNull TimeUnit timeUnit) {
        if (this.delayDeleteSessionId2Session.containsKey(connectionSession.getId())) {
            return false;
        }
        this.delayDeleteSessionId2Session.putIfAbsent(connectionSession.getId(), connectionSession);
        DelayDeleteTaskAction action =
                new DelayDeleteTaskAction(delay, timeUnit, connectionSession.getId(), delayDeleteSessionId2Session);
        boolean result = delayDeleteQueue.offer(action);
        log.info("Session delayed delete task added, sessionId={}, EET={}, operateResult={}",
                connectionSession.getId(), new Date(action.expiredTime), result);
        return result;
    }

    public ConnectionSession cancelExpire(@NonNull ConnectionSession session) {
        if (!this.delayDeleteSessionId2Session.containsKey(session.getId())) {
            return null;
        }
        log.info("Session delayed deletion canceled, sessionId={}", session.getId());
        return this.delayDeleteSessionId2Session.remove(session.getId());
    }

    @Override
    protected ConnectionSession retrieveSession(String key) {
        return this.repository.get(key);
    }

    @Override
    protected Collection<ConnectionSession> doRetrieveAllSessions() {
        return this.repository.listAllSessions();
    }

    @Override
    protected void doRemoveSession(ConnectionSession session) {
        this.repository.delete(session);
    }

    @Override
    protected void doStoreSession(ConnectionSession session) {
        this.repository.store(session);
    }

    public Integer getActiveSessionCount() {
        return repository.listAllSessions().size();
    }


    /**
     * {@link DelayDeleteTask}
     *
     * @author yh263208
     * @date 2021-11-30 16:29
     * @since ODC_release_3.2.2
     */
    @Slf4j
    static class DelayDeleteTask implements Runnable {

        private final DelayQueue<DelayDeleteTaskAction> delayQueue;

        public DelayDeleteTask(@NonNull DelayQueue<DelayDeleteTaskAction> delayQueue) {
            this.delayQueue = delayQueue;
        }

        @Override
        public void run() {
            log.info("Connection session delayed deletion task started successfully");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    this.delayQueue.take().doAction();
                } catch (InterruptedException e) {
                    log.warn("Delay deletion daemon thread is interrupted, task exits", e);
                    return;
                } catch (Exception e) {
                    log.warn("Delay deletion daemon thread is abnormal, task is restarted", e);
                }
            }
        }
    }

    /**
     * {@link DelayDeleteTaskAction}
     *
     * @author yh263208
     * @date 2021-11-30 16:59
     * @since ODC_release_3.2.2
     */
    @Getter
    static class DelayDeleteTaskAction implements Delayed {
        private final String sessionId;
        private final long expiredTime;
        private final Map<String, ConnectionSession> delayDeleteSessionId2Session;

        public DelayDeleteTaskAction(long timeout, TimeUnit timeUnit, @NonNull String sessionId,
                @NonNull Map<String, ConnectionSession> delayDeleteSessionId2Session) {
            this.sessionId = sessionId;
            this.expiredTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
            this.delayDeleteSessionId2Session = delayDeleteSessionId2Session;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expiredTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (!(o instanceof DelayDeleteTaskAction)) {
                return 1;
            }
            return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
        }

        public void doAction() {
            ConnectionSession session = this.delayDeleteSessionId2Session.get(this.sessionId);
            if (session == null) {
                return;
            } else {
                this.delayDeleteSessionId2Session.remove(this.sessionId);
            }
            session.expire();
            log.info("Delayed deletion of connection session successfully, sessionId={}, AET={}", this.sessionId,
                    new Date());
        }
    }

}
