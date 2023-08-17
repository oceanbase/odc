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

import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.AuthenticationInfo;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;

/**
 * Authenticator for security module, used to identify a subject(a user or an application) In
 * {@code java.security}, Authentication operation has two different kinds of service object.
 *
 * For one hand, code base can be a kind of authentication object, the remote code can not be
 * trusted.
 *
 * For another hand, user can be a kind of authentication object, a user can be trusted after be
 * authenticated. These two differents service objects can be treated as
 * {@link javax.security.auth.Subject}.
 *
 * For simplicity, we just treat this {@link javax.security.auth.Subject} as a user. A user may have
 * different identities(eg. engineer, student, father and etc.) Each identity can be verified or
 * authenticated by this object, the verified identity can be called {@link Principal}
 *
 * @author yh263208
 * @date 2021-07-08 15:13
 * @since ODC_release_3.2.0
 */
public interface Authenticator {
    /**
     * The caller call this method to verify or authenticate an identity for a
     * {@link javax.security.auth.Subject} Each {@link javax.security.auth.Subject} may have multi
     * {@link Principal}, but a single {@link Authenticator} can only authenticate one of these
     * {@link Principal}
     *
     * @param token token to authenticate
     * @return return the authenticate information
     * @exception AuthenticationException checked exception will be thrown when authenticate failed
     */
    <T extends Principal, V> AuthenticationInfo<T, V> authenticate(BaseAuthenticationToken<T, V> token)
            throws AuthenticationException;

    /**
     * Each {@link Authenticator} can only authenticate one {@link Principal}. This method is to
     * indicate whether the {@link Authenticator} can authenticate a certain {@link Principal}
     *
     * @param principal {@link Principal} that needed to be authenticated
     * @return Flag to indicate whether the {@link Authenticator} support the {@link Principal}
     */
    boolean supports(Class<? extends Principal> principal);

}
