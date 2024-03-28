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
package com.oceanbase.odc.service.iam.auth.ldap;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.service.iam.auth.AttemptableUsernamePasswordAuthenticationToken;

public class ODCLdapAuthenticationProvider extends LdapAuthenticationProvider {

    public ODCLdapAuthenticationProvider(LdapAuthenticator authenticator,
            UserDetailsContextMapper userDetailsContextMapper) {
        super(authenticator);
        setUserDetailsContextMapper(userDetailsContextMapper);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return LdapPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected Authentication createSuccessfulAuthentication(UsernamePasswordAuthenticationToken authentication,
            UserDetails user) {
        Authentication successfulAuthentication = super.createSuccessfulAuthentication(authentication, user);
        Preconditions.checkArgument(successfulAuthentication instanceof UsernamePasswordAuthenticationToken);
        Preconditions.checkArgument(authentication instanceof AttemptableUsernamePasswordAuthenticationToken);
        return AttemptableUsernamePasswordAuthenticationToken.authenticated(
                (UsernamePasswordAuthenticationToken) successfulAuthentication,
                ((AttemptableUsernamePasswordAuthenticationToken) authentication).getLoginAttemptKey());
    }

}
