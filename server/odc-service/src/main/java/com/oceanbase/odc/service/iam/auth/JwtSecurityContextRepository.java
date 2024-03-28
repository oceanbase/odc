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

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.auth0.jwt.interfaces.Claim;
import com.github.benmanes.caffeine.cache.Cache;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.iam.JwtService;
import com.oceanbase.odc.service.iam.model.JwtConstants;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(value = {"odc.iam.auth.method"}, havingValue = "jwt")
public class JwtSecurityContextRepository implements SecurityContextRepository {

    @Autowired
    private JdbcUserDetailService jdbcUserDetailService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    @Qualifier("authenticationCache")
    private Cache<Long, Authentication> authenticationCache;
    @Value("${server.servlet.session.timeout:8h}")
    private Duration timeoutSetting;

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {

        HttpServletRequest request = requestResponseHolder.getRequest();
        HttpServletResponse response = requestResponseHolder.getResponse();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Cookie[] cookies = request.getCookies();
        String token = null;

        if (cookies == null) {
            log.info("Created SecurityContext {} because there was no cookie", context);
            return context;
        }

        for (Cookie cookie : cookies) {
            if (JwtConstants.ODC_JWT_TOKEN.equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }

        if (!StringUtils.hasText(token) || !jwtService.verify(token)) {
            log.info("Created SecurityContext {} because there was no token or token is invalid", context);
            return context;
        }

        Date expiration = jwtService.getExpiresAt(token);
        if (jwtService.isExpired(expiration)) {
            log.info("Created SecurityContext {} because token is expired", context);
            return context;
        }

        Map<String, Claim> claims = jwtService.getClaims(token);
        long id = claims.get(JwtConstants.ID).asLong();

        if (authenticationCache.getIfPresent(id) != null) {
            Authentication authentication = authenticationCache.getIfPresent(id);
            context.setAuthentication(authentication);
        } else {
            String username = claims.get(JwtConstants.PRINCIPAL).asString();
            UserDetails userDetails = jdbcUserDetailService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, null);
            context.setAuthentication(usernamePasswordAuthenticationToken);
            authenticationCache.put(id, usernamePasswordAuthenticationToken);
        }

        // jwt renewal policy, the default is to complete the renewal before the expiration time, to avoid
        // frequent updates
        User user = (User) context.getAuthentication().getPrincipal();
        if (jwtService.isRenew(expiration)) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put(JwtConstants.ID, user.getId());
            hashMap.put(JwtConstants.PRINCIPAL, user.getAccountName());
            hashMap.put(JwtConstants.ORGANIZATION_ID, user.getOrganizationId());
            hashMap.put(JwtConstants.ORGANIZATION_TYPE, JsonUtils.toJson(user.getOrganizationType()));
            String renewToken = jwtService.sign(hashMap);
            Cookie cookie = new Cookie(JwtConstants.ODC_JWT_TOKEN, renewToken);
            cookie.setPath("/");
            cookie.setMaxAge((int) timeoutSetting.getSeconds());
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }
        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {}

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return StringUtils.hasText(request.getHeader(JwtConstants.ODC_JWT_TOKEN));
    }
}
