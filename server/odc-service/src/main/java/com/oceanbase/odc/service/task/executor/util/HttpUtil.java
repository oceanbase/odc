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

package com.oceanbase.odc.service.task.executor.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;

/**
 * @author gaoda.xy
 * @date 2023/11/30 17:25
 */
public class HttpUtil {

    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int SOCKET_TIMEOUT_SECONDS = 30;
    private static final String DEFAULT_METHOD = "POST";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final Map<String, String> DEFAULT_HEADERS = new HashMap<String, String>() {
        {
            put("Content-Type", "application/json");
        }
    };
    private static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
            .setConnectionRequestTimeout(CONNECT_TIMEOUT_SECONDS * 1000)
            .setConnectTimeout(CONNECT_TIMEOUT_SECONDS * 1000)
            .setSocketTimeout(SOCKET_TIMEOUT_SECONDS * 1000)
            .build();
    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClientBuilder.create()
            .setDefaultRequestConfig(DEFAULT_REQUEST_CONFIG)
            .build();

    public static <T> T request(String uri, String jsonBody, TypeReference<T> responseTypeRef) throws IOException {
        return request(DEFAULT_METHOD, uri, DEFAULT_HEADERS, null, jsonBody, responseTypeRef);
    }

    public static <T> T request(String method, String uri, Map<String, String> headers,
            Map<String, String> parameters, String jsonBody, TypeReference<T> responseTypeRef) throws IOException {
        RequestBuilder builder = RequestBuilder.create(method.toUpperCase())
                .setUri(uri)
                .setCharset(DEFAULT_CHARSET)
                .setVersion(HttpVersion.HTTP_1_1)
                .setConfig(DEFAULT_REQUEST_CONFIG);
        if (Objects.nonNull(headers)) {
            headers.forEach(builder::addHeader);
        }
        if (Objects.nonNull(parameters)) {
            parameters.forEach(builder::addParameter);
        }
        HttpEntity entity = EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setText(jsonBody)
                .build();
        builder.setEntity(entity);
        String response = DEFAULT_HTTP_CLIENT.execute(builder.build(), new BasicResponseHandler());
        return JsonUtils.fromJson(response, responseTypeRef);
    }

}
