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

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionManager;

/**
 * Session manager, used to manage the life cycle of the session. Mainly used for session creation
 * and acquisition. This interface is based on {@link HttpServletRequest} and
 * {@link HttpServletResponse}
 *
 * @author yh263208
 * @date 2021-07-21 20:48
 * @since ODC_release_3.2.0
 */
public interface ServletBaseSecuritySessionManager extends SecuritySessionManager {
    /**
     * Start a session and use a keyword to persist the session into a buffer
     *
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param context context for {@link SecuritySession} creation
     * @return session object
     */
    SecuritySession start(ServletRequest request, ServletResponse response, Map<String, Object> context);

    /**
     * Get a session from the session manager
     *
     * @param request Keyword used to store session objects, <code>HttpServletRequest</code>
     * @param response {@link HttpServletResponse}
     * @return session object
     */
    SecuritySession getSession(ServletRequest request, ServletResponse response);

}
