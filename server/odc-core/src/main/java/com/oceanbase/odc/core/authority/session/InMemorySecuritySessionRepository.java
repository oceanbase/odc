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
package com.oceanbase.odc.core.authority.session;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;

import lombok.NonNull;

/**
 * The data access object of the session, the data access object can be stored in a variety of media
 * {@code InMemeorySessionDAO} is a kind of implement which store {@link SecuritySession} in memory
 *
 * @author yh263208
 * @date 2021-07-15 17:19
 * @since ODC_release_3.2.0
 */
public class InMemorySecuritySessionRepository implements SecuritySessionRepository {
    /**
     * A container for holding {@link SecuritySession} objects, implemented as a concurrent object
     */
    private final Map<Serializable, SecuritySession> sessionId2SessionMap = new ConcurrentHashMap<>();

    @Override
    public Serializable store(@NonNull SecuritySession session) {
        Validate.notNull(session.getId(), "Session.Id can not be null");
        this.sessionId2SessionMap.putIfAbsent(session.getId(), session);
        return session.getId();
    }

    @Override
    public SecuritySession get(@NonNull Serializable sessionId) {
        return this.sessionId2SessionMap.get(sessionId);
    }

    @Override
    public void update(@NonNull SecuritySession session) {
        Validate.notNull(session.getId(), "Session.Id can not be null");
        this.sessionId2SessionMap.put(session.getId(), session);
    }

    @Override
    public void delete(@NonNull SecuritySession session) {
        Validate.notNull(session.getId(), "Session.Id can not be null");
        this.sessionId2SessionMap.remove(session.getId());
    }

    @Override
    public Collection<SecuritySession> getAllSessions() {
        return sessionId2SessionMap.values();
    }

}
