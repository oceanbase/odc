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
package com.oceanbase.odc.core.authority.auth;

import java.security.Principal;
import java.util.Collection;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;

/**
 * Since there are multiple {@link Authenticator}, a {@link AuthenticatorManager} is needed to unify
 * the verification behavior of each {@link Authenticator}, including matching specific identities
 * with {@link Authenticator}, managing authenticate behaviors, etc.
 *
 * @author yh263208
 * @date 2021-07-20 14:31
 * @since ODC_release_3.2.0
 */
public interface AuthenticatorManager {
    /**
     * Authenticate method, since there may be multiple authenticate credentials, the final result is a
     * {@link Subject} that has been verified
     *
     * @param tokens collection of {@link BaseAuthenticationToken}
     * @exception AuthenticationException exception may be thrown when authenticate failed
     */
    Subject authenticate(Collection<BaseAuthenticationToken<? extends Principal, ?>> tokens)
            throws AuthenticationException;

}
