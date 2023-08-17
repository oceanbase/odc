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
import java.util.Collections;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.auth.AuthenticatorManager;
import com.oceanbase.odc.core.authority.auth.AuthorizerManager;
import com.oceanbase.odc.core.authority.auth.ReturnValueProvider;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.exception.AuthenticationException;
import com.oceanbase.odc.core.authority.model.BaseAuthenticationToken;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.PermissionProvider;
import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.session.SecuritySessionManager;

import lombok.NonNull;

/**
 * Core manager for security module, all operation have to be done via this object
 *
 * @author yh263208
 * @date 2021-07-08 15:12
 * @since ODC_release_3.2.0
 */
public interface SecurityManager
        extends AuthenticatorManager, AuthorizerManager, SecuritySessionManager, ReturnValueProvider,
        PermissionProvider, AutoCloseable {
    /**
     * The proxy object of the session manager, used to proxy the work of the
     * <code>SessionManager</code>
     *
     * @author yh263208
     * @date 2021-07-21 19:13
     * @since ODC_release_3.2.0
     */
    interface DelegateSessionManager {
        /**
         * Start a {@link SecuritySession}
         *
         * @return {@link SecuritySession}
         */
        SecuritySession startSession();

        /**
         * Get a {@link SecuritySession}
         *
         * @return {@link SecuritySession}
         */
        SecuritySession getSession();

    }

    /**
     * The login method, the user performs the login operation by calling this method. After logging in,
     * you need to save the user's identity information in the {@link SecuritySession}, and save it in
     * the {@link SecuritySession} to make it easier to use in subsequent calls.
     *
     * @param tokens collection of {@link BaseAuthenticationToken}
     * @param delegate {@link DelegateSessionManager} to provide a delegate of
     *        {@link SecuritySessionManager}
     * @return {@link Subject}
     * @exception AuthenticationException exception will be thrown when login failed
     */
    Subject login(Collection<BaseAuthenticationToken<? extends Principal, ?>> tokens, DelegateSessionManager delegate)
            throws AuthenticationException;

    /**
     * The caller of the logout method name logs out the user from the registered state through this
     * method, and at the same time expires the user's {@link SecuritySession}
     *
     * @param session {@link SecuritySession} that will be expired
     */
    default void logout(@NonNull SecuritySession session) {
        session.expire();
    }

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param permissions permission collection
     * @return result
     */
    boolean isPermitted(Collection<Permission> permissions);

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param permissions permission collection
     * @exception AccessDeniedException exception will be thrown when authentication failed
     */
    void checkPermission(Collection<Permission> permissions) throws AccessDeniedException;

    default boolean isPermitted(Permission permission) {
        return isPermitted(Collections.singletonList(permission));
    }

    default void checkPermission(Permission permission) throws AccessDeniedException {
        checkPermission(Collections.singletonList(permission));
    }

    Object decide(Object returnValue) throws AccessDeniedException;

}
