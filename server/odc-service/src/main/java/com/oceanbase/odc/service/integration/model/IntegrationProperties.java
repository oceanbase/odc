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
package com.oceanbase.odc.service.integration.model;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * Common properties for SQL interceptor and approval integration
 * 
 * @author gaoda.xy
 * @date 2023/4/11 10:01
 */
@Data
public class IntegrationProperties {
    @NotNull
    private HttpProperties http;
    @Valid
    @NotNull
    private Encryption encryption;

    @Data
    public static class HttpProperties {
        private int connectTimeoutSeconds = 5;
        private int socketTimeoutSeconds = 30;
    }

    @Data
    public static class ApiProperties {
        private RequestMethod method = RequestMethod.POST;
        @NotNull
        private String url;
        private Map<String, String> headers;
        private Map<String, String> queryParameters;
        private Body body;
        private boolean requestEncrypted = false;
        private boolean responseEncrypted = false;
        private ResponseContentType responseContentType = ResponseContentType.JSON;
        @NotBlank
        private String requestSuccessExpression = "true";
    }

    @Data
    public static class Body {
        private BodyType type = BodyType.RAW;
        private Object content;
    }

    public enum BodyType {
        RAW,
        FORM_DATA
    }

    public enum ResponseContentType {
        JSON,
        XML
    }

    public enum RequestMethod {
        GET,
        POST,
        PUT,
        PATCH
    }
}
