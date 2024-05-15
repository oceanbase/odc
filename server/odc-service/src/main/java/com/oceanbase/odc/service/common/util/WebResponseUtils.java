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

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.UriUtils;

import com.google.common.net.InetAddresses;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.common.response.Error;
import com.oceanbase.odc.service.common.response.ErrorResponse;
import com.oceanbase.odc.service.common.response.OdcErrorResult;
import com.oceanbase.odc.service.common.response.Responses;

import lombok.extern.slf4j.Slf4j;

/**
 * Utils for <code>HttpServletResponse</code>
 *
 * @author yh263208
 * @date 2021-09-09 16:55
 * @since ODC_release_3.2.0
 */
@Slf4j
public class WebResponseUtils {
    private static final String DATA_URI_PREFIX_V1 = "/api/v1";
    private static final String DATA_URI_PREFIX_V2 = "/api/v2";
    private static final String XSRF_TOKEN_KEY = "XSRF-TOKEN";

    public static void writeBackLoginExpiredJson(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            Throwable exception, LocaleResolver localeResolver) throws IOException {
        // init locale for error message i18n
        Locale currentLocale = LocaleContextHolder.getLocale();
        try {
            Locale locale = localeResolver.resolveLocale(httpServletRequest);
            LocaleContextHolder.setLocale(locale);

            resetCookie(httpServletRequest, httpServletResponse, XSRF_TOKEN_KEY);

            if (httpServletRequest.getRequestURI().startsWith(DATA_URI_PREFIX_V1)) {
                WebResponseUtils.writeJsonObjectWithUnauthorizedStatus(OdcErrorResult.error(ErrorCodes.LoginExpired),
                        httpServletRequest, httpServletResponse);
            } else if (httpServletRequest.getRequestURI().startsWith(DATA_URI_PREFIX_V2)) {
                Error error = Error.of(ErrorCodes.LoginExpired);
                error.addDetail(exception);
                ErrorResponse errorResponse = Responses.error(HttpStatus.UNAUTHORIZED, error);
                errorResponse.setServer(SystemUtils.getHostName());
                WebResponseUtils.writeJsonObjectWithUnauthorizedStatus(errorResponse, httpServletRequest,
                        httpServletResponse);
            } else {
                ErrorResponse errorResponse = Responses.error(HttpStatus.NOT_FOUND, Error.of(ErrorCodes.NotFound));
                WebResponseUtils.writeJsonObjectWithStatus(errorResponse, httpServletRequest, httpServletResponse,
                        HttpServletResponse.SC_NOT_FOUND);
            }
        } finally {
            // recover to current locale
            LocaleContextHolder.setLocale(currentLocale);
        }
    }

    public static void resetCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        addCookie(request, response, cookie);
    }

    public static void addCookie(HttpServletRequest request, HttpServletResponse response, Cookie cookie) {
        String requestHost = WebRequestUtils.getRequestHost(request);
        String domain = InetAddresses.isInetAddress(requestHost) ? requestHost : calcParentDomain(requestHost);
        cookie.setDomain(domain);
        cookie.setPath(getRequestContext(request));
        response.addCookie(cookie);
    }

    public static <T> void writeJsonObjectWithOkStatus(T responseBody, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        writeJsonObjectWithStatus(responseBody, request, response, HttpServletResponse.SC_OK);
    }

    public static <T> void writeJsonObjectWithBadRequestStatus(T responseBody, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        writeJsonObjectWithStatus(responseBody, request, response, HttpServletResponse.SC_BAD_REQUEST);
    }

    public static <T> void writeJsonObjectWithUnauthorizedStatus(T responseBody, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        writeJsonObjectWithStatus(responseBody, request, response, HttpServletResponse.SC_UNAUTHORIZED);
    }

    public static <T> void writeJsonObjectWithStatus(T responseBody, HttpServletRequest request,
            HttpServletResponse response, int httpStatus) throws IOException {
        Validate.isTrue(httpStatus > 0, "HttpStatus can not be negative");
        Validate.notNull(response, "Response can not be null for WebResponseUtils#writeJsonObjectWithStatus");
        response.setStatus(httpStatus);
        writeJsonObject(responseBody, request, response);
    }

    public static <T> ResponseEntity<T> getStreamResponseEntity(T responseBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        ResponseEntity<T> response;
        response = ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(responseBody);
        return response;
    }

    public static <T> ResponseEntity<T> getFileAttachmentResponseEntity(T responseBody, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=" + UriUtils.encode(fileName, "UTF-8"));
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        ResponseEntity<T> response;
        response = ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/octet-stream;"))
                .body(responseBody);
        return response;
    }

    private static <T> void writeJsonObject(T responseBody, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Validate.notNull(responseBody, "ResposneBody can not be null for WebResponseUtils#writeJsonObject");
        Validate.notNull(request, "Request can not be null for WebResponseUtils#writeJsonObject");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        String body = JsonUtils.toJson(responseBody);
        response.getWriter().print(body);

        String userId = "Not-Required";
        String clientAddress = WebRequestUtils.getClientAddress(request);
        String userAgent = WebRequestUtils.getUserAgent(request);
        String fullURL = WebRequestUtils.getRequestFullURL(request);
        String method = "JSON";

        log.info("ODC_REQUEST_TRACE, httpMethod={}, fullURL={}, method={}, "
                + "clientAddress={}, userId={}, result={}, userAgent={}",
                request.getMethod(), fullURL, method, clientAddress, userId, body, userAgent);
    }

    private static String calcParentDomain(String host) {
        if (StringUtils.isEmpty(host)) {
            return host;
        }
        String[] items = host.split("\\.");
        return host.replace(items[0] + ".", "");
    }

    private static String getRequestContext(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return (contextPath.length() > 0) ? contextPath : "/";
    }
}
