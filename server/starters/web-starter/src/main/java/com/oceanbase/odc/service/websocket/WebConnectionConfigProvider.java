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
package com.oceanbase.odc.service.websocket;

import java.security.Principal;

import javax.websocket.Session;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.bastion.BastionAuthenticationToken;

@Component
@Profile("alipay")
public class WebConnectionConfigProvider extends DefaultConnectionConfigProvider {

    @Override
    public ConnectionConfig getConnectionSession(String resourceId, Session session) {
        Principal principal = session.getUserPrincipal();
        if (principal == null) {
            throw new AccessDeniedException();
        }
        if (principal instanceof PreAuthenticatedAuthenticationToken) {
            PreAuthenticatedAuthenticationToken preAuthenticatedAuthenticationToken =
                    (PreAuthenticatedAuthenticationToken) principal;
            SecurityContextHolder.getContext().setAuthentication(preAuthenticatedAuthenticationToken);
        } else if (principal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    (UsernamePasswordAuthenticationToken) principal;
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            SecurityContextHolder.getContext().setAuthentication(oAuth2AuthenticationToken);
        } else if (principal instanceof BastionAuthenticationToken) {
            BastionAuthenticationToken bastionAuthenticationToken = (BastionAuthenticationToken) principal;
            SecurityContextHolder.getContext().setAuthentication(bastionAuthenticationToken);
        } else {
            throw new UnexpectedException("Unexpected principal type:" + principal.getClass().getSimpleName());
        }
        return super.getConnectionSession(resourceId, session);
    }

}
