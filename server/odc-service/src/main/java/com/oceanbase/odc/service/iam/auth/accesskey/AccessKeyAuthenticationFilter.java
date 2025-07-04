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
package com.oceanbase.odc.service.iam.auth.accesskey;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.iam.AccessKeyEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.iam.AccessKeyService;
import com.oceanbase.odc.service.iam.model.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccessKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final long MAX_TIME_SKEW_SECONDS = 300L;

    private final AccessKeyService accessKeyService;
    private final UserRepository userRepository;


    public AccessKeyAuthenticationFilter(AccessKeyService accessKeyService, UserRepository userRepository) {
        this.accessKeyService = accessKeyService;
        this.userRepository = userRepository;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (WebRequestUtils.isAccessKeyAuth(request)) {
            try {
                AuthenticationParam param = parse(request);

                validateSignature(param);

                User user = convert(param);
                request.setAttribute("currentOrganizationId", user.getOrganizationId());
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));
            } catch (AuthenticationException e) {
                log.warn("AccessKey authentication failed: {}", e.getMessage());
                throw e;
            }
        }

        filterChain.doFilter(request, response);

    }


    protected AuthenticationParam parse(HttpServletRequest request) throws IOException {
        String headerAuthorization = request.getHeader("Authorization");
        if (StringUtils.isEmpty(headerAuthorization)) {
            throw new BadCredentialsException("Missing Authorization header");
        }

        SecureSignatureUtils.AuthorizationInfo authInfo =
                SecureSignatureUtils.parseAuthorizationHeader(headerAuthorization);
        if (authInfo == null) {
            throw new BadCredentialsException("Invalid Authorization header format");
        }

        String headerDate = getRequestDate(request);
        if (!SecureSignatureUtils.isTimestampValid(headerDate, MAX_TIME_SKEW_SECONDS)) {
            throw new BadCredentialsException("Request timestamp expired or invalid");
        }

        String nonce = request.getHeader("x-odc-nonce");
        if (nonce == null) {
            throw new BadCredentialsException("Missing nonce parameter");
        }

        byte[] body = readRequestBody(request);

        return AuthenticationParam.builder()
                .host(request.getHeader("Host"))
                .path(request.getRequestURI())
                .method(request.getMethod())
                .queryParams(parseQueryParams(request))
                .algorithm(authInfo.getAlgorithm())
                .accessKeyId(authInfo.getAccessKeyId())
                .signature(authInfo.getSignature())
                .headerDate(headerDate)
                .body(body)
                .nonce(nonce)
                .build();
    }


    protected byte[] readRequestBody(HttpServletRequest request) throws IOException {
        if (WebRequestUtils.isMultipart(request)) {
            return null;
        }

        try (var inputStream = request.getInputStream()) {
            if (inputStream != null) {
                byte[] byteArray = IOUtils.toByteArray(inputStream);
                if (byteArray.length == 0) {
                    return null;
                }
                return byteArray;
            }
        }
        return null;
    }


    protected String getRequestDate(HttpServletRequest request) {
        String headerDate = request.getHeader("x-odc-date");
        if (StringUtils.isEmpty(headerDate)) {
            headerDate = request.getHeader("Date");
        }
        if (StringUtils.isEmpty(headerDate)) {
            throw new BadCredentialsException("Missing Date or x-odc-date header");
        }
        return headerDate;
    }



    protected void validateSignature(AuthenticationParam param) {
        try {
            String accessKeySecret = getAccessKeySecret(param.getAccessKeyId());
            if (StringUtils.isEmpty(accessKeySecret)) {
                throw new BadCredentialsException("Invalid AccessKey ID");
            }

            boolean isValid = SecureSignatureUtils.verifySignature(param, accessKeySecret);

            if (!isValid) {
                throw new BadCredentialsException("Invalid signature");
            }
            log.debug("Signature validation passed for AccessKey: {}", param.getAccessKeyId());
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during signature validation", e);
            throw new BadCredentialsException("Signature validation failed");
        }
    }



    protected Map<String, String> parseQueryParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();

        String queryString = request.getQueryString();
        if (StringUtils.isEmpty(queryString)) {
            return params;
        }

        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            processQueryParam(pair, params);
        }
        return params;
    }

    private void processQueryParam(String pair, Map<String, String> params) {
        if (StringUtils.isEmpty(pair)) {
            return;
        }

        try {
            String[] keyValue = pair.split("=", 2);
            String key = decodeQueryParam(keyValue[0]);

            if (StringUtils.isEmpty(key)) {
                return;
            }
            String value = keyValue.length > 1 ? decodeQueryParam(keyValue[1]) : "";
            addParamToMap(params, key, value);
        } catch (Exception e) {
            log.warn("Failed to decode query parameter: {}", pair, e);
        }
    }

    private String decodeQueryParam(String param) {
        return URLDecoder.decode(param, StandardCharsets.UTF_8);
    }

    private void addParamToMap(Map<String, String> params, String key, String value) {
        if (params.containsKey(key)) {
            params.compute(key, (k, existingValue) -> existingValue + "," + value);
        } else {
            params.put(key, value);
        }
    }

    protected String getAccessKeySecret(String accessKeyId) {
        return accessKeyService.getDecryptAccessKey(accessKeyId);
    }

    protected User convert(AuthenticationParam param) {
        AccessKeyEntity accessKeyEntity = accessKeyService.getByAccessKeyId(param.getAccessKeyId()).orElseThrow(
                NullPointerException::new);
        UserEntity userEntity = userRepository.findById(accessKeyEntity.getUserId()).orElseThrow(
                NullPointerException::new);
        User user = new User(userEntity);
        TraceContextHolder.setUserId(user.getId());
        TraceContextHolder.setOrganizationId(user.getOrganizationId());
        return user;
    }

}
