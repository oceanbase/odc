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

import java.security.Principal;

import com.oceanbase.odc.core.authority.auth.Authenticator;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.AuthenticationInfo;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;
import com.oceanbase.odc.service.iam.model.User;

/**
 * Basic <code>Authenticator</code> just for ODC application
 *
 * @author yh263208
 * @date 2021-08-02 16:19
 * @since ODC-release_3.2.0
 */
public class EmptyAuthenticator implements Authenticator {

    @Override
    public <T extends Principal, V> AuthenticationInfo<T, V> authenticate(BaseAuthenticationToken<T, V> token)
            throws AuthenticationException {
        if (token.getPrincipal() == null) {
            throw new AuthenticationException("Principal is null");
        }
        return token;
    }

    @Override
    public boolean supports(Class<? extends Principal> principal) {
        return User.class.isAssignableFrom(principal);
    }

}
