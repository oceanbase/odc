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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.context.SecurityContextRepository;

import com.oceanbase.odc.service.iam.auth.CustomJwtLogoutSuccessHandler;
import com.oceanbase.odc.service.iam.auth.CustomPostRequestSessionInvalidationFilter;
import com.oceanbase.odc.service.iam.auth.JwtSecurityContextRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zj.cj
 * @date 2024-03-19 21:58
 * @since ODC_release_4.2.4
 */
@Slf4j
@Profile("alipay")
@Configuration
@ConditionalOnExpression("#{@environment.getProperty('odc.iam.auth.type') == 'local' && @environment.getProperty('odc.iam.auth.method') == 'jwt'}")
public class WebJwtSecurityConfiguration extends WebJSessionSecurityConfiguration {

    @Autowired
    private JwtSecurityContextRepository jwtSecurityContextRepository;
    @Autowired
    private CustomJwtLogoutSuccessHandler customJwtLogoutSuccessHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.addFilterBefore(new CustomPostRequestSessionInvalidationFilter(), SecurityContextPersistenceFilter.class);
    }

    @Override
    protected SecurityContextRepository securityContextRepository() {
        return this.jwtSecurityContextRepository;
    }

    @Override
    protected LogoutSuccessHandler logoutSuccessHandler() {
        return this.customJwtLogoutSuccessHandler;
    }

    @Override
    protected void configHttpSession(HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

}
