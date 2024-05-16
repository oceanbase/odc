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
package com.oceanbase.odc.config;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import com.google.common.net.InetAddresses;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCode;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.common.response.OdcErrorResult;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.common.util.WebResponseUtils;

/**
 * we always disable csrf for HttpSecurity configuration, create CsrfFilter manually for avoid
 * conflict with session management filter
 * 
 * @author wenniu.ly
 * @date 2021/6/18
 */
@Component
public class CsrfConfigureHelper {

    @Autowired
    private CommonSecurityProperties commonSecurityProperties;

    @Autowired
    private LocaleResolver localeResolver;

    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        if (commonSecurityProperties.isCsrfEnabled()) {
            CsrfFilter csrfFilter = new CsrfFilter(new OdcCsrfTokenRepository());
            csrfFilter.setRequireCsrfProtectionMatcher(requestMatcher());
            csrfFilter.setAccessDeniedHandler(accessDeniedHandler());
            http.addFilterAfter(csrfFilter, SessionManagementFilter.class);
        }
    }

    private RequestMatcher requestMatcher() {
        return new DefaultRequiresCsrfMatcher();
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return new OdcAccessDeniedHandlerImpl();
    }

    public static class DefaultRequiresCsrfMatcher implements RequestMatcher {
        private final Set<String> allowedMethods;
        private final Set<String> allowedUrls;

        private DefaultRequiresCsrfMatcher() {
            this.allowedMethods = new HashSet(Arrays.asList("GET", "HEAD", "TRACE", "OPTIONS"));
            this.allowedUrls = new HashSet(Arrays.asList(
                    "/api/v1/user/create",
                    "/api/v2/iam/login",
                    "/api/v1/user/csrfToken",
                    "/api/v2/bastion/login"));
        }

        public boolean matches(HttpServletRequest request) {
            String requestURI = request.getRequestURI();
            if (allowedMethods.contains(request.getMethod())) {
                return false;
            }
            if (allowedUrls.contains(requestURI)) {
                return false;
            }
            if (isBasicAuth(request)) {
                return false;
            }
            return true;
        }

        private boolean isBasicAuth(HttpServletRequest request) {
            String authHeader = request.getHeader("Authorization");
            return (authHeader != null && authHeader.startsWith("Basic "));
        }
    }

    private static class OdcCsrfTokenRepository implements CsrfTokenRepository {
        private CookieCsrfTokenRepository delegates = CookieCsrfTokenRepository.withHttpOnlyFalse();

        @Override
        public CsrfToken generateToken(HttpServletRequest request) {
            return delegates.generateToken(request);
        }

        @Override
        public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
            OdcHttpServletResponseWrapper wrapper = new OdcHttpServletResponseWrapper(request, response);
            delegates.saveToken(token, request, wrapper);
        }

        @Override
        public CsrfToken loadToken(HttpServletRequest request) {
            return delegates.loadToken(request);
        }
    }

    private static class OdcHttpServletResponseWrapper extends HttpServletResponseWrapper {
        private final HttpServletRequest request;

        public OdcHttpServletResponseWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(response);
            this.request = request;
        }

        @Override
        public void addCookie(Cookie cookie) {
            String requestHost = WebRequestUtils.getRequestHost(this.request);
            String domain = InetAddresses.isInetAddress(requestHost) ? requestHost : calcParentDomain(requestHost);
            cookie.setDomain(domain);
            super.addCookie(cookie);
        }

        private String calcParentDomain(String host) {
            if (StringUtils.isEmpty(host)) {
                return host;
            }
            String[] items = host.split("\\.");
            return host.replace(items[0] + ".", "");
        }
    }

    public class OdcAccessDeniedHandlerImpl extends AccessDeniedHandlerImpl {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                AccessDeniedException accessDeniedException) throws IOException, ServletException {
            if (!response.isCommitted()) {
                ErrorCode errorCode = ErrorCodes.AccessDenied;
                if (accessDeniedException instanceof MissingCsrfTokenException) {
                    errorCode = ErrorCodes.MissingCsrfToken;
                } else if (accessDeniedException instanceof InvalidCsrfTokenException) {
                    errorCode = ErrorCodes.InvalidCsrfToken;
                }
                OdcErrorResult errorResult = initErrorResult(errorCode,
                        new Object[] {"NO_TOKEN X-CSRF-Token header may not set or invalid"});
                WebResponseUtils.writeJsonObjectWithBadRequestStatus(errorResult, request, response);
            }
        }

        public OdcErrorResult initErrorResult(ErrorCode errorCode, Object[] args) {
            try {
                OdcErrorResult result = OdcErrorResult.error(errorCode, args);
                return attachTraceInfo(result);
            } catch (Throwable throwable) {
                return OdcErrorResult.empty();
            }
        }

        private OdcErrorResult attachTraceInfo(OdcErrorResult result) {
            result.setServer(SystemUtils.getHostName());
            result.setTraceId(TraceContextHolder.getTraceId());
            result.setDurationMillis(TraceContextHolder.getDuration());
            result.setTimestamp(OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(TraceContextHolder.getStartEpochMilli()), ZoneId.systemDefault()));
            return result;
        }
    }

}
