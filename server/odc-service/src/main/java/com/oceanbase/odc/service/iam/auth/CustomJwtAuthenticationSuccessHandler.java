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
import java.time.Duration;
import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.iam.JwtService;
import com.oceanbase.odc.service.iam.LoginHistoryService;
import com.oceanbase.odc.service.iam.model.JwtConstants;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(value = "odc.iam.auth.method", havingValue = "jwt")
public class CustomJwtAuthenticationSuccessHandler extends CustomAuthenticationSuccessHandler {
    @Autowired
    private JwtService jwtService;
    @Autowired
    @Qualifier("authenticationCache")
    private Cache<Long, Authentication> authenticationCache;
    @Value("${server.servlet.session.timeout:8h}")
    private Duration timeoutSetting;

    public CustomJwtAuthenticationSuccessHandler(SecurityManager securityManager,
            LoginHistoryService loginHistoryService) {
        super(securityManager, loginHistoryService);
    }


    @Override
    protected void handleAfterSucceed(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            Authentication authentication) throws IOException {
        SuccessResponse<String> successResponse = Responses.success("ok");
        User user = (User) authentication.getPrincipal();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(JwtConstants.ID, user.getId());
        hashMap.put(JwtConstants.PRINCIPAL, user.getAccountName());
        hashMap.put(JwtConstants.ORGANIZATION_ID, user.getOrganizationId());
        hashMap.put(JwtConstants.ORGANIZATION_TYPE, JsonUtils.toJson(user.getOrganizationType()));
        String token = jwtService.sign(hashMap);
        Cookie cookie = new Cookie(JwtConstants.ODC_JWT_TOKEN, token);
        cookie.setPath("/");
        cookie.setMaxAge((int) timeoutSetting.getSeconds());
        cookie.setHttpOnly(true);
        httpServletResponse.addCookie(cookie);
        authenticationCache.put(user.getId(), authentication);
        WebResponseUtils.writeJsonObjectWithOkStatus(successResponse, httpServletRequest, httpServletResponse);

    }
}
