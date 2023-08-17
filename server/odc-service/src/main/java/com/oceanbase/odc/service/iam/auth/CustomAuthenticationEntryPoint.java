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

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.servlet.LocaleResolver;

import com.oceanbase.odc.service.common.util.WebResponseUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <code>CustomAuthenticationEntryPoint</code> to process login failure (no <code>JSESSIONID</code>)
 *
 * @author yh263208
 * @date 2021-08-31 12:03
 * @since ODC_release_3.2.0
 * @see org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
 */
@Slf4j
public class CustomAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    private final LocaleResolver localeResolver;

    /**
     * @param loginFormUrl URL where the login page can be found. Should either be relative to the
     *        web-app context path (include a leading {@code /}) or an absolute URL.
     */
    public CustomAuthenticationEntryPoint(String loginFormUrl, LocaleResolver localeResolver) {
        super(loginFormUrl);
        this.localeResolver = localeResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        log.info("Not login for uri={}", request.getRequestURI());
        if (request.getRequestURI().startsWith(CustomInvalidSessionStrategy.DATA_URI_PREFIX)) {
            WebResponseUtils.writeBackLoginExpiredJson(request, response, authException, this.localeResolver);
        } else {
            super.commence(request, response, authException);
        }
    }
}
