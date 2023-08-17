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
import java.util.Collections;
import java.util.Map;

import com.oceanbase.odc.core.authority.session.EmptySecuritySessionEventListener;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionEventListener;
import com.oceanbase.odc.core.authority.session.SecuritySessionRepository;
import com.oceanbase.odc.core.authority.session.factory.SecuritySessionFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a default session manager that provides basic session management capabilities
 *
 * @author yh263208
 * @date 2021-07-15 15:29
 * @since ODC_release_3.2.0
 */
@Slf4j
public class DefaultSecuritySessionManager extends BaseValidatedSecuritySessionManager {
    /**
     * Default session event listener, Usedd to monitor the occurrence of session events
     *
     * @author yh263208
     * @date 2021-07-15 15:37
     * @since ODC_release_3.2.0
     * @see SecuritySessionEventListener
     */
    protected static class RemoveSecuritySessionEventListener extends EmptySecuritySessionEventListener {

        private final DefaultSecuritySessionManager sessionManager;

        public RemoveSecuritySessionEventListener(@NonNull DefaultSecuritySessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        @Override
        public void onExpiredEventSucceed(Serializable id, SecuritySession session) {
            try {
                this.sessionManager.removeCertainSession(session);
            } catch (Throwable e) {
                log.warn("Failed to delete an expired Session, sessionId={}", id, e);
            }
        }

        @Override
        public void onExpiredEventFailed(Serializable id, SecuritySession session, Throwable e) {
            log.warn("Failed to expire a session, sessionId={}", id, e);
        }
    }

    private final SecuritySessionRepository repository;

    public DefaultSecuritySessionManager(SecuritySessionFactory sessionFactory,
            @NonNull SecuritySessionRepository repository) {
        super(sessionFactory);
        this.repository = repository;
        addListener(new RemoveSecuritySessionEventListener(this));
    }

    @Override
    protected SecuritySession retrieveSession(Serializable key) {
        return this.repository.get(key);
    }

    @Override
    protected void doRemoveCertainSession(SecuritySession session) {
        this.repository.delete(session);
    }

    @Override
    protected void doStoreSession(SecuritySession session, Map<String, Object> context) {
        this.repository.store(session);
    }

    @Override
    protected Serializable getSessionId(Serializable key) {
        return key;
    }

    @Override
    public Collection<SecuritySession> retrieveAllSessions() {
        Collection<SecuritySession> sessions = this.repository.getAllSessions();
        return sessions == null ? Collections.emptyList() : sessions;
    }

}
