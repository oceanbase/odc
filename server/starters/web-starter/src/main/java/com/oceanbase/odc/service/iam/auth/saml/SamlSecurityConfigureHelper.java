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
package com.oceanbase.odc.service.iam.auth.saml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.iam.auth.CustomAuthenticationFailureHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationSuccessHandler;

@Component
@Profile("alipay")
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValue = "local")
public class SamlSecurityConfigureHelper {


    @Autowired
    private RelyingPartyRegistrationRepository registrations;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    public void configure(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        http.saml2Login();
        Saml2WebSsoAuthenticationFilter saml2WebSsoAuthenticationFilter =
                new Saml2WebSsoAuthenticationFilter(registrations);
        saml2WebSsoAuthenticationFilter.setAuthenticationSuccessHandler(customAuthenticationSuccessHandler);
        saml2WebSsoAuthenticationFilter.setAuthenticationFailureHandler(customAuthenticationFailureHandler);
        saml2WebSsoAuthenticationFilter.setAuthenticationManager(authenticationManager);
        http.addFilterBefore(
                saml2WebSsoAuthenticationFilter,
                Saml2WebSsoAuthenticationFilter.class);
        http.addFilterBefore(
                new SamlTestLoginAuthenticationFilter(),
                Saml2WebSsoAuthenticationFilter.class);
    }
}
