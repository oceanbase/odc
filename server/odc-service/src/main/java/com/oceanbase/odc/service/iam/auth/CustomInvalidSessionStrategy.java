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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.session.InvalidSessionStrategy;
import org.springframework.web.servlet.LocaleResolver;

import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.common.util.WebResponseUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/6
 */

@Slf4j
public class CustomInvalidSessionStrategy implements InvalidSessionStrategy {

    public static final String DATA_URI_PREFIX = "/api/v";
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final String redirectUrl;
    private final LocaleResolver localeResolver;

    public CustomInvalidSessionStrategy(String redirectUrl, LocaleResolver localeResolver) {
        this.redirectUrl = redirectUrl;
        this.localeResolver = localeResolver;
    }

    @Override
    public void onInvalidSessionDetected(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        log.info("Session invalid for uri={}", httpServletRequest.getRequestURI());
        if (httpServletRequest.getRequestURI().startsWith(DATA_URI_PREFIX)) {
            BadRequestException exception = new BadRequestException("Invalid Http Session");
            WebResponseUtils.writeBackLoginExpiredJson(httpServletRequest, httpServletResponse, exception,
                    this.localeResolver);
        } else {
            redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, redirectUrl);
        }
    }
}
