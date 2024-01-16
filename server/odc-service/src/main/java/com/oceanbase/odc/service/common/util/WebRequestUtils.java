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
package com.oceanbase.odc.service.common.util;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebRequestUtils {

    private static final String USER_AGENT_HEADER = "User-Agent";

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"};

    /**
     * 获取请求地址，不同 client 可能使用不同的请求地址访问 OCP，如 DNS，VIP，IP直连等方式。<br>
     * 注意客户端的请求可能是通过反向代理服务器访问的，这里使用了 Spring 的 UriComponentsBuilder 工具来结合 header 的值计算客户端实际访问的 url，比如有可能
     * https 是基于代理服务器实现的，但是 ocp-server 是使用的 http。
     */
    public static String getRequestAddress(HttpServletRequest request) {
        UriComponents uriComponents = getRequestUriComponents(request);
        // scheme may http/https
        String scheme = uriComponents.getScheme();
        String host = uriComponents.getHost();
        int port = uriComponents.getPort();
        // 当 http 使用 80 或 https 使用 443 端口时，port == -1，地址不包含 port
        if (port != -1) {
            return String.format("%s://%s:%d", scheme, host, port);
        }
        return String.format("%s://%s", scheme, host);
    }

    public static String getRequestHost(HttpServletRequest request) {
        return getRequestUriComponents(request).getHost();
    }

    public static String getRequestFullURL(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String query = request.getQueryString();
        if (StringUtils.isNotEmpty(query)) {
            url.append('?').append(query);
        }
        return url.toString();
    }

    private static UriComponents getRequestUriComponents(HttpServletRequest request) {
        PreConditions.notNull(request, "request");
        HttpRequest httpRequest = new ServletServerHttpRequest(request);
        UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(httpRequest).build();
        if (StringUtils.isEmpty(uriComponents.getHost())) {
            String requestURL = request.getRequestURL().toString();
            uriComponents = UriComponentsBuilder.fromUriString(requestURL).build();
        }
        return uriComponents;
    }

    /**
     * 除了 getRemoteAddr，其他header均可伪造
     */
    public static String getClientAddress(HttpServletRequest request) {
        if (Objects.isNull(request)) {
            return "N/A";
        }
        for (String header : IP_HEADER_CANDIDATES) {
            String remoteAddress = request.getHeader(header);
            if (remoteAddress != null && remoteAddress.length() != 0 && !"unknown".equalsIgnoreCase(remoteAddress)) {
                return remoteAddress;
            }
        }
        return request.getRemoteAddr ();
    }

    public static String getUserAgent(HttpServletRequest request) {
        if (Objects.isNull(request)) {
            return "N/A";
        }
        String header = request.getHeader("User-Agent");
        return header == null ? "Unknown" : header;
    }

    public static String getCookieValue(HttpServletRequest request, String key) {
        if (Objects.isNull(request)) {
            log.warn("HttpServletRequest is null when getting cookie value");
            return null;
        }
        Cookie[] cookies = request.getCookies();
        String value = null;
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(key)) {
                    value = cookie.getValue();
                }
            }
        }
        return value;
    }

    /**
     * Get <code>Cookie</code> object from <code>ServletRequest</code>
     *
     * @param request <code>ServletRequest</code> object, this object have to be an instance of
     *        <code>HttpServletRequest</code>
     * @param cookieName Name for <code>Cookie</code> object
     * @return A certain <code>Cookie</code>
     * @exception IllegalArgumentException exception will be thrown when request is not an instance of
     *            <code>HttpServletRequest</code>
     */
    public static Cookie getCookieByName(ServletRequest request, String cookieName) {
        Validate.notNull(cookieName, "Cookie Name can not be null for WebRequestUtils#getCookieFromRequest");
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

    /**
     * Remove a <code>Cookie</code> from response
     *
     * @param cookieName name for <code>Cookie</code>
     * @param req request object
     * @param resp response object
     */
    public static void removeCookieByName(String cookieName, ServletRequest req, ServletResponse resp) {
        Validate.notNull(cookieName, "Cookie name can not be null for WebRequestUtils#removeCookie");
        HttpServletRequest request = toHttp(req);
        HttpServletResponse response = toHttp(resp);
        Cookie cookie = getCookieByName(request, cookieName);
        if (cookie == null) {
            return;
        }
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Read value from a certain <code>Cookie</code>
     *
     * @param request <code>HttpServletRequest</code>
     * @param cookieName <code>Cookie</code> object's name
     * @return <code>Cookie</code> value
     */
    public static String readCookieValue(String cookieName, ServletRequest request) {
        Cookie cookie = getCookieByName(request, cookieName);
        Validate.notNull(cookie, "Cookie can not be null for WebRequestUtils#readCookieValue");
        HttpServletRequest request1 = toHttp(request);
        /**
         * <code>Cookie</code> path may be null or empty, which means matches any request URI
         */
        String path = cookie.getPath();
        if (path != null && org.apache.commons.lang.StringUtils.isBlank(path.trim())) {
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

    public static String getHeaderValue(HttpServletRequest httpRequest, String headerName) {
        return httpRequest.getHeader(headerName);
    }

    public static HttpHeaders getHeaders(HttpServletRequest httpRequest) {
        return Collections.list(httpRequest.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        h -> Collections.list(httpRequest.getHeaders(h)),
                        (oldValue, newValue) -> newValue,
                        HttpHeaders::new));
    }

    @SuppressWarnings("all")
    public static <T> T getValue(@NonNull String key, Class<T> clazz) {
        ServletRequestAttributes attributes = getRequestAttributes();
        Object value = attributes.getAttribute(key, RequestAttributes.SCOPE_REQUEST);
        if (value == null) {
            return null;
        } else if (!clazz.equals(value.getClass())) {
            return null;
        }
        return (T) value;
    }

    private static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Verify.notNull(requestAttributes, "RequestAttributes");
        return (ServletRequestAttributes) requestAttributes;
    }

    public static boolean isRedirectUrlValid(HttpServletRequest request, String redirectUrl) {
        if (StringUtils.isBlank(redirectUrl)) {
            return false;
        }
        String requestHost = getRequestUriComponents(request).getHost();
        String redirectHost = UriComponentsBuilder.fromUriString(redirectUrl).build().getHost();
        return StringUtils.equals(requestHost, redirectHost) || StringUtils.isEmpty(redirectHost);
    }

    /**
     * Judge method to indicate whether the cookie path match the requestURI
     *
     * @param cookiePath cookie path
     * @param requestPath request URI
     * @return matches flag
     */
    private static boolean pathMatches(String cookiePath, String requestPath) {
        Validate.notNull(cookiePath, "Cookie Path can not be null for WebRequestUtils#pathMatches");
        Validate.notNull(requestPath, "Request Path can not be null for WebRequestUtils#pathMatches");
        if (!requestPath.startsWith(cookiePath)) {
            return false;
        }
        return requestPath.length() == cookiePath.length() || cookiePath.charAt(cookiePath.length() - 1) == '/'
                || requestPath.charAt(cookiePath.length()) == '/';
    }

    /**
     * Convert method to convert <code>ServletRequest</code> to <code>HttpServletRequest</code>
     *
     * @param request <code>ServletRequest</code>
     * @return <code>HttpServletRequest</code>
     * @exception IllegalArgumentException exception will be thrown when request is null
     */
    private static HttpServletRequest toHttp(ServletRequest request) {
        Validate.notNull(request, "Request can not be null for WebRequestUtils#convertToHttpServletRequest");
        if (request instanceof HttpServletRequest) {
            return (HttpServletRequest) request;
        }
        throw new IllegalArgumentException("Request is not an instance of HttpServletRequest");
    }

    /**
     * Convert method to convert <code>ServletResponse</code> to <code>HttpServletResponse</code>
     *
     * @param response <code>ServletResponse</code>
     * @return <code>HttpServletResponse</code>
     * @exception IllegalArgumentException exception will be thrown when response is null
     */
    private static HttpServletResponse toHttp(ServletResponse response) {
        Validate.notNull(response, "Response can not be null for WebRequestUtils#convertToHttpServletResponse");
        if (response instanceof HttpServletResponse) {
            return (HttpServletResponse) response;
        }
        throw new IllegalArgumentException("Response is not an instance of HttpServletResponse");
    }

    @Nullable
    public static HttpServletRequest getCurrentRequest() {
        try {
            return ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
        } catch (Exception e) {
            return null;
        }
    }


    @Nullable
    public static String getCurrentDomain() {
        HttpServletRequest currentRequest = WebRequestUtils.getCurrentRequest();
        if (currentRequest != null) {
            int remotePort = currentRequest.getRemotePort();
            String port = "";
            if (remotePort != 80) {
                port = ":" + currentRequest.getServerPort();
            }
            return currentRequest.getScheme() + "://" + currentRequest.getServerName() + port;
        }
        return null;
    }

}
