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
package com.oceanbase.odc.metadb.connection;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.common.util.EmptyValues;
import com.oceanbase.odc.test.tool.TestRandom;

public class ConnectionConfigRepositoryTest extends ServiceTestEnv {

    private static final Long CREATOR_ID = 101L;
    private static final Long ORGANIZATION_ID = 102L;
    private static final String NAME = "TEST_C1";
    private static final String CLUSTER_NAME = "C1";
    private static final String TENANT_NAME = "T1";

    @Autowired
    private ConnectionConfigRepository repository;

    @Before
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void test_findByUpdateTimeBefore_return1() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        repository.save(connection);
        List<ConnectionEntity> entities = repository.findByUpdateTimeBefore(new Date(new Date().getTime() + 5 * 1000L));
        Assert.assertEquals(1, entities.size());
    }

    @Test
    public void test_findByUpdateTimeBefore_return0() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        repository.save(connection);
        List<ConnectionEntity> entities = repository.findByUpdateTimeBefore(new Date(new Date().getTime() - 5 * 1000L));
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void test_findByUpdateTimeBeforeAndTemp_return1() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        connection.setTemp(true);
        repository.save(connection);
        List<ConnectionEntity> entities =
                repository.findByUpdateTimeBeforeAndTemp(new Date(new Date().getTime() + 5 * 1000L), true);
        Assert.assertEquals(1, entities.size());
    }

    @Test
    public void test_findByUpdateTimeBeforeAndTemp_return0() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        connection.setTemp(true);
        repository.save(connection);
        List<ConnectionEntity> entities =
                repository.findByUpdateTimeBeforeAndTemp(new Date(new Date().getTime() + 5 * 1000L), false);
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void findById_NotExists_IsPresentFalse() {
        Optional<ConnectionEntity> find = repository.findById(-1L);

        Assert.assertFalse(find.isPresent());
    }

    @Test
    public void save_Create_CreateTimeNotNull() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);

        ConnectionEntity save = repository.save(connection);

        Assert.assertNotNull(save.getCreateTime());
    }

    @Test
    public void save_Update_CreateTimeNotNull() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);

        ConnectionEntity save = repository.save(connection);

        save.setClusterName("C1-2");

        ConnectionEntity save2 = repository.save(connection);

        Assert.assertNotNull(save2.getCreateTime());
    }

    @Test
    public void findById_Exists_IsPresentTrue() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        ConnectionEntity save = repository.save(connection);
        Long id = save.getId();

        Optional<ConnectionEntity> find = repository.findById(id);

        Assert.assertTrue(find.isPresent());
    }

    @Test
    public void findAll_BySpecsMatch_Found() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        ConnectionEntity saved = repository.save(connection);

        Specification<ConnectionEntity> spec = Specification
                .where(ConnectionSpecs.visibleScopeEqual(ConnectionVisibleScope.PRIVATE))
                .and(ConnectionSpecs.typeIn(Collections.singletonList(ConnectType.OB_MYSQL)))
                .and(ConnectionSpecs.enabledEqual(true))
                .and(ConnectionSpecs.organizationIdEqual(ORGANIZATION_ID))
                .and(ConnectionSpecs.userIdEqual(CREATOR_ID))
                .and(ConnectionSpecs.dialectTypeIn(Collections.singletonList(DialectType.OB_MYSQL)))
                .and(ConnectionSpecs.nameLike("C1"))
                .and(ConnectionSpecs.idIn(Collections.singletonList(saved.getId())))
                .and(ConnectionSpecs.tenantNameIn(Collections.emptyList()))
                .and(ConnectionSpecs.clusterNameIn(Collections.emptyList()))
                .and(ConnectionSpecs.idLike(saved.getId().toString()))
                .and(ConnectionSpecs.hostLike(connection.getHost()))
                .and(ConnectionSpecs.portLike(connection.getPort().toString()))
                .and(ConnectionSpecs.isNotTemp());

        List<ConnectionEntity> all = repository.findAll(spec);

        Assert.assertEquals(1, all.size());
    }

    @Test
    public void findAll_ClusterNameNull_WithEmptyExpression() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        connection.setClusterName(null);
        repository.save(connection);

        Specification<ConnectionEntity> spec = Specification
                .where(ConnectionSpecs.visibleScopeEqual(ConnectionVisibleScope.PRIVATE))
                .and(ConnectionSpecs.clusterNameIn(Arrays.asList(NAME, EmptyValues.EXPRESSION)))
                .and(ConnectionSpecs.isNotTemp());

        List<ConnectionEntity> all = repository.findAll(spec);

        Assert.assertEquals(1, all.size());
    }

    @Test
    public void findAll_TempConnectionIsNotTemp_NotFound() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        connection.setTemp(true);
        repository.save(connection);

        List<ConnectionEntity> all = repository.findAll(ConnectionSpecs.isNotTemp());

        Assert.assertEquals(0, all.size());
    }

    @Test
    public void findAll_TempConnectionAll_Found() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        connection.setTemp(true);
        repository.save(connection);

        List<ConnectionEntity> all = repository.findAll();

        Assert.assertEquals(1, all.size());
    }

    @Test
    public void getByVisibleScopeAndOwnerIdAndName_Exists_MatchId() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        ConnectionEntity savedEntity = repository.saveAndFlush(connection);

        Long id = repository.findByVisibleScopeAndOwnerIdAndName(ConnectionVisibleScope.PRIVATE, CREATOR_ID, NAME)
                .orElseThrow(() -> new RuntimeException("connection not found")).getId();

        Assert.assertEquals(savedEntity.getId(), id);
    }

    @Test
    public void getByVisibleScopeAndOwnerIdAndName_NotExists_isPresentFalse() {
        Optional<ConnectionEntity> optionalEntity =
                repository.findByVisibleScopeAndOwnerIdAndName(ConnectionVisibleScope.PRIVATE, CREATOR_ID, NAME);

        Assert.assertFalse(optionalEntity.isPresent());
    }

    @Test
    public void deleteByVisibleScopeAndOwnerId_Exists_Return1() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.PRIVATE);
        repository.save(connection);

        int rows = repository.deleteByVisibleScopeAndOwnerId(ConnectionVisibleScope.PRIVATE, CREATOR_ID);

        Assert.assertEquals(1, rows);
    }

    @Test
    public void findIdsByVisibleScopeAndOrganizationId() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.ORGANIZATION);
        repository.save(connection);

        Set<Long> ids = repository.findIdsByVisibleScopeAndOrganizationId(
                ConnectionVisibleScope.ORGANIZATION, ORGANIZATION_ID);

        HashSet<Long> expected = new HashSet<>();
        expected.add(connection.getId());
        Assert.assertEquals(expected, ids);
    }

    @Test
    public void findByVisibleScopeAndOrganizationId() {
        ConnectionEntity connection = createEntity(ConnectionVisibleScope.ORGANIZATION);
        ConnectionEntity savedEntity = repository.save(connection);

        List<ConnectionEntity> foundEntities =
                repository.findByVisibleScopeAndOrganizationId(ConnectionVisibleScope.ORGANIZATION, ORGANIZATION_ID);
        List<ConnectionEntity> notFoundEntities =
                repository.findByVisibleScopeAndOrganizationId(ConnectionVisibleScope.ORGANIZATION, 1L);

        Assert.assertEquals(0, notFoundEntities.size());
        Assert.assertEquals(1, foundEntities.size());
        Assert.assertEquals(savedEntity.getId(), foundEntities.get(0).getId());
    }

    @Test
    public void deleteByIds_servalConnsExist_deleteSucceed() {
        ConnectionEntity c1 = createEntity(ConnectionVisibleScope.ORGANIZATION);
        c1.setName("test_01");
        ConnectionEntity c2 = createEntity(ConnectionVisibleScope.PRIVATE);
        c1.setName("test_02");
        ConnectionEntity c3 = createEntity(ConnectionVisibleScope.PRIVATE);
        c3.setName("test_03");
        List<ConnectionEntity> entities = repository.saveAll(Arrays.asList(c1, c2, c3));

        int affectRows =
                repository.deleteByIds(entities.stream().map(ConnectionEntity::getId).collect(Collectors.toSet()));
        Assert.assertEquals(entities.size(), affectRows);
    }

    private ConnectionEntity createEntity(ConnectionVisibleScope visibleScope) {
        ConnectionEntity entity = TestRandom.nextObject(ConnectionEntity.class);
        entity.setId(null);
        entity.setName(NAME);
        entity.setClusterName(CLUSTER_NAME);
        entity.setTenantName(TENANT_NAME);
        entity.setEnabled(true);
        entity.setVisibleScope(visibleScope);
        entity.setCreatorId(CREATOR_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setType(ConnectType.OB_MYSQL);
        entity.setDialectType(DialectType.OB_MYSQL);
        entity.setCreateTime(null);
        entity.setUpdateTime(null);
        entity.setTemp(false);
        if (visibleScope == ConnectionVisibleScope.PRIVATE) {
            entity.setOwnerId(CREATOR_ID);
        } else {
            entity.setOwnerId(ORGANIZATION_ID);
        }
        return entity;
    }
}
