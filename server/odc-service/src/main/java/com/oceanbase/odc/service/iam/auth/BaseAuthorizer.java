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
package com.oceanbase.odc.service.iam.auth;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.oceanbase.odc.core.authority.auth.Authorizer;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.service.iam.model.User;

/**
 * @Author: Lebie
 * @Date: 2023/4/26 16:28
 * @Description: []
 */
public abstract class BaseAuthorizer implements Authorizer {
    @Override
    public void checkPermission(Principal principal, Collection<Permission> permissions, SecurityContext context)
            throws AccessDeniedException {
        User odcUser = (User) principal;
        if (Objects.isNull(odcUser.getId())) {
            throw new AccessDeniedException(new NullPointerException("User Id is null"));
        }
        if (!isPermitted(principal, permissions, context)) {
            throw new AccessDeniedException(principal, permissions);
        }
    }

    @Override
    public boolean isPermitted(Principal principal, Collection<Permission> permissions, SecurityContext context) {
        User odcUser = (User) principal;
        if (Objects.isNull(odcUser.getId())) {
            return false;
        }
        List<Permission> permittedPermissions = listPermittedPermissions(principal);
        for (Permission permission : permissions) {
            boolean accessDenied = true;
            for (Permission resourcePermission : permittedPermissions) {
                if (resourcePermission.implies(permission)) {
                    accessDenied = false;
                    break;
                }
            }
            if (accessDenied) {
                return false;
            }
        }
        return true;
    }

    /**
     * An <code>Authorizer</code> may not work for all <code>Principal</code> of the
     * <code>Subject</code>, so a method is needed to indicate whether a certain <code>Authorizer</code>
     * can work for a certain <code>Principal</code>
     *
     * @param principal <code>Class</code> type for a <code>Principal</code>
     * @return Flag to indicate whether the <code>Authorizer</code> supports the <code>Principal</code>
     */
    @Override
    public boolean supports(Class<? extends Principal> principal) {
        return User.class.isAssignableFrom(principal);
    }

    protected abstract List<Permission> listPermittedPermissions(Principal principal);
}
