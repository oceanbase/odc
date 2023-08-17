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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.AuthenticationInfo;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link DefaultAuthenticatorManager}
 *
 * @author yh263208
 * @date 2021-07-20 14:47
 * @see AuthenticatorManager
 * @since ODC_release_3.2.0
 */
@Slf4j
public class DefaultAuthenticatorManager implements AuthenticatorManager {

    private final List<Authenticator> authenticators = new LinkedList<>();

    public DefaultAuthenticatorManager(@NonNull Collection<Authenticator> collections) {
        authenticators.addAll(collections);
    }

    public void addAuthenticator(@NonNull Authenticator authenticator) {
        this.authenticators.add(authenticator);
    }

    @Override
    public Subject authenticate(@NonNull Collection<BaseAuthenticationToken<? extends Principal, ?>> tokens)
            throws AuthenticationException {
        Set<Principal> principalSet = new HashSet<>();
        Set<Object> credentialSet = new HashSet<>();
        for (BaseAuthenticationToken<? extends Principal, ?> token : tokens) {
            for (Authenticator authenticator : this.authenticators) {
                if (!authenticator.supports(token.getPrincipal().getClass())
                        || principalSet.contains(token.getPrincipal())) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Begin to authenticate the principal, Principal={}", token.getPrincipal().getName());
                }
                try {
                    AuthenticationInfo<? extends Principal, ?> info = authenticator.authenticate(token);
                    if (info == null || info.getPrincipal() == null) {
                        continue;
                    }
                    principalSet.add(info.getPrincipal());
                    if (info.getCredential() != null) {
                        credentialSet.add(info.getCredential());
                    }
                } catch (AuthenticationException e) {
                    log.warn("Failed to authenticate the principal, Principal={}", token.getPrincipal().getName(), e);
                }
            }
        }
        if (principalSet.size() == 0) {
            throw new AuthenticationException("No principal is authenticated");
        }
        return new Subject(true, principalSet, credentialSet, Collections.emptySet());
    }

}
