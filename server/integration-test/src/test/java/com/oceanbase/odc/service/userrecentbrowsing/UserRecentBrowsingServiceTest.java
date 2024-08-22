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
package com.oceanbase.odc.service.userrecentbrowsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.oceanbase.odc.metadb.userrecentbrowsing.UserRecentBrowsingEntity;
import com.oceanbase.odc.metadb.userrecentbrowsing.UserRecentBrowsingRepository;
import com.oceanbase.odc.server.OdcServer;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OdcServer.class)
@EnableTransactionManagement
public class UserRecentBrowsingServiceTest {

    @Autowired
    private UserRecentBrowsingService userRecentBrowsingService;

    @Autowired
    private UserRecentBrowsingRepository userRecentBrowsingRepository;

    private Long projectId;
    private Long userId;
    private Long itemId;
    private BrowseItemType itemType;
    private Long browseMills;

    @Before
    public void setUp() {
        projectId = System.currentTimeMillis();
        userId = 1L;
        itemId = 1L;
        itemType = BrowseItemType.WORKSHEET;
        browseMills = System.currentTimeMillis();
    }

    @After
    public void tearDown() {
        userRecentBrowsingRepository.delete(UserRecentBrowsingEntity.builder()
                .projectId(projectId).build());
    }

    @Test
    public void add() {
        UserRecentBrowsingEntity entity =
                userRecentBrowsingService.add(projectId, userId, itemType, itemId, new Date(browseMills));
        assertNotNull(entity);
        assertNotNull(entity.getId());
        entity = userRecentBrowsingRepository.findById(entity.getId()).get();
        assertEquals(projectId, entity.getProjectId());
        assertEquals(userId, entity.getUserId());
        assertEquals(itemType.name(), entity.getItemType());
        assertEquals(itemId, entity.getItemId());
        assertTimeEqualsInSeconds(browseMills, entity.getBrowseTime());

        UserRecentBrowsingEntity entity2 =
                userRecentBrowsingService.add(projectId, userId, itemType, itemId, new Date(browseMills + 1000));
        assertNotNull(entity2);
        assertEquals(entity2.getId(), entity.getId());
        entity2 = userRecentBrowsingRepository.findById(entity2.getId()).get();
        assertEquals(projectId, entity2.getProjectId());
        assertEquals(userId, entity2.getUserId());
        assertEquals(itemType.name(), entity2.getItemType());
        assertEquals(itemId, entity2.getItemId());
        assertTimeEqualsInSeconds(browseMills + 1000, entity2.getBrowseTime());
    }

    @Test
    public void getByProjectAndUserAndItem() {
        UserRecentBrowsingEntity addedEntity =
                userRecentBrowsingService.add(projectId, userId, itemType, itemId, new Date(browseMills));
        UserRecentBrowsingEntity retrievedEntity =
                userRecentBrowsingService.getByProjectAndUserAndItem(projectId, userId, itemType, itemId);

        assertNotNull(retrievedEntity);
        assertEquals(addedEntity.getId(), retrievedEntity.getId());
        assertTimeEqualsInSeconds(browseMills, retrievedEntity.getBrowseTime());
    }

    @Test
    public void listRecentBrowsingEntitiesWithLimit_Normal() {
        UserRecentBrowsingEntity add1 = userRecentBrowsingService.add(projectId, userId, itemType, itemId,
                new Date(browseMills));
        UserRecentBrowsingEntity add2 = userRecentBrowsingService.add(projectId, userId, BrowseItemType.GIT_REPO,
                itemId + 1,
                new Date(browseMills + 1000));
        UserRecentBrowsingEntity add3 = userRecentBrowsingService.add(projectId, userId, itemType,
                itemId + 2,
                new Date(browseMills - 3000));

        List<UserRecentBrowsingEntity> recentEntities = userRecentBrowsingService
                .listRecentBrowsingEntitiesWithLimit(projectId, userId,
                        Arrays.asList(BrowseItemType.GIT_REPO, BrowseItemType.WORKSHEET), 100);

        assertEquals(3, recentEntities.size());
        assertEquals(add2.getId(), recentEntities.get(0).getId());
        assertEquals(add1.getId(), recentEntities.get(1).getId());
        assertEquals(add3.getId(), recentEntities.get(2).getId());
    }

    @Test
    public void listRecentBrowsingEntitiesWithLimit_ExceedLimit() {
        for (int i = 0; i < 100; i++) {
            userRecentBrowsingService.add(projectId, userId, itemType, itemId + i,
                    new Date(browseMills + i * 1000));
            userRecentBrowsingService.add(projectId, userId, BrowseItemType.GIT_REPO,
                    itemId + 100 + i,
                    new Date(browseMills + i * 1000));
        }

        List<UserRecentBrowsingEntity> recentEntities = userRecentBrowsingService
                .listRecentBrowsingEntitiesWithLimit(projectId, userId,
                        Arrays.asList(BrowseItemType.GIT_REPO, BrowseItemType.WORKSHEET), 100);

        assertEquals(100, recentEntities.size());
        assertTimeEqualsInSeconds(browseMills + 50 * 1000, recentEntities.get(98).getBrowseTime());
        assertTimeEqualsInSeconds(browseMills + 50 * 1000, recentEntities.get(99).getBrowseTime());

        assertEquals(BrowseItemType.GIT_REPO.name(), recentEntities.get(98).getItemType());
        assertEquals(BrowseItemType.WORKSHEET.name(), recentEntities.get(99).getItemType());
    }

    private void assertTimeEqualsInSeconds(Long ts, Date time) {
        assertEquals(ts / 1_000, time.getTime() / 1_000);
    }
}
