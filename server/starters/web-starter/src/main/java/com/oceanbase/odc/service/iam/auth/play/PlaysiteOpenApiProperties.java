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
package com.oceanbase.odc.service.iam.auth.play;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2021/12/18 下午4:05
 * @Description: [This is a configuration class for loading ob official website open api properties]
 */
@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "odc.iam.auth.alipay.openapi")
public class PlaysiteOpenApiProperties {
    /**
     * 调用 Alipay OpenAPI 配置项
     */
    private String appId;
    private String serverUrl;
    private String privateKey;
    private String alipayPublicKey;
    private String signType;
    private String format;
    private String charset;

    /**
     * 官网域名
     */
    private String obOfficialDomain;

    /**
     * 官网登录 URL
     */
    private String obOfficialLoginUrl;

    /**
     * 官网登出 URL
     */
    private String obOfficialLogoutUrl;
}
