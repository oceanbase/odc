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
package com.oceanbase.odc.core.authority;

import java.security.Principal;
import java.util.Collection;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.auth.AuthenticatorManager;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;

import lombok.Getter;
import lombok.NonNull;

/**
 * Abstract validator security manager {@link BaseAuthenticatorSecurityManager}, used to handle the
 * management work related to the {@link AuthenticatorManager}
 *
 * @author yh263208
 * @date 2021-07-21 16:33
 * @since ODC_release_3.2.0
 * @see SecurityManager
 */
abstract class BaseAuthenticatorSecurityManager implements SecurityManager {

    @Getter
    private final AuthenticatorManager authenticatorManager;

    public BaseAuthenticatorSecurityManager(@NonNull AuthenticatorManager authenticatorManager) {
        this.authenticatorManager = authenticatorManager;
    }

    /**
     * Authenticate method, since there may be multiple authenticate credentials, the final result is a
     * {@link Subject} that has been verified
     *
     * @param tokens collection of {@link BaseAuthenticationToken}
     * @throws AuthenticationException exception may be thrown when authenticate failed
     */
    @Override
    public Subject authenticate(Collection<BaseAuthenticationToken<? extends Principal, ?>> tokens)
            throws AuthenticationException {
        return this.authenticatorManager.authenticate(tokens);
    }

    @Override
    public void close() throws Exception {
        if (this.authenticatorManager instanceof AutoCloseable) {
            ((AutoCloseable) this.authenticatorManager).close();
        }
    }

}
