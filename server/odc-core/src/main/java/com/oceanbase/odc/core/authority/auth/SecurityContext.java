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

import java.io.Closeable;
import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.permission.Permission;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SecurityContext} is used to encapsulate the context information in a certain
 * authentication process. An authentication process may go through multiple
 * {@link Authorizer#isPermitted(Principal, Collection, SecurityContext)}, and there may be a need
 * to dynamically adjust parameters during the authentication process.
 *
 * On the one hand, the {@link SecurityContext} helps the caller to connect the entire
 * authentication process in series, and at the same time, it also acts as a control handle to
 * control the entire authentication process.
 *
 * @author yh263208
 * @date 2021-07-12 17:21
 * @since ODC_release_3.2.0
 */
public class SecurityContext implements Serializable {

    private static final long serialVersionUID = -4184433309994577411L;
    @Getter
    private final Subject subject;
    private final List<AuthenticationDomain> domains = new LinkedList<>();
    @Getter
    @Setter
    private boolean privileged = false;
    @Getter
    private boolean allDenied = true;
    @Getter
    private boolean allPermitted = true;
    /**
     * If is true, the entire authentication process ends
     */
    @Getter
    private volatile boolean terminated = false;

    public SecurityContext(@NonNull Subject subject) throws AuthenticationException {
        if (subject.getPrincipals() == null || subject.getPrincipals().isEmpty()) {
            throw new AuthenticationException("No principal exists");
        }
        this.subject = subject;
    }

    /**
     * End the entire authentication process
     */
    public void terminate() {
        this.terminated = true;
    }

    public boolean permit(@NonNull Collection<Permission> permissions) {
        Set<Principal> principalSet = this.subject.getPrincipals();
        for (Principal principal : principalSet) {
            for (AuthenticationDomain domain : this.domains) {
                if (domain.permit(principal, permissions)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean permit(@NonNull Permission permission) {
        return permit(Collections.singleton(permission));
    }

    public AuthenticationDomain createDomain(@NonNull Authorizer authorizer,
            @NonNull Collection<Permission> permissions) {
        validTerminated();
        return new AuthenticationDomain(authorizer, permissions, this);
    }

    public void forEach(@NonNull Consumer<AuthenticationDomain> consumer) {
        this.domains.forEach(consumer);
    }

    @Override
    public String toString() {
        String principals = "Principal:["
                + this.getSubject().getPrincipals().stream().map(Principal::getName).collect(Collectors.joining(", "))
                + "]";
        StringBuilder buffer = new StringBuilder();
        for (AuthenticationDomain domain : this.domains) {
            Map<Principal, Boolean> checkResult = domain.principal2AuthResult;
            buffer.append(",").append(domain.getName()).append(":[").append(checkResult.entrySet().stream()
                    .map(entry -> entry.getKey().getName() + ":" + entry.getValue()).collect(Collectors.joining(",")))
                    .append("]");
        }
        return principals + buffer.toString();
    }

    private void validTerminated() {
        if (!terminated) {
            return;
        }
        throw new IllegalStateException("Context is terminated");
    }

    private void endDomain(@NonNull AuthenticationDomain target) {
        for (Boolean permitted : target.principal2AuthResult.values()) {
            if (permitted == null) {
                throw new IllegalStateException("Permitted flag can not be null");
            }
            this.allPermitted &= permitted;
            this.allDenied &= !permitted;
        }
        this.domains.add(target);
    }

    /**
     * An authentication process may contain multiple authentication operations. Each authentication
     * operation can be abstracted into an {@link AuthenticationDomain}.
     *
     * {@link AuthenticationDomain} records the complete information of an authentication operation,
     * including the subjects involved.
     *
     * @author yh263208
     * @date 2021-07-12 21:13
     * @since ODC_release_3.2.0
     */
    @Slf4j
    public static class AuthenticationDomain implements Closeable {
        @Getter
        private final Authorizer authorizer;
        /**
         * An authentication operation may involve multiple subjects of a user, and each subject corresponds
         * to an authentication result
         */
        private final Map<Principal, Boolean> principal2AuthResult;
        @Getter
        private final String name;
        @Getter
        private final Collection<Permission> permissions;
        private final SecurityContext context;

        private AuthenticationDomain(Authorizer authorizer,
                Collection<Permission> permissions, SecurityContext context) {
            this.authorizer = authorizer;
            this.permissions = permissions;
            this.principal2AuthResult = new HashMap<>();
            this.name = authorizer.getClass().getSimpleName();
            this.context = context;
        }

        public boolean record(@NonNull Principal principal) {
            return internalRecord(principal, authorizer.isPermitted(principal, permissions, context));
        }

        public boolean record(@NonNull Principal principal, boolean result) {
            return internalRecord(principal, result);
        }

        /**
         * whether a group of resources under a subject is implied by this {@link AuthenticationDomain}
         *
         * @param principal authentication principal
         * @param permissions collection of permission
         * @return imply result
         */
        public boolean permit(@NonNull Principal principal, @NonNull Collection<Permission> permissions) {
            if (!authorizer.supports(principal.getClass())) {
                return false;
            }
            for (Permission permission : permissions) {
                boolean subImpliesFlag = false;
                for (Permission subPermission : this.permissions) {
                    if (subPermission.implies(permission)) {
                        subImpliesFlag = true;
                        break;
                    }
                }
                if (!subImpliesFlag) {
                    return false;
                }
            }
            return this.principal2AuthResult.getOrDefault(principal, false);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("AuthenticationDomain: ");
            builder.append(this.principal2AuthResult.entrySet().stream()
                    .map(entry -> "[" + entry.getKey().getName() + ": " + entry.getValue() + "]")
                    .collect(Collectors.joining(","))).append("\n");
            for (Permission permission : this.permissions) {
                builder.append("  ").append(permission.toString()).append("\n");
            }
            return builder.substring(0, builder.length() - 1);
        }

        @Override
        public void close() {
            context.endDomain(this);
        }

        private boolean internalRecord(@NonNull Principal principal, boolean result) {
            if (!authorizer.supports(principal.getClass())) {
                throw new IllegalStateException("Authorizer " + name + " does not support " + principal.getClass());
            }
            Boolean preResult = this.principal2AuthResult.get(principal);
            if (preResult != null) {
                if (result != preResult) {
                    log.warn("The record in the authentication domain already exists, principal={}", principal);
                }
                return preResult;
            }
            this.principal2AuthResult.putIfAbsent(principal, result);
            return result;
        }
    }

}
