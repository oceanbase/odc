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
package com.oceanbase.odc.service.task.util;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author gaoda.xy
 * @date 2023/11/30 17:25
 */
@Slf4j
public class HttpClientUtils {
    private static final String APPLICATION_JSON = "application/json; charset=utf-8";

    private static final int CONNECT_TIMEOUT_SECONDS = 3;
    private static final int SOCKET_TIMEOUT_SECONDS = 30;

    private static OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

    public static void setHttpClient(OkHttpClient httpClient) {
        log.info("Set OkHttpClient, previous={}, current={}",
                HttpClientUtils.httpClient == null ? null : HttpClientUtils.httpClient.hashCode(),
                httpClient.hashCode());
        HttpClientUtils.httpClient = httpClient;
    }

    public static <T> T request(String method, String uri, TypeReference<T> responseTypeRef) throws IOException {
        return request(method, uri, null, null, null, responseTypeRef);
    }

    public static <T> T request(String method, String uri, String jsonBody, TypeReference<T> responseTypeRef)
            throws IOException {
        return request(method, uri, null, null, jsonBody, responseTypeRef);
    }

    public static <T> T request(@NonNull String method, @NonNull String uri, Map<String, String> headers,
            Map<String, String> parameters, String jsonBody, @NonNull TypeReference<T> responseTypeRef)
            throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(uri).newBuilder();
        if (Objects.nonNull(parameters)) {
            parameters.forEach(urlBuilder::addQueryParameter);
        }
        String requestUrl = urlBuilder.build().toString();
        RequestBody requestBody = jsonBody == null ? null
                : RequestBody.create(jsonBody, MediaType.parse(APPLICATION_JSON));

        Headers.Builder headersBuilder = new Headers.Builder();
        if (Objects.nonNull(headers)) {
            headers.forEach(headersBuilder::add);
        }
        Headers requestHeaders = headersBuilder.build();

        Request request = new Builder()
                .url(requestUrl)
                .headers(requestHeaders)
                .method(method, requestBody)
                .build();

        try (Response response = HttpClientUtils.httpClient.newCall(request).execute()) {
            ResponseBody responseBody = Objects.requireNonNull(response.body(), "response.body is null");
            if (!response.isSuccessful()) {
                throw new IOException("Request failed with code: " + response.code()
                        + ", message: " + response.message()
                        + (responseBody.contentLength() > 0 ? ", response: " + responseBody.string() : ""));
            }
            String responseBodyJson = responseBody.string();
            return JsonUtils.fromJson(responseBodyJson, responseTypeRef);
        }

    }

}
