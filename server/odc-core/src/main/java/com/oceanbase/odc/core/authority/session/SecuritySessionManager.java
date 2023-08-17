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
import java.util.Map;

/**
 * Session manager, used to manage the life cycle of the session. Mainly used for session creation
 * and acquisition
 *
 * @author yh263208
 * @date 2021-07-12 19:34
 * @since ODC_release_3.2.0
 */
public interface SecuritySessionManager {
    /**
     * Start a session and use a keyword to persist the session into a buffer
     *
     * @param context context for {@link SecuritySession} creation
     * @return session object
     */
    SecuritySession start(Map<String, Object> context);

    /**
     * Get a session from the session manager
     *
     * @param key Keyword used to store session objects
     * @return session object
     */
    SecuritySession getSession(Serializable key);

}
