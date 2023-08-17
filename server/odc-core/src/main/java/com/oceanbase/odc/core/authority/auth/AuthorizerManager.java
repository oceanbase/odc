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

import java.util.Collection;

import javax.security.auth.Subject;

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.permission.Permission;

/**
 * The {@link AuthorizerManager} is mainly used to manage multiple {@link Authorizer}. When there
 * are multiple {@link Authorizer}, it is used to manage the relationship between the
 * {@link Authorizer} and transfer {@link SecurityContext}
 */
public interface AuthorizerManager {
    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param subject Operating subject
     * @param permissions permission collection
     * @return {@link SecurityContext}
     */
    SecurityContext permit(Subject subject, Collection<Permission> permissions);

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param subject Operating subject
     * @param permissions permission collection
     * @param strategy {@link PermissionStrategy}
     * @return {@link SecurityContext}
     * @exception AccessDeniedException exception will be thrown when authentication failed
     */
    SecurityContext checkPermission(Subject subject, Collection<Permission> permissions, PermissionStrategy strategy)
            throws AccessDeniedException;

}
