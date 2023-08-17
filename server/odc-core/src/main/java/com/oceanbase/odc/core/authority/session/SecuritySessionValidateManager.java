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

import java.util.Collection;

/**
 * The {@link SecuritySessionValidateManager} is used to verify whether a session is valid. Three
 * methods are provided, one is used to obtain all current sessions, and the other provides a method
 * to delete an expired session
 *
 * @author yh263208
 * @date 2021-07-15 10:43
 * @since ODC_release_3.2.0
 */
public interface SecuritySessionValidateManager {
    /**
     * Get all {@link SecuritySession} object from session manager including expired
     * {@link SecuritySession}
     *
     * @return collection of {@link SecuritySession} object
     */
    Collection<SecuritySession> retrieveAllSessions();

    /**
     * Remove a expired {@link SecuritySession} object from {@link SecuritySessionManager}
     *
     * @param session {@link SecuritySession} object taht will be removed
     */
    void removeCertainSession(SecuritySession session);

}
