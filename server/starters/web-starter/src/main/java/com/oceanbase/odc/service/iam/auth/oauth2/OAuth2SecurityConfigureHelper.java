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
package com.oceanbase.odc.service.iam.auth.oauth2;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.DelegatingOAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.iam.auth.CustomAuthenticationFailureHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationSuccessHandler;
import com.oceanbase.odc.service.integration.oauth2.AddableClientRegistrationManager;
import com.oceanbase.odc.service.integration.oauth2.SSOStateManager;
import com.oceanbase.odc.service.state.StatefulUuidStateIdGenerator;

@Component
@Profile("alipay")
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValue = "local")
public class OAuth2SecurityConfigureHelper {

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private OAuth2UserServiceImpl oAuth2UserServiceImpl;

    @Autowired
    private OidcUserServiceImpl oidcUserService;

    @Autowired
    private AddableClientRegistrationManager addableClientRegistrationManager;
    @Autowired
    private StatefulUuidStateIdGenerator statefulUuidStateIdGenerator;
    @Autowired
    private SSOStateManager SSOStateManager;


    public void configure(HttpSecurity http)
            throws Exception {
        http.oauth2Login()
                .successHandler(customAuthenticationSuccessHandler)
                .failureHandler(customAuthenticationFailureHandler)
                .authorizationEndpoint()
                .authorizationRequestResolver(
                        new CustomOAuth2AuthorizationRequestResolver(this.addableClientRegistrationManager,
                                statefulUuidStateIdGenerator, SSOStateManager))
                .and()
                // token 端点配置, 根据 code 获取 token
                .tokenEndpoint()
                .accessTokenResponseClient(new CustomOAuth2AccessTokenResponseClient())
                .and()
                // 用户信息端点配置, 根据accessToken获取用户基本信息
                .userInfoEndpoint()
                .userService(new DelegatingOAuth2UserService<>(
                        Arrays.asList(this.oAuth2UserServiceImpl, new DefaultOAuth2UserService())))
                .oidcUserService(oidcUserService);

        http.addFilterBefore(
                new OAuth2TestLoginAuthenticationFilter(),
                OAuth2LoginAuthenticationFilter.class);
    }
}
