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
package com.oceanbase.odc.service.iam.auth.local;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.oceanbase.odc.service.integration.oauth2.TestLoginContext;
import com.oceanbase.odc.service.integration.oauth2.TestLoginManager;

/**
 * test login with terminate when user info has been
 * obtained；{@link TestLoginManager#abortIfTestLoginTest} Default OAuth2LoginAuthenticationFilter
 * login failure handling will clean up security context
 * {@link AbstractAuthenticationProcessingFilter#unsuccessfulAuthentication(HttpServletRequest, HttpServletResponse, AuthenticationException)},
 * will cause the original login state failure。
 * {@link HttpSessionSecurityContextRepository.SaveToSessionResponseWrapper#saveContext(SecurityContext context)},
 * 
 */
public class TestLoginAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TestLoginContext.stashTestLoginSecurityContext(request);
            filterChain.doFilter(request, response);
            TestLoginContext.restoreTestLoginSecurityContext(request);
        } finally {
            TestLoginContext.removeSecurityContext();
        }
    }
}
