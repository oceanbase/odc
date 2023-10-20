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
package com.oceanbase.odc.migrate.jdbc.common;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.RoleType;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionRepository;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * {@link V41018PermissionMigrateTest}
 *
 * @author yh263208
 * @date 2022-12-28 11:33
 * @since ODC_release_4.1.0
 */
public class V41018PermissionMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Before
    public void setUp() {
        this.userPermissionRepository.deleteAll();
        this.userRoleRepository.deleteAll();
        this.roleRepository.deleteAll();
        this.rolePermissionRepository.deleteAll();
    }

    @Test
    public void migrate_internalRoleExists_migrateSucceed() {
        RoleEntity roleEntity = this.roleRepository.save(getInternalRole());
        UserRoleEntity userRoleEntity = this.userRoleRepository.save(getUserRole(roleEntity));
        RolePermissionEntity rolePermissionEntity = this.rolePermissionRepository.save(getRolePermission(roleEntity));

        V41018PermissionMigrate migrate = new V41018PermissionMigrate();
        migrate.migrate(dataSource);
        List<UserPermissionEntity> actual =
                this.userPermissionRepository.findByPermissionId(rolePermissionEntity.getPermissionId())
                        .stream().peek(e -> {
                            e.setId(null);
                            e.setCreateTime(null);
                            e.setUpdateTime(null);
                        }).collect(Collectors.toList());
        UserPermissionEntity userPermissionEntity = new UserPermissionEntity();
        userPermissionEntity.setUserId(userRoleEntity.getUserId());
        userPermissionEntity.setPermissionId(rolePermissionEntity.getPermissionId());
        userPermissionEntity.setCreatorId(roleEntity.getCreatorId());
        userPermissionEntity.setOrganizationId(roleEntity.getOrganizationId());
        List<UserPermissionEntity> expect = Collections.singletonList(userPermissionEntity);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void migrate_noRelatedUserExists_migrateSucceed() {
        RoleEntity roleEntity = this.roleRepository.save(getInternalRole());
        RolePermissionEntity rolePermissionEntity = this.rolePermissionRepository.save(getRolePermission(roleEntity));

        V41018PermissionMigrate migrate = new V41018PermissionMigrate();
        migrate.migrate(dataSource);
        List<UserPermissionEntity> actual =
                this.userPermissionRepository.findByPermissionId(rolePermissionEntity.getPermissionId());
        Assert.assertTrue(actual.isEmpty());
    }

    private RoleEntity getInternalRole() {
        RoleEntity roleEntity = TestRandom.nextObject(RoleEntity.class);
        roleEntity.setType(RoleType.INTERNAL);
        roleEntity.setId(null);
        roleEntity.setEnabled(true);
        return roleEntity;
    }

    private UserRoleEntity getUserRole(RoleEntity roleEntity) {
        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setCreatorId(roleEntity.getCreatorId());
        userRoleEntity.setOrganizationId(roleEntity.getOrganizationId());
        userRoleEntity.setRoleId(roleEntity.getId());
        userRoleEntity.setUserId(new Random().nextLong());
        return userRoleEntity;
    }

    private RolePermissionEntity getRolePermission(RoleEntity roleEntity) {
        RolePermissionEntity rolePermissionEntity = new RolePermissionEntity();
        rolePermissionEntity.setCreatorId(roleEntity.getCreatorId());
        rolePermissionEntity.setOrganizationId(roleEntity.getOrganizationId());
        rolePermissionEntity.setRoleId(roleEntity.getId());
        rolePermissionEntity.setPermissionId(new Random().nextLong());
        return rolePermissionEntity;
    }

}
