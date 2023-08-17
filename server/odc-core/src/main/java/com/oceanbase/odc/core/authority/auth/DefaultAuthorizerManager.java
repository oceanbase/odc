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
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.auth.SecurityContext.AuthenticationDomain;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.permission.Permission;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Default authentication manager
 *
 * @author yh263208
 * @date 2021-07-20 16:46
 * @since ODC_release_3.2.0
 * @see AuthorizerManager
 */
@Slf4j
public class DefaultAuthorizerManager implements AuthorizerManager {

    private final List<Authorizer> authorizers = new LinkedList<>();

    public DefaultAuthorizerManager(@NonNull Collection<Authorizer> authorizers) {
        this.authorizers.addAll(authorizers);
    }

    public void addAuthorizer(@NonNull Authorizer authorizer) {
        this.authorizers.add(authorizer);
    }

    @Override
    public SecurityContext permit(Subject subject, @NonNull Collection<Permission> permissions) {
        if (subject == null) {
            log.warn("Failed to get the context, cause the subject is null");
            throw new AccessDeniedException(new AuthenticationException("Subject is null, not login"));
        }
        SecurityContext context;
        try {
            context = new SecurityContext(subject);
        } catch (AuthenticationException e) {
            return null;
        }
        for (Authorizer authorizer : this.authorizers) {
            if (context.isTerminated() || context.isPrivileged()) {
                return context;
            }
            try (AuthenticationDomain domain = context.createDomain(authorizer, permissions)) {
                for (Principal principal : subject.getPrincipals()) {
                    if (!authorizer.supports(principal.getClass())) {
                        continue;
                    }
                    domain.record(principal);
                }
            }
        }
        return context;
    }

    @Override
    public SecurityContext checkPermission(Subject subject, Collection<Permission> permissions,
            PermissionStrategy strategy) throws AccessDeniedException {
        SecurityContext context = permit(subject, permissions);
        if (strategy.decide(context)) {
            return context;
        }
        throw new AccessDeniedException(permissions);
    }

}
