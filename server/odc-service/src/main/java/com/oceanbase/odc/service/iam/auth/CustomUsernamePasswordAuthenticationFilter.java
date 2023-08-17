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

import org.apache.commons.lang.Validate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.shared.exception.AttemptLoginOverLimitException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;
import com.oceanbase.odc.service.iam.util.FailedLoginAttemptLimiter;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/4
 */
@Slf4j
public class CustomUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final SecurityManager securityManager;
    private final LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache;
    private final SensitivePropertyHandler sensitivePropertyHandler;

    public CustomUsernamePasswordAuthenticationFilter(@NonNull SecurityManager securityManager,
            @NonNull LoadingCache<String, FailedLoginAttemptLimiter> clientAddressLoginAttemptCache,
            SensitivePropertyHandler sensitivePropertyHandler) {
        this.securityManager = securityManager;
        this.clientAddressLoginAttemptCache = clientAddressLoginAttemptCache;
        this.sensitivePropertyHandler = sensitivePropertyHandler;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            SecuritySession session = securityManager.getSession(null);
            if (session == null) {
                log.debug("Can not get the session, uri={}", ((HttpServletRequest) request).getRequestURI());
            }
            super.doFilter(request, response, chain);
        } finally {
            DefaultLoginSecurityManager.removeContext();
            DefaultLoginSecurityManager.removeSecurityContext();
        }
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        FailedLoginAttemptLimiter failedLoginAttemptLimiter =
                clientAddressLoginAttemptCache.get(WebRequestUtils.getClientAddress(request));
        String username = request.getParameter("username");
        String password = sensitivePropertyHandler.decrypt(request.getParameter("password"));
        TraceContextHolder.setAccountName(username);
        try {
            Validate.notNull(failedLoginAttemptLimiter, "Failed to get failedLoginAttemptLimiter");
            final Holder<Authentication> authenticationHolder = new Holder<>();
            CustomUsernamePasswordAuthenticationFilter that = this;
            failedLoginAttemptLimiter.attempt(() -> {
                UsernamePasswordAuthenticationToken token =
                        new UsernamePasswordAuthenticationToken(username, password);
                that.setDetails(request, token);
                Authentication authentication = that.getAuthenticationManager().authenticate(token);
                authenticationHolder.setValue(authentication);
                return true;
            });
            return authenticationHolder.getValue();
        } catch (AuthenticationException e) {
            // if already AuthenticationException throw straightly
            throw e;
        } catch (OverLimitException | AttemptLoginOverLimitException e) {
            throw new AuthenticationServiceException("AttemptAuthentication over limit", e);
        } catch (Exception e) {
            // InternalAuthenticationServiceException result into exception stack log output inside spring
            // security AbstractAuthenticationProcessingFilter
            throw new InternalAuthenticationServiceException("Authentication failed", e);
        }
    }
}
