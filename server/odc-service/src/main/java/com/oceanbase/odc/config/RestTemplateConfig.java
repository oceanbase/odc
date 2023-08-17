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
package com.oceanbase.odc.config;

import java.nio.charset.Charset;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter4;
import com.google.common.collect.ImmutableList;
import com.oceanbase.odc.core.shared.rest.OdcRestTemplate;
import com.oceanbase.odc.service.common.util.IgnoreResponseErrorHandler;

/**
 * @author yixun
 * @version 2020-02-25
 */
@Configuration
public class RestTemplateConfig {

    @Value("${server.port:8989}")
    private Integer serverPort;

    /**
     * internal proxy for aliyun generic api
     */
    @Bean("internalProxyRestTemplate")
    public RestTemplate internalProxyRestTemplate() {
        return new RestTemplateBuilder()
                .rootUri("http://localhost:" + serverPort)
                .setConnectTimeout(Duration.ofSeconds(1))
                .setReadTimeout(Duration.ofSeconds(60))
                .errorHandler(new IgnoreResponseErrorHandler())
                .build();
    }

    @Bean("vpcRestTemplate")
    public RestTemplate vpcRestTemplate() {
        StringHttpMessageConverter stringHttpMc = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        FastJsonHttpMessageConverter4 fastJsonHttpMc = new FastJsonHttpMessageConverter4();
        fastJsonHttpMc.setSupportedMediaTypes(ImmutableList
                .of(MediaType.APPLICATION_JSON_UTF8, MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA,
                        MediaType.APPLICATION_FORM_URLENCODED));
        RestTemplate restTemplate = new RestTemplateBuilder()
                .additionalMessageConverters(stringHttpMc, fastJsonHttpMc)
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        return new OdcRestTemplate(restTemplate, "ocpApi");
    }


    @Bean("omsRestTemplate")
    public RestTemplate omsRestTemplate() {
        RestTemplate build = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(1))
                .setReadTimeout(Duration.ofSeconds(60))
                .errorHandler(new IgnoreResponseErrorHandler())
                .build();
        return new OdcRestTemplate(build, "omsApi", false);
    }

}
