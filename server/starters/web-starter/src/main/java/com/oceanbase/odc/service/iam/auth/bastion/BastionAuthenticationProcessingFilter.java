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
package com.oceanbase.odc.service.iam.auth.bastion;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.service.bastion.model.AuthLoginReq;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BastionAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private static final RequestMatcher LOGIN_REQUEST_MATCHER =
            new AntPathRequestMatcher("/api/v2/bastion/login", "POST");

    private final ObjectMapper objectMapper;

    public BastionAuthenticationProcessingFilter(ObjectMapper objectMapper) {
        super(LOGIN_REQUEST_MATCHER);
        this.objectMapper = objectMapper;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        AuthLoginReq loginReq = AuthLoginReq.fromRequest(request, objectMapper);
        String token = Objects.isNull(loginReq.getToken()) ? "" : loginReq.getToken();
        BastionAuthenticationToken authRequest =
                new BastionAuthenticationToken("Bastion-username-before-verify", token);
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
