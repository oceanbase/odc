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

import lombok.NonNull;

/**
 * Manager to manage {@code ConnectionSession}, used to get a {@code ConnectionSession} create a
 * {@code ConnectionManager}, etc
 *
 * @author yh263208
 * @date 2021-11-01 20?28
 * @since ODC_release_3.2.2
 */
public interface ConnectionSessionManager {
    /**
     * Start a session and use a keyword to persist the session into a buffer
     *
     * @param factory {@code ConnectionSessionFactory}
     * @return session object
     */
    ConnectionSession start(@NonNull ConnectionSessionFactory factory);

    /**
     * Get a session from the session manager
     *
     * @param id Keyword used to store session objects
     * @return session object
     */
    ConnectionSession getSession(@NonNull String id);
}

