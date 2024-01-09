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
package com.oceanbase.odc.service.iam.auth.ldap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;
import com.oceanbase.odc.service.integration.model.LdapContextHolder;
import com.oceanbase.odc.service.integration.model.LdapContextHolder.LdapContext;

public class LdapUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final SensitivePropertyHandler sensitivePropertyHandler;

    public LdapUsernamePasswordAuthenticationFilter(SensitivePropertyHandler sensitivePropertyHandler,
            AuthenticationManager authenticationManager) {
        super();
        this.sensitivePropertyHandler = sensitivePropertyHandler;
        this.setAuthenticationManager(authenticationManager);
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/api/v2/iam/ldap/login", "POST"));
    }

    public LdapUsernamePasswordAuthenticationFilter(AntPathRequestMatcher antPathRequestMatcher,
            SensitivePropertyHandler sensitivePropertyHandler,
            AuthenticationManager authenticationManager) {
        this(sensitivePropertyHandler, authenticationManager);
        this.setRequiresAuthenticationRequestMatcher(antPathRequestMatcher);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        LdapContext context = LdapContextHolder.getContext();
        if (StringUtils.isBlank(context.getRegistrationId())) {
            throw new AuthenticationServiceException("registrationId is empty");
        }
        TraceContextHolder.setAccountName(context.getUsername());
        String decrypt = sensitivePropertyHandler.decrypt(context.getPassword());
        LdapPasswordAuthenticationToken authRequest =
                new LdapPasswordAuthenticationToken(context.getUsername(), decrypt);
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
