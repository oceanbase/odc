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
package com.oceanbase.odc.core.authority.util;

import java.util.concurrent.TimeUnit;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.authority.session.SecuritySession;

import lombok.NonNull;

/**
 * For web-related tools, encapsulate some web-related operations
 *
 * @author yh263208
 * @date 2021-07-16 17:34
 * @since ODC_release_3.2.0
 */
public class WebUtil {
    /**
     * Get a httponly {@link Cookie} object
     *
     * @param session {@link SecuritySession} object
     * @return Template {@link Cookie} object
     */
    public static Cookie generateSecurityCookie(@NonNull SecuritySession session) {
        Cookie cookie = new Cookie(SecurityConstants.CUSTOM_COOKIE_NAME, (String) session.getId());
        cookie.setHttpOnly(true);
        long maxAgeSeconds = TimeUnit.SECONDS.convert(session.getTimeoutMillis(), TimeUnit.MILLISECONDS);
        if (maxAgeSeconds > Integer.MAX_VALUE) {
            cookie.setMaxAge(Integer.MAX_VALUE);
        } else {
            cookie.setMaxAge((int) maxAgeSeconds);
        }
        cookie.setPath("/");
        return cookie;
    }

    public static Cookie getCookieByName(ServletRequest request, @NonNull String cookieName) {
        HttpServletRequest request1 = toHttp(request);
        Cookie[] cookies = request1.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    public static String readCookieValue(String cookieName, ServletRequest request) {
        Cookie cookie = getCookieByName(request, cookieName);
        Validate.notNull(cookie, "Cookie can not be null for WebRequestUtils#readCookieValue");
        HttpServletRequest request1 = toHttp(request);
        /**
         * <code>Cookie</code> path may be null or empty, which means matches any request URI
         */
        String path = cookie.getPath();
        if (path != null && StringUtils.isBlank(path.trim())) {
            path = null;
        }
        if (path != null) {
            path = path.trim();
        }
        if (path != null && !pathMatches(path, request1.getRequestURI())) {
            return null;
        }
        return cookie.getValue();
    }

    private static HttpServletRequest toHttp(@NonNull ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            return (HttpServletRequest) request;
        }
        throw new IllegalArgumentException("Request is not an instance of HttpServletRequest");
    }

    private static boolean pathMatches(@NonNull String cookiePath, @NonNull String requestPath) {
        if (!requestPath.startsWith(cookiePath)) {
            return false;
        }
        return requestPath.length() == cookiePath.length() || cookiePath.charAt(cookiePath.length() - 1) == '/'
                || requestPath.charAt(cookiePath.length()) == '/';
    }

}
