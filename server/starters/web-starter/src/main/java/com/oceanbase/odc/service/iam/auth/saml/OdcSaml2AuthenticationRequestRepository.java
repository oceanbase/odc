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
import org.springframework.security.saml2.core.Saml2ParameterNames;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.integration.oauth2.SSOStateManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(value = {"odc.iam.auth.type"}, havingValue = "local")
public class OdcSaml2AuthenticationRequestRepository implements
        Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> {

    @Autowired
    private SSOStateManager ssoStateManager;

    @Override
    public AbstractSaml2AuthenticationRequest loadAuthenticationRequest(HttpServletRequest request) {

        String relayState = request.getParameter(Saml2ParameterNames.RELAY_STATE);
        if (relayState == null) {
            return null;
        }

        AbstractSaml2AuthenticationRequest authenticationRequest = ssoStateManager.getAuthenticationRequest(
                relayState);
        if (!authenticationRequest.getRelayState().equals(relayState)) {
            log.error("Relay State received from request '{}' is dfferent from saved request '{}'.", relayState,
                    authenticationRequest.getRelayState());
            return null;
        }

        log.debug("SAML2 Request retrieved : {}", authenticationRequest);
        return authenticationRequest;
    }

    @Override
    public void saveAuthenticationRequest(AbstractSaml2AuthenticationRequest authenticationRequest,
            HttpServletRequest request, HttpServletResponse response) {

        // As per OpenSamlAuthenticationRequestResolver, it will always have value. However, one validation
        // can be added to check for null and regenerate.
        String relayState = authenticationRequest.getRelayState();
        log.debug("Relay State Received: {}", relayState);
        ssoStateManager.saveAuthenticationRequest(relayState, authenticationRequest);
    }

    @Override
    public AbstractSaml2AuthenticationRequest removeAuthenticationRequest(HttpServletRequest request,
            HttpServletResponse response) {

        AbstractSaml2AuthenticationRequest authenticationRequest = loadAuthenticationRequest(request);
        if (authenticationRequest == null) {
            return null;
        }
        ssoStateManager.removeAuthenticationRequest(authenticationRequest.getRelayState());
        return authenticationRequest;
    }

}
