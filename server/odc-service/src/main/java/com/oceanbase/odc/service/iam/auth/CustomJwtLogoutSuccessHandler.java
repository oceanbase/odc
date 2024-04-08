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

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.iam.model.JwtConstants;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/4
 */
@Slf4j
@Component
@ConditionalOnProperty(value = {"odc.iam.auth.method"}, havingValue = "jwt")
public class CustomJwtLogoutSuccessHandler implements LogoutSuccessHandler {
    @Autowired
    @Qualifier("authenticationCache")
    private Cache<Long, Authentication> authenticationCache;

    public CustomJwtLogoutSuccessHandler() {}

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            Authentication authentication) throws IOException, ServletException {

        Cookie cookie = new Cookie(JwtConstants.ODC_JWT_TOKEN, JwtConstants.AUTHENTICATION_BLANK_VALUE);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        httpServletResponse.addCookie(cookie);
        User user = (User) authentication.getPrincipal();
        if (authenticationCache.getIfPresent(user.getId()) != null) {
            authenticationCache.invalidate(user.getId());
        }

        setJsonResponse(httpServletRequest, httpServletResponse);
    }

    private void setJsonResponse(HttpServletRequest httpServletRequest, HttpServletResponse response)
            throws IOException {
        SuccessResponse successResponse = Responses.success("ok");
        WebResponseUtils.writeJsonObjectWithOkStatus(successResponse, httpServletRequest, response);
    }
}
