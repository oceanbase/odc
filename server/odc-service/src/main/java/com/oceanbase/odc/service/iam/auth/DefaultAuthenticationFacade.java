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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.oceanbase.odc.core.authority.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.service.iam.model.Organization;
import com.oceanbase.odc.service.iam.model.User;

public class DefaultAuthenticationFacade implements AuthenticationFacade {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Override
    public User currentUser() throws AccessDeniedException {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException();
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new AccessDeniedException();
        }
        Verify.verify(principal instanceof User, "principal not an User");
        return (User) principal;
    }

    @Override
    public long currentUserId() throws AccessDeniedException {
        Long id = currentUser().getId();
        Verify.notNull(id, "currentUser.id");
        return id;
    }

    @Override
    public String currentUserIdStr() throws AccessDeniedException {
        return "" + currentUserId();
    }

    @Override
    public String currentUserAccountName() throws AccessDeniedException {
        String accountName = currentUser().getAccountName();
        Verify.notNull(accountName, "currentUser.accountName");
        return accountName;
    }

    @Override
    public String currentUsername() throws AccessDeniedException {
        String name = currentUser().getName();
        Verify.notNull(name, "currentUser.name");
        return name;
    }

    @Override
    public long currentOrganizationId() throws AccessDeniedException {
        Long organizationId = currentUser().getOrganizationId();
        Verify.notNull(organizationId, "currentUser.organizationId");
        return organizationId;
    }

    @Override
    public String currentOrganizationIdStr() throws AccessDeniedException {
        return "" + currentOrganizationId();
    }

    @Override
    public Organization currentOrganization() throws AccessDeniedException {
        long organizationId = currentOrganizationId();
        return organizationRepository.findById(organizationId).map(Organization::ofEntity).orElseThrow(
                () -> new NotFoundException(ResourceType.ODC_ORGANIZATION, "organizationId", organizationId));
    }
}
