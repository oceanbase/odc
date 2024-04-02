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
package com.oceanbase.odc.metadb.dbobject;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.test.tool.TestRandom;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author gaoda.xy
 * @date 2024/3/27 19:15
 */
public class DBObjectRepositoryTest extends ServiceTestEnv {

    @Autowired
    private DBObjectRepository dbObjectRepository;

    @Before
    public void setUp() {
        dbObjectRepository.deleteAll();
    }

    @After
    public void tearDown() {
        dbObjectRepository.deleteAll();
    }

    @Test
    public void test_findAll() {
        List<DBObjectEntity> entities = Arrays.asList(
                TestRandom.nextObject(DBObjectEntity.class),
                TestRandom.nextObject(DBObjectEntity.class),
                TestRandom.nextObject(DBObjectEntity.class));
        dbObjectRepository.saveAll(entities);
        Assert.assertEquals(entities.size(), dbObjectRepository.findAll().size());
    }

    @Test
    public void test_findTop1000ByDatabaseIdInAndNameLike() {
        DBObjectEntity entity1 = TestRandom.nextObject(DBObjectEntity.class);
        entity1.setDatabaseId(1L);
        entity1.setName("object_for_test_1");
        DBObjectEntity entity2 = TestRandom.nextObject(DBObjectEntity.class);
        entity2.setDatabaseId(2L);
        entity2.setName("object_for_test_2");
        dbObjectRepository.saveAll(Arrays.asList(entity1, entity2));
        dbObjectRepository.flush();
        List<DBObjectEntity> entities =
                dbObjectRepository.findTop1000ByDatabaseIdInAndNameLike(Arrays.asList(1L, 2L), "%test%");
        Assert.assertEquals(2, entities.size());
    }

    @Test
    public void test_findTop1000ByDatabaseIdInAndTypeAndNameLike() {
        DBObjectEntity entity1 = TestRandom.nextObject(DBObjectEntity.class);
        entity1.setDatabaseId(1L);
        entity1.setType(DBObjectType.TABLE);
        entity1.setName("table_for_test_1");
        DBObjectEntity entity2 = TestRandom.nextObject(DBObjectEntity.class);
        entity2.setDatabaseId(1L);
        entity2.setType(DBObjectType.VIEW);
        entity2.setName("view_for_test_1");
        dbObjectRepository.saveAll(Arrays.asList(entity1, entity2));
        List<DBObjectEntity> entities = dbObjectRepository.findTop1000ByDatabaseIdInAndTypeAndNameLike(
                Arrays.asList(1L, 2L), DBObjectType.VIEW, "%test%");
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals("view_for_test_1", entities.get(0).getName());
    }

}
