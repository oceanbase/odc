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

import java.util.Collection;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.auth.AuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.AuthorizerManager;
import com.oceanbase.odc.core.authority.auth.PermissionStrategy;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.permission.Permission;

import lombok.Getter;
import lombok.NonNull;

/**
 * Abstract authenticator security manager {@link BaseAuthorizerSecurityManager}, used for all work
 * related to proxy and authentication
 *
 * @author yh263208
 * @date 2021-07-21 16:42
 * @since ODC_release_3.2.0
 * @see BaseAuthenticatorSecurityManager
 */
abstract class BaseAuthorizerSecurityManager extends BaseAuthenticatorSecurityManager {

    private static final ThreadLocal<SecurityContext> SECURITY_CONTEXT_HOLDER = new ThreadLocal<>();
    @Getter
    private final AuthorizerManager authorizerManager;
    private final PermissionStrategy permissionStrategy;

    public BaseAuthorizerSecurityManager(AuthenticatorManager authenticatorManager,
            @NonNull AuthorizerManager authorizerManager, @NonNull PermissionStrategy permissionStrategy) {
        super(authenticatorManager);
        this.authorizerManager = authorizerManager;
        this.permissionStrategy = permissionStrategy;
    }

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param subject Operating subject
     * @param permissions permission collection
     * @return result
     */
    @Override
    public SecurityContext permit(Subject subject, Collection<Permission> permissions) {
        SecurityContext context = this.authorizerManager.permit(subject, permissions);
        SECURITY_CONTEXT_HOLDER.remove();
        if (context != null) {
            setSecurityContext(context);
        }
        return context;
    }

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param subject Operating subject
     * @param permissions permission collection
     * @param strategy {@link PermissionStrategy}
     * @exception AccessDeniedException exception will be thrown when authentication failed
     */
    @Override
    public SecurityContext checkPermission(Subject subject,
            Collection<Permission> permissions, PermissionStrategy strategy) throws AccessDeniedException {
        SecurityContext context = this.authorizerManager.checkPermission(subject, permissions, strategy);
        SECURITY_CONTEXT_HOLDER.remove();
        if (context != null) {
            setSecurityContext(context);
        }
        return context;
    }

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param permissions permission collection
     * @return result
     */
    public boolean isPermitted(Subject subject, Collection<Permission> permissions) {
        return this.permissionStrategy.decide(permit(subject, permissions));
    }

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param permissions permission collection
     * @exception AccessDeniedException exception will be thrown when authentication failed
     */
    public void checkPermission(Subject subject, Collection<Permission> permissions) throws AccessDeniedException {
        checkPermission(subject, permissions, this.permissionStrategy);
    }

    public static SecurityContext setSecurityContext(@NonNull SecurityContext context) {
        SECURITY_CONTEXT_HOLDER.remove();
        SECURITY_CONTEXT_HOLDER.set(context);
        return context;
    }

    public static SecurityContext getSecurityContext() {
        return SECURITY_CONTEXT_HOLDER.get();
    }

    public static void removeSecurityContext() {
        SECURITY_CONTEXT_HOLDER.remove();
    }

    @Override
    public void close() throws Exception {
        Exception thrown = null;
        try {
            super.close();
        } catch (Exception e) {
            thrown = e;
        }
        if (this.authorizerManager instanceof AutoCloseable) {
            ((AutoCloseable) this.authorizerManager).close();
        }
        if (thrown != null) {
            throw thrown;
        }
    }

}
