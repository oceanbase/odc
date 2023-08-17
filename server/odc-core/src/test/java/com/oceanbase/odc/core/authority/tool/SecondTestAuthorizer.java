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
import java.util.Collection;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.authority.auth.Authorizer;
import com.oceanbase.odc.core.authority.auth.SecurityContext;
import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.authority.permission.Permission;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecondTestAuthorizer implements Authorizer {
    private final boolean finalResult;
    private final boolean terminted;
    private final boolean privileged;

    public SecondTestAuthorizer(boolean finalResult, boolean terminted, boolean privileged) {
        this.finalResult = finalResult;
        this.terminted = terminted;
        this.privileged = privileged;
    }

    @Override
    public boolean isPermitted(Principal principal, Collection<Permission> permissions, SecurityContext context) {
        log.info("Begin to checkPermission for DefaultAuthorizerTwo, principal={}, permissions={}, context={}",
                principal.getName(), permissions.stream().map(Object::toString).collect(Collectors.joining(",")),
                context);
        if (this.terminted) {
            context.terminate();
        }
        if (this.privileged) {
            context.setPrivileged(true);
        }
        return this.finalResult;
    }

    @Override
    public void checkPermission(Principal principal, Collection<Permission> permissions, SecurityContext context)
            throws AccessDeniedException {
        if (!isPermitted(principal, permissions, context)) {
            throw new AccessDeniedException();
        }
    }

    @Override
    public boolean supports(Class<? extends Principal> principal) {
        return true;
    }

}
