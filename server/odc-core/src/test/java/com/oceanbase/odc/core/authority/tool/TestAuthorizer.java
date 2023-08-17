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
package com.oceanbase.odc.core.authority.tool;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;

import com.oceanbase.odc.core.authority.auth.Authorizer;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;

public class TestAuthorizer implements Authorizer {

    @Override
    public boolean isPermitted(Principal principal, Collection<Permission> permissions, SecurityContext context) {
        Collection<Permission> grantPermissions =
                Arrays.asList(new ResourcePermission(TestResourceFactory.getResource_1(), ResourcePermission.READ),
                        new ResourcePermission(TestResourceFactory.getNestResource_4(), ResourcePermission.READ));
        for (Permission permission : permissions) {
            boolean accessDenied = true;
            for (Permission resourcePermission : grantPermissions) {
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

    @Override
    public void checkPermission(Principal principal, Collection<Permission> permissions, SecurityContext context)
            throws AccessDeniedException {
        if (!isPermitted(principal, permissions, context)) {
            throw new AccessDeniedException(principal, permissions);
        }
    }

    @Override
    public boolean supports(Class<? extends Principal> principal) {
        return true;
    }

}

