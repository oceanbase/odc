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

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.permission.Permission;

/**
 * Authorizer for security module, used to answer the check the permission for an operation of a
 * resource
 *
 * @author yh263208
 * @date 2021-07-08 15:16
 * @since ODC_release_3.2.0
 */
public interface Authorizer {
    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param principal Operating subject
     * @param permissions permission collection
     * @param context context for Authentication
     * @return result
     */
    boolean isPermitted(Principal principal, Collection<Permission> permissions, SecurityContext context);

    /**
     * Permission verification interface, used to verify whether the user has the right to operate on a
     * certain resource
     *
     * @param principal Operating subject
     * @param permissions permission collection
     * @param context context for Authentication
     * @exception AccessDeniedException exception will be thrown when authentication failed
     */
    void checkPermission(Principal principal, Collection<Permission> permissions, SecurityContext context)
            throws AccessDeniedException;

    /**
     * Each {@link Authorizer} can only authenticate one {@link Principal}. This method is to indicate
     * whether the {@link Authorizer} can support a certain {@link Principal}
     *
     * @param principal {@link Principal} that needed to be authenticated
     * @return Flag to indicate whether the {@link Authorizer} support the {@link Principal}
     */
    boolean supports(Class<? extends Principal> principal);

}
