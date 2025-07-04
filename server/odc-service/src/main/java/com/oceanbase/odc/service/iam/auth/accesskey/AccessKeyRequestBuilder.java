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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import lombok.Data;

public class AccessKeyRequestBuilder {

    private static String getCurrentRfcDate() {
        return ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private static HttpHeaders createRequestHeaders(String accessKeyId, String secretAccessKey, String host,
            String path, HttpMethod method, byte[] body, Map<String, String> queryParams,
            Map<String, String> customHeaders, String algorithm) {
        HttpHeaders headers = new HttpHeaders();

        String rfcDate = getCurrentRfcDate();

        headers.set("Host", host);
        headers.set("Date", rfcDate);

        String nonce = SecureSignatureUtils.generateNonce();
        headers.set("x-odc-nonce", nonce);

        if (customHeaders != null) {
            customHeaders.forEach(headers::set);
        }

        String signature = SecureSignatureUtils.generateSecureSignature(
                host, path, method.name(), queryParams, body,
                secretAccessKey, rfcDate, nonce, algorithm);

        String authHeader = String.format("ODC-ACCESS-KEY-%s %s:%s", algorithm, accessKeyId, signature);
        headers.set("Authorization", authHeader);

        return headers;
    }



    @Data
    public static class RequestConfig {
        private String accessKeyId;
        private String secretAccessKey;
        private String host;
        private String path;
        private HttpMethod method;
        private byte[] body;
        private Map<String, String> queryParams;
        private Map<String, String> customHeaders;
        private String algorithm;
        private String nonce;

        public RequestConfig() {
            this.algorithm = "HMACSHA256";
        }

        public RequestConfig(String accessKeyId, String secretAccessKey, String host, String path, HttpMethod method) {
            this();
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
            this.host = host;
            this.path = path;
            this.method = method;
        }

        public HttpHeaders buildHeaders() {
            return createRequestHeaders(accessKeyId, secretAccessKey, host, path, method, body, queryParams,
                    customHeaders, algorithm);
        }

        public RequestConfig withQueryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public RequestConfig withCustomHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public RequestConfig withAlgorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public RequestConfig withBody(byte[] body) {
            this.body = body;
            return this;
        }
    }
}
