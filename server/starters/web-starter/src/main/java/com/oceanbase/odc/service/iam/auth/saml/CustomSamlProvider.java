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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.iam.model.User;

/**
 * why ues OpenSamlAuthenticationProvider instead of OpenSaml4AuthenticationProvider ? according to
 * {@link "https://github.com/spring-projects/spring-security/issues/11434"},
 * OpenSaml4AuthenticationProvider depends on opensaml 4.1+, which requires jdk11. In the current
 * version is loaded by default implementation OpenSaml4AuthenticationProvider, although it has been
 * deprecated.
 */
public class CustomSamlProvider implements AuthenticationProvider {

    private final AuthenticationProvider defaultAuthenticationProvider;

    private final DefaultSamlUserService defaultSamlUserService;

    public CustomSamlProvider(DefaultSamlUserService defaultSamlUserService) {
        this.defaultSamlUserService = defaultSamlUserService;
        defaultAuthenticationProvider = new OpenSamlAuthenticationProvider();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication authenticate = defaultAuthenticationProvider.authenticate(authentication);
        Verify.verify(authenticate instanceof Saml2Authentication,
                "invalid type of authentication, class: " + authentication.getClass());
        Saml2Authentication saml2Authentication = (Saml2Authentication) authenticate;
        User user = defaultSamlUserService.loadUser(saml2Authentication);
        return new Saml2Authentication(user, ((Saml2Authentication) authenticate).getSaml2Response(),
                authenticate.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return defaultAuthenticationProvider.supports(authentication);
    }
}
