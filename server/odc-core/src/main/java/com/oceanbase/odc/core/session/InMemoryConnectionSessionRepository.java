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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;

import lombok.NonNull;

/**
 * The data access object of the session, the data access object can be stored in a variety of media
 * {@link ConnectionSessionRepository} is a kind of implement which store {@link ConnectionSession}
 * in memory
 *
 * @author yh263208
 * @date 2021-11-15 17:04
 * @since ODC_release_3.2.2
 */
public class InMemoryConnectionSessionRepository implements ConnectionSessionRepository {
    /**
     * A container for holding {@link ConnectionSession} objects, implemented as a concurrent object
     * {@link ConcurrentHashMap}
     */
    private final Map<Serializable, ConnectionSession> sessionId2SessionMap = new ConcurrentHashMap<>();

    @Override
    public String store(@NonNull ConnectionSession session) {
        Validate.notNull(session.getId(), "Session.Id can not be null");
        if (this.sessionId2SessionMap.containsKey(session.getId())) {
            throw new IllegalStateException("Session id \"" + session.getId() + "\" already exists");
        }
        this.sessionId2SessionMap.putIfAbsent(session.getId(), session);
        return session.getId();
    }

    @Override
    public ConnectionSession get(@NonNull String sessionId) {
        return this.sessionId2SessionMap.get(sessionId);
    }

    @Override
    public void delete(@NonNull ConnectionSession session) {
        Validate.notNull(session.getId(), "Session.Id can not be null");
        this.sessionId2SessionMap.remove(session.getId());
    }

    @Override
    public Collection<ConnectionSession> listAllSessions() {
        return sessionId2SessionMap.values();
    }

}
