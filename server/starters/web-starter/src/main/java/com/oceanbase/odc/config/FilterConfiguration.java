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

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

import com.oceanbase.odc.server.web.trace.WebRequestBodyCopyFilter;

@Configuration
@Profile("alipay")
public class FilterConfiguration {

    /**
     * Register WebRequestBodyCopyFilter with high priority to ensure it executes before
     * SecurityFilterChain, so that AccessKeyAuthenticationFilter can correctly read the request body
     */
    @Bean
    public FilterRegistrationBean<WebRequestBodyCopyFilter> webRequestBodyCopyFilter() {
        FilterRegistrationBean<WebRequestBodyCopyFilter> registrationBean =
                new FilterRegistrationBean<>();

        registrationBean.setFilter(new WebRequestBodyCopyFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setName("webRequestBodyCopyFilter");

        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);

        return registrationBean;
    }
}
