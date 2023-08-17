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
package com.oceanbase.odc.service.iam.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.servlet.LocaleResolver;

import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.service.common.util.WebResponseUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractOdcPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    private final LocaleResolver localeResolver;
    private final SecurityManager securityManager;


    public AbstractOdcPreAuthenticatedProcessingFilter(@NonNull SecurityManager securityManager,
            LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
        this.securityManager = securityManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        try {
            getSession(httpServletRequest);
            super.doFilter(request, response, chain);
        } catch (AccessDeniedException ex) {
            if (httpServletRequest.getRequestURI().startsWith(CustomInvalidSessionStrategy.DATA_URI_PREFIX)) {
                WebResponseUtils.writeBackLoginExpiredJson(httpServletRequest, httpServletResponse, ex,
                        this.localeResolver);
            } else {
                httpServletResponse.sendRedirect("/index.html");
            }
        } finally {
            DefaultLoginSecurityManager.removeContext();
            DefaultLoginSecurityManager.removeSecurityContext();
        }
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "";
    }

    protected void getSession(HttpServletRequest request) {
        SecuritySession session = securityManager.getSession(null);
        if (session == null) {
            log.debug("Can not get the session, uri={}", request.getRequestURI());
        }
    }
}
