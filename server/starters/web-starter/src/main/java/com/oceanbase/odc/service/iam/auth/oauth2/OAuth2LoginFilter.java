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
package com.oceanbase.odc.service.iam.auth.oauth2;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oceanbase.odc.config.CommonSecurityProperties;
import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.session.SecuritySession;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/10/13 下午7:42
 * @Description: []
 */
@Slf4j
public class OAuth2LoginFilter extends OncePerRequestFilter {

    private final SecurityManager securityManager;

    private final CommonSecurityProperties commonSecurityProperties;


    public OAuth2LoginFilter(SecurityManager servletSecurityManager,
            CommonSecurityProperties commonSecurityProperties) {
        this.securityManager = servletSecurityManager;
        this.commonSecurityProperties = commonSecurityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        boolean urlInWhiteList = Arrays.stream(commonSecurityProperties.getAuthWhitelist())
                .anyMatch(url -> StringUtils.containsIgnoreCase(request.getRequestURI(), url));
        if (!urlInWhiteList) {
            SecuritySession session = securityManager.getSession(null);
            if (session == null) {
                log.info("Can not get the session from request for security framework, requestURI={}",
                        request.getRequestURI());
                DefaultLoginSecurityManager.removeContext();
                DefaultLoginSecurityManager.removeSecurityContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
