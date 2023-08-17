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
package com.oceanbase.odc.core.authority;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;
import com.oceanbase.odc.core.authority.model.LoginSecurityManagerConfig;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionManager;
import com.oceanbase.odc.core.authority.session.manager.ServletBaseSecuritySessionManager;
import com.oceanbase.odc.core.authority.util.SecurityConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Servlet-based security manager
 *
 * @author yh263208
 * @date 2021-07-21 20:42
 * @since ODC_release_3.2.0
 * @see DefaultLoginSecurityManager
 */
@Slf4j
public abstract class BaseServletSecurityManager extends DefaultLoginSecurityManager
        implements ServletBaseSecuritySessionManager {

    public BaseServletSecurityManager(LoginSecurityManagerConfig config) {
        super(config);
        if (!(config.getSessionManager() instanceof ServletBaseSecuritySessionManager)) {
            throw new IllegalArgumentException("SessionManager's type is illegal");
        }
    }

    public Subject login(ServletRequest request, ServletResponse response) throws AuthenticationException {
        Collection<BaseAuthenticationToken<? extends Principal, ?>> tokens =
                getTokenFromRequest((HttpServletRequest) request);
        BaseServletSecurityManager that = this;
        return login(tokens, new DelegateSessionManager() {
            @Override
            public SecuritySession startSession() {
                return start(request, response, null);
            }

            @Override
            public SecuritySession getSession() {
                return that.getSession(request, response);
            }
        });
    }

    protected abstract Collection<BaseAuthenticationToken<? extends Principal, ?>> getTokenFromRequest(
            HttpServletRequest request);

    @Override
    public SecuritySession start(ServletRequest request, ServletResponse response, Map<String, Object> context) {
        SecuritySessionManager sessionManager = getSessionManager();
        if (!(sessionManager instanceof ServletBaseSecuritySessionManager)) {
            throw new UnsupportedOperationException("SessionManager's type is illegal");
        }
        return ((ServletBaseSecuritySessionManager) sessionManager).start(request, response, context);
    }

    @Override
    public SecuritySession getSession(ServletRequest request, ServletResponse response) {
        ServletBaseSecuritySessionManager sessionManager = (ServletBaseSecuritySessionManager) getSessionManager();
        SecuritySession session = sessionManager.getSession(request, response);
        if (session == null) {
            return null;
        }
        try {
            Subject subject = (Subject) session.getAttribute(SecurityConstants.SECURITY_SESSION_SUBJECT_KEY);
            setContext(subject);
        } catch (InvalidSessionException e) {
            log.warn("Failed to get subject from the session, sessionId={}, sessionHost={}", session.getId(),
                    session.getHost(), e);
        }
        return session;
    }

}
