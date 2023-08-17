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
package com.oceanbase.odc;

import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.Subject;

import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.service.iam.model.User;

public abstract class MockedAuthorityTestEnv extends ServiceTestEnv {
    protected final long ADMIN_USER_ID = 1L;
    protected final long ADMIN_ROLE_ID = 1L;
    protected final long ORGANIZATION_ID = 1L;

    @MockBean
    protected PermissionRepository repository;

    protected void grantAllPermissions(ResourceType... types) {
        User user = new User();
        user.setId(ADMIN_USER_ID);
        user.setOrganizationId(ORGANIZATION_ID);

        Subject subject = new Subject(true, new HashSet<>(Collections.singletonList(user)),
                Collections.emptySet(), Collections.emptySet());
        DefaultLoginSecurityManager.setContext(subject);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, ""));
        List<PermissionEntity> returnVal = new LinkedList<>();
        for (ResourceType item : types) {
            PermissionEntity entity = new PermissionEntity();
            entity.setResourceIdentifier(item.name() + ":*");
            entity.setAction("*");
            entity.setOrganizationId(ORGANIZATION_ID);
            returnVal.add(entity);
        }
        Mockito.when(
                repository.findByUserIdAndRoleStatusAndOrganizationId(eq(ADMIN_USER_ID), eq(true), eq(ORGANIZATION_ID)))
                .thenReturn(returnVal);
        Mockito.when(repository.findByUserIdAndUserStatusAndRoleStatusAndOrganizationId(eq(ADMIN_USER_ID), eq(true),
                eq(true), eq(ORGANIZATION_ID))).thenReturn(returnVal);
    }

}
