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
package com.oceanbase.odc.service.resourcehistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.resourcehistory.ResourceLastAccessEntity;
import com.oceanbase.odc.metadb.resourcehistory.ResourceLastAccessRepository;
import com.oceanbase.odc.server.OdcServer;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OdcServer.class)
@EnableTransactionManagement
public class ResourceLastAccessServiceTest {

    @Autowired
    private ResourceLastAccessService resourceLastAccessService;

    @Autowired
    private ResourceLastAccessRepository resourceLastAccessRepository;

    private Long organizationId = 1L;
    private Long projectId = 1L;
    private Long userId = 1L;
    private Long itemId = 1L;
    private ResourceType resourceType = ResourceType.ODC_WORKSHEET;
    private Long browseMills;

    @Before
    public void setUp() {
        clearDataInOrganization();
        browseMills = System.currentTimeMillis();
    }

    @After
    public void tearDown() {
        clearDataInOrganization();
        resourceLastAccessRepository.delete(ResourceLastAccessEntity.builder()
                .organizationId(organizationId).projectId(projectId).build());
    }

    @Test
    public void batchAdd() {
        ResourceLastAccessEntity entity =
                resourceLastAccessService.add(organizationId, projectId, userId, resourceType, itemId,
                        new Date(browseMills));
        ResourceLastAccessEntity findEntity = resourceLastAccessRepository.findById(entity.getId()).get();
        assertTimeEqualsInSeconds(browseMills, findEntity.getLastAccessTime());
        entity.setLastAccessTime(new Date(browseMills + 1000));
        ResourceLastAccessEntity entity2 = ResourceLastAccessEntity.builder()
                .organizationId(organizationId).projectId(projectId).userId(userId)
                .resourceType(resourceType.getLocalizedMessage()).resourceId(itemId)
                .lastAccessTime(new Date(browseMills + 2000))
                .build();
        int affectCount = resourceLastAccessService.batchAdd(Arrays.asList(entity, entity2));
        assertEquals(2, affectCount);
    }

    @Test
    public void add() {
        // insert
        ResourceLastAccessEntity entity =
                resourceLastAccessService.add(organizationId, projectId, userId, resourceType, itemId,
                        new Date(browseMills));
        ResourceLastAccessEntity findEntity = resourceLastAccessRepository.findById(entity.getId()).get();
        assertTimeEqualsInSeconds(browseMills, findEntity.getLastAccessTime());

        // update
        ResourceLastAccessEntity entity2 =
                resourceLastAccessService.add(organizationId, projectId, userId, resourceType, itemId,
                        new Date(browseMills + 1000));
        ResourceLastAccessEntity findEntity2 = resourceLastAccessRepository.findById(entity2.getId()).get();
        assertTimeEqualsInSeconds(browseMills + 1000, findEntity2.getLastAccessTime());
    }

    @Test
    public void detail() {
        resourceLastAccessService.add(organizationId, projectId, userId, resourceType, itemId,
                new Date(browseMills));
        Optional<ResourceLastAccessEntity> findEntityOptional =
                resourceLastAccessService.detail(organizationId, projectId, userId, resourceType, itemId);
        assertTrue(findEntityOptional.isPresent());
        assertTimeEqualsInSeconds(browseMills, findEntityOptional.get().getLastAccessTime());
    }

    @Test
    public void listLastAccessesOfUser() {
        int totalSize = 99;
        int pageSize = 10;
        for (int i = 0; i < totalSize; i++) {
            resourceLastAccessService.add(organizationId, projectId, userId, resourceType, itemId + i,
                    new Date(browseMills + i * 1000));
        }
        // first page
        PageRequest page = PageRequest.of(0, pageSize, Sort.by(Direction.DESC, "lastAccessTime"));
        Page<ResourceLastAccessEntity> lastAccessEntitiesEntities = resourceLastAccessService
                .listLastAccessesOfUser(organizationId, projectId, userId, resourceType, page);
        assertEquals(totalSize, lastAccessEntitiesEntities.getTotalElements());
        assertEquals(pageSize, lastAccessEntitiesEntities.getContent().size());
        assertTimeEqualsInSeconds(browseMills + (totalSize - 1) * 1000,
                lastAccessEntitiesEntities.toList().get(0).getLastAccessTime());

        // last page
        int lastPage = totalSize % pageSize == 0 ? totalSize / pageSize - 1 : totalSize / pageSize;
        page = PageRequest.of(lastPage, pageSize, Sort.by(Direction.DESC, "lastAccessTime"));
        lastAccessEntitiesEntities = resourceLastAccessService
                .listLastAccessesOfUser(organizationId, projectId, userId, resourceType, page);
        assertEquals(totalSize % pageSize, lastAccessEntitiesEntities.getContent().size());
        assertTimeEqualsInSeconds(browseMills,
                lastAccessEntitiesEntities.toList().get(totalSize % pageSize - 1).getLastAccessTime());
    }

    private void assertTimeEqualsInSeconds(Long ts, Date time) {
        assertEquals(ts / 1_000, time.getTime() / 1_000);
    }

    private void clearDataInOrganization() {
        resourceLastAccessRepository.delete(ResourceLastAccessEntity.builder()
                .organizationId(organizationId).build());
    }
}
