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

/**
 * Data access object for {@link SecuritySession}
 *
 * A {@link SecuritySession} object can be stored in multi-medium, like file, DB or memory This data
 * access object is to solve the storage problem of session
 *
 * @author yh263208
 * @date 2021-07-13 17:47
 * @since ODC_release_3.2.0
 */
public interface SecuritySessionRepository {
    /**
     * Persist a session object to the storage medium
     *
     * @param session {@link SecuritySession} that will be stored
     * @return unique id for {@link SecuritySession}
     */
    Serializable store(SecuritySession session);

    /**
     * Get a session object from the storage medium
     *
     * @param sessionId query id for {@link SecuritySession}
     * @return {@link SecuritySession}, this return value can be null when nothing found
     */
    SecuritySession get(Serializable sessionId);

    /**
     * Update a {@link SecuritySession} object to the storage medium
     *
     * @param session {@link SecuritySession} which will be updated
     */
    void update(SecuritySession session);

    /**
     * Delete a {@link SecuritySession} object from storage meduim
     *
     * @param session {@link SecuritySession} which will be deleted
     */
    void delete(SecuritySession session);

    /**
     * Obtain all active {@link SecuritySession} object, The active {@link SecuritySession} object refer
     * to the {@link SecuritySession} object
     *
     * @return collection of active {@link SecuritySession}
     */
    Collection<SecuritySession> getAllSessions();
}
