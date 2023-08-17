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
package com.oceanbase.odc.service.bastion.model;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "odc.integration.bastion")
public class BastionProperties {

    private EncryptionProperties encryption;
    private AccountProperties account;

    @Data
    public static class AccountProperties {
        private boolean autoLoginEnabled;
        private String mockUsername;
        private HttpProperties http;
        private QueryProperties query;
    }

    @Data
    public static class QueryProperties {
        private String requestUrl;
        private String requestMethod;
        private List<String> requestHeaders;
        private String requestBody;
        /**
         * Request 是否加密，默认加密
         */
        private Boolean requestEncrypted;
        private String responseBodyValidExpression;
        private String responseBodyUsernameExtractExpression;
        private String responseBodyNickNameExtractExpression;
        /**
         * Response 是否加密，默认加密
         */
        private Boolean responseEncrypted;
    }

    @Data
    public static class HttpProperties {
        private int connectTimeoutSeconds = 5;
        private int readTimeoutSeconds = 20;
    }

    @Data
    public static class EncryptionProperties {
        private boolean enabled;
        private EncryptionAlgorithm algorithm = EncryptionAlgorithm.RAW;
        private String secret;
    }

    public enum EncryptionAlgorithm {
        /**
         * 不加密
         */
        RAW,

        /**
         * AES256+BASE64
         */
        AES256_BASE64,

        /**
         * AES256+HEX
         */
        AES256_HEX,

        /**
         * 中国移动 4A 特有的加密方式 <br>
         * 非产品化功能，为兼容之前的 4A 集成方式
         */
        CMCC4A
    }

}
