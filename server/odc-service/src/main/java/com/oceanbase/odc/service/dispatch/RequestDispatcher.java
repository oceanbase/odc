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
package com.oceanbase.odc.service.dispatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.InternalServerError;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispatcher for {@code RPC}
 *
 * @author yh263208
 * @date 2022-03-24 16:35
 * @since ODC_release_3.3.0
 */
@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class RequestDispatcher {

    private final static String PROTOCAL = "http";
    private final static String TTL_HEADER_NAME = "ODC-RPC-TTL";
    @Value("${odc.rpc.max-ttl:3}")
    private String maxTtl;
    @Autowired
    private HttpRequestProvider requestProvider;
    @Autowired
    private DispatchProperties dispatchProperties;

    public DispatchResponse forward(@NonNull String ip, @NonNull Integer port) throws IOException {
        HttpServletRequest request = requestProvider.getRequest();
        Verify.notNull(request, "HttpServletRequest");

        StringBuilder uriBuilder = new StringBuilder(request.getRequestURI());
        Map<String, String[]> parametersMap = request.getParameterMap();
        if (parametersMap.size() != 0) {
            uriBuilder.append("?");
            List<String> parameters = new LinkedList<>();
            for (Map.Entry<String, String[]> entry : parametersMap.entrySet()) {
                String parameter = entry.getKey();
                for (String value : entry.getValue()) {
                    parameters.add(parameter + "=" + encode(value, request.getCharacterEncoding()));
                }
            }
            uriBuilder.append(String.join("&", parameters));
        }
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders headers = getRequestHeaders(request);
        ByteArrayOutputStream outputStream = requestProvider.getRequestBody();
        if (outputStream == null) {
            return forward(ip, port, method, uriBuilder.toString(), headers, null);
        }
        return forward(ip, port, method, uriBuilder.toString(), headers, outputStream.toByteArray());
    }

    public DispatchResponse forward(@NonNull String ip, @NonNull Integer port, @NonNull HttpMethod method,
            @NonNull String requestUri, @NonNull HttpHeaders headers, byte[] requestBody) throws IOException {
        verifyAndReduceTtl(headers);
        String realUri = generateRealUri(ip, port, requestUri);
        log.info("Request dispatch starts, uri={}", realUri);
        HttpHeaders responseHeaders = new HttpHeaders();
        RestTemplate restTemplate = dispatchRestTemplate();
        ByteArrayInputStream inputStream = restTemplate.execute(URI.create(realUri), method, clientRequest -> {
            clientRequest.getHeaders().addAll(headers);
            if (requestBody == null) {
                return;
            }
            IOUtils.write(requestBody, clientRequest.getBody());
        }, clientResponse -> {
            responseHeaders.addAll(clientResponse.getHeaders());
            return new ByteArrayInputStream(IOUtils.toByteArray(clientResponse.getBody()));
        });
        Verify.notNull(inputStream, "CallResult");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, outputStream);
        return DispatchResponse.of(outputStream.toByteArray(), responseHeaders);
    }

    public HttpHeaders getRequestHeaders() {
        return getRequestHeaders(requestProvider.getRequest());
    }

    private void verifyAndReduceTtl(HttpHeaders httpHeaders) {
        List<String> ttl = httpHeaders.get(TTL_HEADER_NAME);
        if (CollectionUtils.isEmpty(ttl)) {
            httpHeaders.add(TTL_HEADER_NAME, maxTtl);
        } else if (ttl.size() == 1) {
            int ttlInt = Integer.parseInt(ttl.get(0));
            ttlInt--;
            if (ttlInt < 0) {
                log.warn("Please check deployment configuration, e.g. ODC_HOST");
                throw new BadRequestException(ErrorCodes.BadRequest,
                        new Object[] {"TTL has been exhausted, loopback call may be generated, please contact admin"},
                        "TTL is exhausted, " + ttlInt);
            }
            httpHeaders.put(TTL_HEADER_NAME, Collections.singletonList(ttlInt + ""));
        } else {
            throw new IllegalStateException("Multi-TTL headers value, " + ttl);
        }
    }

    private String generateRealUri(String ip, Integer port, @NonNull String uri) {
        URI requestUri = URI.create(uri);
        String parameters = requestUri.getRawQuery() == null ? "" : "?" + requestUri.getRawQuery();
        String rawPath = requestUri.getRawPath();
        rawPath = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
        return String.format("%s://%s:%s%s%s", PROTOCAL, ip, port, rawPath, parameters);
    }

    private HttpHeaders getRequestHeaders(@NonNull HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String value = request.getHeader(headerName);
            headers.add(headerName, value);
        }
        return headers;
    }

    private String encode(@NonNull String value, @NonNull String encoding) {
        try {
            return URLEncoder.encode(value, encoding);
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to encode value, value={}, encoding={}", value, encoding, e);
            throw new InternalServerError(e.getMessage());
        }
    }

    private RestTemplate dispatchRestTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(dispatchProperties.getConnectTimeoutSeconds()))
                .setReadTimeout(Duration.ofSeconds(dispatchProperties.getReadTimeoutSeconds()))
                .errorHandler(new IgnoreErrorHandler())
                .build();
    }

}
