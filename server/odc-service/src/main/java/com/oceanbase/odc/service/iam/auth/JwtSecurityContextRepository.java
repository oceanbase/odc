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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.auth0.jwt.interfaces.Claim;
import com.github.benmanes.caffeine.cache.Cache;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.jwt.JwtUtils;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.collaboration.OrganizationResourceMigrator;
import com.oceanbase.odc.service.iam.OrganizationMapper;
import com.oceanbase.odc.service.iam.model.JwtProperties;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtSecurityContextRepository implements SecurityContextRepository {
    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    @Qualifier("organizationResourceMigrator")
    private OrganizationResourceMigrator organizationResourceMigrator;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    @Qualifier("authenticationCache")
    private Cache<Long, Authentication> authenticationCache;

    protected final Log logger = LogFactory.getLog(this.getClass());
    private final OrganizationMapper organizationMapper = OrganizationMapper.INSTANCE;

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {

        HttpServletRequest request = requestResponseHolder.getRequest();
        HttpServletResponse response = requestResponseHolder.getResponse();

        // String token = request.getHeader(JwtProperties.TOKEN);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Cookie[] cookies = request.getCookies();
        String token = null;

        if (cookies == null) {
            if (this.logger.isTraceEnabled()) {
                this.logger
                        .trace(LogMessage.format("Created %s because there was no cookie", context));
            }
            return context;
        }

        for (Cookie cookie : cookies) {
            if (JwtProperties.TOKEN.equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }

        if (!StringUtils.hasText(token) || !JwtUtils.verify(token)) {
            if (this.logger.isTraceEnabled()) {
                this.logger
                        .trace(LogMessage.format("Created %s because there was no token or token is invalid", context));
            }
            return context;
        }

        // 获取过期时间
        Date expiration = JwtUtils.getExpiresAt(token);

        // 判断是否过期
        if (JwtUtils.isExpired(expiration)) {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace(LogMessage.format("Created %s because token is expired", context));
            }
            return context;
        }


        // 模拟出认证通过后完整的Authenication对象

        Map<String, Claim> claims = JwtUtils.getClaims(token);
        long id = claims.get(JwtProperties.ID).asLong();

        if (authenticationCache.getIfPresent(id) != null) {
            Authentication authentication = authenticationCache.getIfPresent(id);
            context.setAuthentication(authentication);
        } else {

            String username = claims.get(JwtProperties.PRINCIPAL).asString();


            UserEntity userEntity = userRepository.findByAccountName(username).orElseThrow(() -> {
                log.warn("Username not found: username {}", username);
                return new UsernameNotFoundException(username);
            });
            User user = new User(userEntity);
            user.setOrganizationId(claims.get(JwtProperties.ORGANIZATION_ID).asLong());

            OrganizationType organizationType =
                    JsonUtils.fromJson(claims.get(JwtProperties.ORGANIZATION_TYPE).asString(),
                            OrganizationType.class);
            user.setOrganizationType(organizationType);
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(user, null, null);
            context.setAuthentication(usernamePasswordAuthenticationToken);
            authenticationCache.put(user.getId(), usernamePasswordAuthenticationToken);
        }


        // jwt续期策略，默认在快要失效时间前完成续期，避免频繁更新
        User user = (User) context.getAuthentication().getPrincipal();
        if (JwtUtils.isRenew(expiration)) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put(JwtProperties.ID, user.getId());
            hashMap.put(JwtProperties.PRINCIPAL, user.getAccountName());
            hashMap.put(JwtProperties.ORGANIZATION_ID, user.getOrganizationId());
            hashMap.put(JwtProperties.ORGANIZATION_TYPE, JsonUtils.toJson(user.getOrganizationType()));
            String renewToken = JwtUtils.sign(hashMap);
            Cookie cookie = new Cookie(JwtProperties.TOKEN, renewToken);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            // response.setHeader(JwtProperties.TOKEN, renewToken);
        }

        return context;
    }



    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {

    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        String header = request.getHeader(JwtProperties.TOKEN);
        if (StringUtils.hasText(header)) {
            return true;
        }
        return false;
    }
}
