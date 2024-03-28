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
package com.oceanbase.odc.service.iam.auth;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import lombok.Getter;

public class AttemptableUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {
    @Getter
    private final String loginAttemptKey;

    public AttemptableUsernamePasswordAuthenticationToken(Object principal, Object credentials,
            String loginAttemptKey) {
        super(principal, credentials);
        this.loginAttemptKey = loginAttemptKey;
    }

    public AttemptableUsernamePasswordAuthenticationToken(Object principal, Object credentials,
            Collection<? extends GrantedAuthority> authorities,
            String loginAttemptKey) {
        super(principal, credentials, authorities);
        this.loginAttemptKey = loginAttemptKey;
    }


    public static AttemptableUsernamePasswordAuthenticationToken authenticated(
            UsernamePasswordAuthenticationToken authenticated,
            String loginAttemptKey) {
        return new AttemptableUsernamePasswordAuthenticationToken(authenticated.getPrincipal(),
                authenticated.getCredentials(),
                authenticated.getAuthorities(), loginAttemptKey);
    }
}
