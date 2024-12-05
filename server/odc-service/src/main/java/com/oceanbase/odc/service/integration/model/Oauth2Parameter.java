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

import static com.oceanbase.odc.core.shared.constant.OdcConstants.ODC_BACK_URL_PARAM;
import static com.oceanbase.odc.service.integration.model.SSOIntegrationConfig.parseOrganizationId;

import java.net.URL;
import java.util.Set;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.integration.oauth2.SSOStateManager;

import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;

@Data
public class Oauth2Parameter implements SSOParameter {

    /**
     * orgId:name
     */
    private String registrationId;
    private String name;
    @NonNull
    private String clientId;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String secret;
    private String authUrl;
    private String tokenUrl;
    private String userInfoUrl;
    @NonNull
    private Set<String> scope;
    private String jwkSetUri;

    private String logoutUrl;
    private String clientAuthenticationMethod;
    private String authorizationGrantType;
    private String userInfoAuthenticationMethod;
    private String redirectUrl;
    private String userNameAttribute;
    private String loginRedirectUrl;

    /**
     * {@link Oauth2Parameter}
     * 
     * @see SSOStateManager put redirect paramters into Oauth2StateManager's cache, default value false
     *      to adaptive history data
     */
    private Boolean useStateParams = true;

    public Oauth2Parameter() {}

    @SneakyThrows
    private static String getRedirectHost(String redirectUrl) {
        URL url = new URL(redirectUrl);
        StringBuilder host = new StringBuilder();
        host.append(url.getProtocol()).append("://").append(url.getHost());
        int port = url.getPort();
        if (port <= 0) {
            return host.toString();
        }
        if ("http".equals(url.getProtocol()) && port != 80 ||
                "https".equals(url.getProtocol()) && port != 443) {
            host.append(":").append(url.getPort());
        }
        return host.toString();
    }

    private static Builder getBuilder(Builder builder, Provider provider) {
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        map.from(provider::getAuthorizationUri).to(builder::authorizationUri);
        map.from(provider::getTokenUri).to(builder::tokenUri);
        map.from(provider::getUserInfoUri).to(builder::userInfoUri);
        map.from(provider::getUserInfoAuthenticationMethod).as(AuthenticationMethod::new)
                .to(builder::userInfoAuthenticationMethod);
        map.from(provider::getJwkSetUri).to(builder::jwkSetUri);
        map.from(provider::getUserNameAttribute).to(builder::userNameAttributeName);
        return builder;
    }

    public void fillParameter() {
        if (loginRedirectUrl == null) {
            loginRedirectUrl = "/oauth2/authorization/" + registrationId;
        }
    }

    public void amendTestParameter(String testType) {
        registrationId = parseOrganizationId(registrationId) + "-" + "test";
        String odcBackUrl = UrlUtils.getQueryParameterFirst(redirectUrl, ODC_BACK_URL_PARAM);
        redirectUrl = getRedirectHost(redirectUrl) + "/login/oauth2/code/" + registrationId;
        loginRedirectUrl = "/oauth2/authorization/" + registrationId;
        redirectUrl = UrlUtils.appendQueryParameter(redirectUrl, OdcConstants.TEST_LOGIN_TYPE, testType);
        redirectUrl = UrlUtils.appendQueryParameter(redirectUrl, ODC_BACK_URL_PARAM, odcBackUrl);
    }

    public ClientRegistration toClientRegistration() {
        return toClientRegistration(null);
    }

    private Provider toProvider(String issueUrl) {
        Provider provider = new Provider();
        provider.setAuthorizationUri(authUrl);
        provider.setJwkSetUri(jwkSetUri);
        provider.setTokenUri(tokenUrl);
        provider.setUserInfoUri(userInfoUrl);
        provider.setUserNameAttribute(userNameAttribute);
        provider.setUserInfoAuthenticationMethod(userInfoAuthenticationMethod);
        provider.setIssuerUri(issueUrl);
        return provider;
    }

    protected ClientRegistration toClientRegistration(String issueUrl) {
        Builder builder = getBuilderFromIssuerIfPossible(toProvider(issueUrl));
        if (builder == null) {
            builder = getBuilder(ClientRegistration.withRegistrationId(registrationId), toProvider(issueUrl));
        }
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        map.from(this::getClientId).to(builder::clientId);
        map.from(this::getSecret).to(builder::clientSecret);
        map.from(this::getClientAuthenticationMethod).as(ClientAuthenticationMethod::new)
                .to(builder::clientAuthenticationMethod);
        map.from(this::getAuthorizationGrantType).as(AuthorizationGrantType::new)
                .to(builder::authorizationGrantType);
        map.from(this::getRedirectUrl).to(builder::redirectUri);
        map.from(this::getScope).as(org.springframework.util.StringUtils::toStringArray).to(builder::scope);
        map.from(this::getName).to(builder::clientName);
        return builder.build();
    }

    private Builder getBuilderFromIssuerIfPossible(Provider provider) {
        String issuer = provider.getIssuerUri();
        if (issuer != null) {
            Builder builder =
                    ClientRegistrations.fromIssuerLocation(issuer).registrationId(registrationId);
            return getBuilder(builder, provider);
        }
        return null;
    }
}
