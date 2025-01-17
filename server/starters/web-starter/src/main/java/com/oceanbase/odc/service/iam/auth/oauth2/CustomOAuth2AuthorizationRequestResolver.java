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

import static com.oceanbase.odc.common.util.StringUtils.urlDecode;
import static com.oceanbase.odc.service.integration.oauth2.TestLoginManager.REGISTRATION_ID_URI_VARIABLE_NAME;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.info.OdcInfoService;
import com.oceanbase.odc.service.integration.model.Oauth2Parameter;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;
import com.oceanbase.odc.service.integration.oauth2.AddableClientRegistrationManager;
import com.oceanbase.odc.service.integration.oauth2.SSOStateManager;
import com.oceanbase.odc.service.state.StatefulUuidStateIdGenerator;

import lombok.SneakyThrows;

public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;

    private final AddableClientRegistrationManager addableClientRegistrationManager;
    private StatefulUuidStateIdGenerator statefulUuidStateIdGenerator;
    private SSOStateManager sSOStateManager;
    private AntPathRequestMatcher authorizationRequestMatcher;

    public CustomOAuth2AuthorizationRequestResolver(AddableClientRegistrationManager addableClientRegistrationManager,
            StatefulUuidStateIdGenerator statefulUuidStateIdGenerator, SSOStateManager sSOStateManager) {
        this.addableClientRegistrationManager = addableClientRegistrationManager;
        this.statefulUuidStateIdGenerator = statefulUuidStateIdGenerator;
        this.sSOStateManager = sSOStateManager;
        DefaultOAuth2AuthorizationRequestResolver defaultOAuth2AuthorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        addableClientRegistrationManager, "/oauth2/authorization");
        defaultOAuth2AuthorizationRequestResolver.setAuthorizationRequestCustomizer(customizer -> {
            customizer.state(statefulUuidStateIdGenerator.generateStateId("SSO_STATE"));
        });
        this.defaultAuthorizationRequestResolver = defaultOAuth2AuthorizationRequestResolver;

        this.authorizationRequestMatcher = new AntPathRequestMatcher(
                "/oauth2/authorization" + "/{" + REGISTRATION_ID_URI_VARIABLE_NAME + "}");

    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        String registrationId = resolveRegistrationId(request);
        return doResolve(request, registrationId);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        return doResolve(request, registrationId);
    }

    @SneakyThrows
    private OAuth2AuthorizationRequest doResolve(HttpServletRequest request, String registrationId) {
        if (registrationId == null) {
            return null;
        }
        SSOIntegrationConfig config = this.addableClientRegistrationManager.findConfigByRegistrationId(
                registrationId);
        Verify.verify(config.isOauth2OrOidc(), "Not matched sso Type, type=" + config.getType());
        OAuth2AuthorizationRequest authorizationRequest = this.defaultAuthorizationRequestResolver.resolve(request);
        if (authorizationRequest == null) {
            return null;
        }

        Oauth2Parameter ssoParameter = (Oauth2Parameter) config.getSsoParameter();
        Boolean useStateParams = ssoParameter.getUseStateParams();
        if (Boolean.TRUE.equals(useStateParams)) {
            String state = authorizationRequest.getState();
            String originRedirectUrl = authorizationRequest.getRedirectUri();
            UriComponentsBuilder.fromUriString(originRedirectUrl).build().getQueryParams()
                    .forEach((key, value) -> sSOStateManager.setStateParameter(state, key, urlDecode(value.get(0))));
            URL url = new URL(originRedirectUrl);
            String urlWithoutQuery = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath()).toString();
            sSOStateManager.setOdcParameters(state, request);

            return OAuth2AuthorizationRequest.from(authorizationRequest)
                    .redirectUri(urlWithoutQuery)
                    .state(state)
                    .build();
        }
        return customAuthorizationRequest(authorizationRequest, request);
    }

    private OAuth2AuthorizationRequest customAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request) {
        String redirectUrl = authorizationRequest.getRedirectUri();
        redirectUrl = OdcInfoService.addOdcParameter(request, redirectUrl);
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .redirectUri(redirectUrl)
                .build();
    }

    private String resolveRegistrationId(HttpServletRequest request) {
        if (authorizationRequestMatcher.matches(request)) {
            return authorizationRequestMatcher.matcher(request).getVariables()
                    .get(REGISTRATION_ID_URI_VARIABLE_NAME);
        }
        return null;
    }



}
