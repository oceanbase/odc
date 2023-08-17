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

/**
 * Data access object for <code>com.oceanbase.odc.authority.core.session.Session</code> A
 * <code>Session</code> object can be stored in multi-medium, like file, DB or memory This data
 * access object is to solve the storage problem of session
 *
 * @author yh263208
 * @date 2021-11-15 16:58
 * @since ODC_release_3.2.2
 */
public interface ConnectionSessionRepository {
    /**
     * Persist a session object to the storage medium
     *
     * @param session {@link ConnectionSession} that will be stored
     * @return unique id for {@link ConnectionSession}
     */
    String store(ConnectionSession session);

    /**
     * Get a session object from the storage medium
     *
     * @param sessionId query id for {@link ConnectionSession}
     * @return {@link ConnectionSession}, this return value can be null when nothing found
     */
    ConnectionSession get(String sessionId);

    /**
     * Delete a {@link ConnectionSession} object from storage meduim
     *
     * @param session {@link ConnectionSession} which will be deleted
     */
    void delete(ConnectionSession session);

    /**
     * Obtain all active {@link ConnectionSession} object
     *
     * @return collection of active {@link ConnectionSession}. This return value will not be null, if
     *         there is not any active {@link ConnectionSession} object, and the method will return an
     *         empty collection
     */
    Collection<ConnectionSession> listAllSessions();

}

