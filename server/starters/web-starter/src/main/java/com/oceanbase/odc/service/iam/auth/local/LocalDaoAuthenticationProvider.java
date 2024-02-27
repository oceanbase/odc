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
package com.oceanbase.odc.service.iam.auth.local;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.service.iam.auth.AttemptableUsernamePasswordAuthenticationToken;

public class LocalDaoAuthenticationProvider extends DaoAuthenticationProvider {

    public LocalDaoAuthenticationProvider() {
        super();
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(AttemptableUsernamePasswordAuthenticationToken.class);
    }

    @Override
    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
            UserDetails user) {
        Authentication successAuthentication = super.createSuccessAuthentication(principal, authentication, user);
        Preconditions.checkArgument(successAuthentication instanceof UsernamePasswordAuthenticationToken);
        Preconditions.checkArgument(authentication instanceof AttemptableUsernamePasswordAuthenticationToken);
        return AttemptableUsernamePasswordAuthenticationToken.authenticated(
                (UsernamePasswordAuthenticationToken) successAuthentication,
                ((AttemptableUsernamePasswordAuthenticationToken) authentication).getLoginAttemptKey());
    }
}
