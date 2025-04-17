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

import static com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.parseRegistrationName;
import static com.oceanbase.odc.service.integration.oauth2.TestLoginManager.REGISTRATION_ID_URI_VARIABLE_NAME;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.core.Saml2ParameterNames;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.authentication.OpenSaml5AuthenticationRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationFailureHandler;
import com.oceanbase.odc.service.iam.auth.CustomAuthenticationSuccessHandler;
import com.oceanbase.odc.service.integration.oauth2.SSOStateManager;
import com.oceanbase.odc.service.state.StatefulUuidStateIdGenerator;

import jakarta.servlet.http.HttpServletRequest;

@Component
@Profile("alipay")
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValue = "local")
public class SamlSecurityConfigureHelper {


    public static final AntPathRequestMatcher samlAuthenticateRequestMatcher =
            new AntPathRequestMatcher("/saml2/authenticate/{registrationId}");
    @Autowired
    private RelyingPartyRegistrationRepository registrations;
    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    @Autowired
    private StatefulUuidStateIdGenerator statefulUuidStateIdGenerator;
    @Autowired
    private SSOStateManager ssoStateManager;

    public void configure(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        OpenSaml5AuthenticationRequestResolver resolver = new OpenSaml5AuthenticationRequestResolver(registrations);
        resolver.setRelayStateResolver(getRelayStateResolver());

        http.saml2Login(h -> {
            h.relyingPartyRegistrationRepository(registrations);
            h.successHandler(customAuthenticationSuccessHandler);
            h.failureHandler(customAuthenticationFailureHandler);
            h.authenticationManager(authenticationManager);
            h.authenticationRequestResolver(resolver);
        });

        http.addFilterBefore(
                new SamlTestLoginAuthenticationFilter(),
                Saml2WebSsoAuthenticationFilter.class);
    }

    private Converter<HttpServletRequest, String> getRelayStateResolver() {
        return (request) -> {
            HttpServletRequest currentRequest = WebRequestUtils.getCurrentRequest();
            // testLogin need save relate info as a key of relayState, some use testLogin state
            if (isSamlTestAuthenticateRequest(currentRequest)) {
                Verify.notNull(currentRequest, "currentRequest");
                String[] strings = currentRequest.getParameterMap().get(Saml2ParameterNames.RELAY_STATE);
                Verify.notNull(strings, "relayState");
                return strings[0];
            } else {
                return statefulUuidStateIdGenerator.generateStateId("SAML_STATEID");
            }
        };
    }

    private boolean isSamlTestAuthenticateRequest(HttpServletRequest request) {
        String registrationId = resolveSamlRegistrationId(request);
        if (registrationId == null) {
            return false;
        }
        return "test".equals(parseRegistrationName(registrationId));
    }

    private String resolveSamlRegistrationId(HttpServletRequest request) {
        if (samlAuthenticateRequestMatcher.matches(request)) {
            return samlAuthenticateRequestMatcher.matcher(request).getVariables()
                    .get(REGISTRATION_ID_URI_VARIABLE_NAME);
        }
        return null;
    }
}
