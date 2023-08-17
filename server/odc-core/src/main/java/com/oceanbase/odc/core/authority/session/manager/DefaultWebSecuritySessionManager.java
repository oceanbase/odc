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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionRepository;
import com.oceanbase.odc.core.authority.session.factory.SecuritySessionFactory;
import com.oceanbase.odc.core.authority.util.SecurityConstants;
import com.oceanbase.odc.core.authority.util.WebUtil;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@link DefaultWebSecuritySessionManager}, which relies on the cookie implementation of the
 * web layer.
 *
 * The difference with {@link ServletRequestSecuritySessionManager} is that the
 * {@link DefaultWebSecuritySessionManager} manages the corresponding session object by itself,
 * while {@link ServletRequestSecuritySessionManager} delegates the management capabilities to the
 * {@link ServletRequest} object.
 *
 * @author yh263208
 * @date 2021-07-16 17:30
 * @see DefaultSecuritySessionManager
 * @since ODC_release_3.2.0
 */
@Slf4j
public class DefaultWebSecuritySessionManager extends DefaultSecuritySessionManager
        implements ServletBaseSecuritySessionManager {
    /**
     * Used to monitor the occurrence of session events
     *
     * @author yh263208
     * @date 2021-07-19 15:29
     * @see RemoveSecuritySessionEventListener
     * @since ODC_release_3.2.0
     */
    static class AddCookieSecuritySessionEventListener extends RemoveSecuritySessionEventListener {

        public AddCookieSecuritySessionEventListener(DefaultSecuritySessionManager sessionManager) {
            super(sessionManager);
        }

        @Override
        public void onCreateEventSucceed(SecuritySession session, @NonNull Map<String, Object> context) {
            super.onCreateEventSucceed(session, context);
            Object req = context.get(SecurityConstants.CONTEXT_SERVLET_REQUEST_KEY);
            Object resp = context.get(SecurityConstants.CONTEXT_SERVLET_RESPONSE_KEY);
            if (!(req instanceof HttpServletRequest)) {
                throw new IllegalArgumentException("HttpServletRequest is necessary");
            }
            if (!(resp instanceof HttpServletResponse)) {
                throw new IllegalArgumentException("HttpServletResponse is necessary");
            }
            HttpServletResponse response = (HttpServletResponse) resp;
            response.addCookie(WebUtil.generateSecurityCookie(session));
        }
    }

    public DefaultWebSecuritySessionManager(SecuritySessionFactory sessionFactory,
            SecuritySessionRepository repository) {
        super(sessionFactory, repository);
        addListener(new AddCookieSecuritySessionEventListener(this));
    }

    /**
     * Start a {@link SecuritySession} by {@link ServletRequest}
     *
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @return Created {@link SecuritySession}
     */
    @Override
    public SecuritySession start(@NonNull ServletRequest request, @NonNull ServletResponse response,
            Map<String, Object> context) {
        if (context == null) {
            context = new HashMap<>();
        }
        context.put(SecurityConstants.CONTEXT_SESSION_HOST_KEY, request.getRemoteHost());
        context.put(SecurityConstants.CONTEXT_SERVLET_REQUEST_KEY, request);
        context.put(SecurityConstants.CONTEXT_SERVLET_RESPONSE_KEY, response);
        return start(context);
    }

    @Override
    public SecuritySession getSession(@NonNull ServletRequest request, @NonNull ServletResponse response) {
        Cookie cookie = WebUtil.getCookieByName(request, SecurityConstants.CUSTOM_COOKIE_NAME);
        if (cookie == null) {
            return null;
        }
        String sessionId = WebUtil.readCookieValue(SecurityConstants.CUSTOM_COOKIE_NAME, request);
        if (sessionId == null) {
            log.warn("Invalid cookie object, sessionId is null");
            return null;
        }
        SecuritySession session = getSession(sessionId);
        if (session == null) {
            cookie.setMaxAge(0);
            return null;
        }
        cookie = WebUtil.generateSecurityCookie(session);
        ((HttpServletResponse) response).addCookie(cookie);
        return session;
    }

}
