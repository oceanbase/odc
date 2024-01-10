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

import java.util.Collections;
import java.util.HashSet;

import javax.security.auth.Subject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.RoleType;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.service.iam.model.User;

public abstract class AuthorityTestEnv extends ServiceTestEnv {
    protected final long ADMIN_USER_ID = 1L;
    protected final long ADMIN_ROLE_ID = 1L;
    protected final long ORGANIZATION_ID = 1L;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected UserRoleRepository userRoleRepository;

    @Autowired
    protected PermissionRepository permissionRepository;

    @Autowired
    protected RolePermissionRepository rolePermissionRepository;

    @Autowired
    protected UserPermissionRepository userPermissionRepository;

    protected UserEntity grantAllPermissions(ResourceType... resourceTypes) {
        deleteAll();
        UserEntity userEntity = createUser("currentUser", "currentUser");
        RoleEntity roleEntity = createRole();
        bindUserRole(userEntity.getId(), roleEntity.getId());
        User user = new User(userEntity);
        Subject subject = new Subject(true, new HashSet<>(Collections.singletonList(user)),
                Collections.emptySet(), Collections.emptySet());
        DefaultLoginSecurityManager.setContext(subject);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, ""));
        for (ResourceType resourceType : resourceTypes) {
            PermissionEntity permissionEntity = createPermissionByResourceType(resourceType);
            RolePermissionEntity rolePermissionEntity =
                    bindRolePermission(roleEntity.getId(), permissionEntity.getId());
        }
        return userEntity;
    }

    protected void deleteAll() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        userRoleRepository.deleteAll();
        permissionRepository.deleteAll();
        userPermissionRepository.deleteAll();
        rolePermissionRepository.deleteAll();
    }

    protected UserEntity createUser(String username, String accountName) {
        UserEntity entity = new UserEntity();
        entity.setName(username);
        entity.setAccountName(accountName);
        entity.setType(UserType.USER);
        entity.setPassword("123456");
        entity.setCipher(Cipher.BCRYPT);
        entity.setCreatorId(ADMIN_USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setBuiltIn(false);
        entity.setActive(true);
        entity.setEnabled(true);
        entity.setDescription("internal for unit test");
        return userRepository.saveAndFlush(entity);
    }

    protected RoleEntity createRole() {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName("currentRole");
        roleEntity.setEnabled(true);
        roleEntity.setBuiltIn(false);
        roleEntity.setType(RoleType.CUSTOM);
        roleEntity.setCreatorId(ADMIN_USER_ID);
        roleEntity.setOrganizationId(ORGANIZATION_ID);
        roleEntity.setDescription("internal for unit test");
        return roleRepository.saveAndFlush(roleEntity);
    }

    protected UserRoleEntity bindUserRole(Long userId, Long roleId) {
        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setUserId(userId);
        userRoleEntity.setRoleId(roleId);
        userRoleEntity.setCreatorId(ADMIN_USER_ID);
        userRoleEntity.setOrganizationId(ORGANIZATION_ID);
        return userRoleRepository.saveAndFlush(userRoleEntity);
    }

    protected PermissionEntity createPermissionByResourceType(ResourceType resourceType) {
        PermissionEntity entity = new PermissionEntity();
        entity.setResourceIdentifier(resourceType.name() + ":*");
        entity.setAction("*");
        entity.setType(PermissionType.PUBLIC_RESOURCE);
        entity.setCreatorId(ADMIN_USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setBuiltIn(false);
        entity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
        return permissionRepository.saveAndFlush(entity);
    }

    protected RolePermissionEntity bindRolePermission(Long roleId, Long permissionId) {
        RolePermissionEntity entity = new RolePermissionEntity();
        entity.setRoleId(roleId);
        entity.setPermissionId(permissionId);
        entity.setCreatorId(ADMIN_USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        return rolePermissionRepository.saveAndFlush(entity);
    }

}
