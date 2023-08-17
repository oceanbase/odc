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
package com.oceanbase.odc.metadb.iam;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.RoleType;

/**
 * @author gaoda.xy
 * @date 2022/12/7 20:51
 */
public class RoleRepositoryTest extends ServiceTestEnv {

    private static final Long USER_ID = 1L;
    private static final Long ORGANIZATION_ID = 1L;

    @Autowired
    private RoleRepository roleRepository;

    @Before
    public void setUp() throws Exception {
        roleRepository.deleteAll();
    }

    @Test
    public void testFindByName() {
        RoleEntity entity = createRoleEntity("testFindByName");
        Assert.assertEquals(1, roleRepository.findByName(entity.getName()).size());
        Assert.assertEquals(roleRepository.findByName(entity.getName()).get(0).getName(), entity.getName());
    }

    @Test
    public void testFindByOrganizationId() {
        RoleEntity roleEntity1 = createRoleEntity("role1");
        RoleEntity roleEntity2 = createRoleEntity("role2");
        RoleEntity roleEntity3 = createRoleEntity("role3");
        Page<RoleEntity> roleEntityPage =
                roleRepository.findByOrganizationId(ORGANIZATION_ID, PageRequest.of(1, 2, Direction.DESC, "id"));
        Assert.assertEquals(2, roleEntityPage.getTotalPages());
        Assert.assertEquals(3, roleEntityPage.getTotalElements());
        Assert.assertEquals(roleEntity1.getId(), roleEntityPage.getContent().get(0).getId());
    }

    @Test
    public void testFindTopByNameAndOrganizationId() {
        RoleEntity entity1 = createRoleEntity("entity1");
        RoleEntity entity2 = createRoleEntity("entity2");
        Assert.assertFalse(roleRepository.findByNameAndOrganizationId(null, 1L).isPresent());
        Assert.assertFalse(roleRepository.findByNameAndOrganizationId("no_exists", 1L).isPresent());
        Assert.assertEquals(roleRepository.findByNameAndOrganizationId("entity1", 1L).get().getId(), entity1.getId());
    }

    @Test
    public void testFindByOrganizationIdAndRoleIds() {
        RoleEntity roleEntity1 = createRoleEntity("role1");
        RoleEntity roleEntity2 = createRoleEntity("role2");
        RoleEntity roleEntity3 = createRoleEntity("role3");
        Page<RoleEntity> roleEntityPage = roleRepository.findByOrganizationIdAndRoleIds(ORGANIZATION_ID,
                Arrays.asList(roleEntity1.getId(), roleEntity3.getId()), PageRequest.of(1, 1, Direction.DESC, "id"));
        Assert.assertEquals(2, roleEntityPage.getTotalPages());
        Assert.assertEquals(2, roleEntityPage.getTotalElements());
        Assert.assertEquals(roleEntity1.getId(), roleEntityPage.getContent().get(0).getId());
    }

    private RoleEntity createRoleEntity(String name) {
        RoleEntity entity = new RoleEntity();
        entity.setName(name);
        entity.setEnabled(true);
        entity.setType(RoleType.CUSTOM);
        entity.setOrganizationId(1L);
        entity.setCreatorId(1L);
        entity.setBuiltIn(false);
        return roleRepository.saveAndFlush(entity);
    }

}
