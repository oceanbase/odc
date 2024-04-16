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

/**
 * @author gaoda.xy
 * @date 2024/3/27 19:11
 */
public class DBColumnRepositoryTest extends ServiceTestEnv {

    @Autowired
    private DBColumnRepository dbColumnRepository;

    @Before
    public void setUp() {
        dbColumnRepository.deleteAll();
    }

    @After
    public void tearDown() {
        dbColumnRepository.deleteAll();
    }

    @Test
    public void test_findAll() {
        List<DBColumnEntity> entities = Arrays.asList(
                TestRandom.nextObject(DBColumnEntity.class),
                TestRandom.nextObject(DBColumnEntity.class),
                TestRandom.nextObject(DBColumnEntity.class));
        dbColumnRepository.saveAll(entities);
        Assert.assertEquals(entities.size(), dbColumnRepository.findAll().size());
    }

    @Test
    public void test_findTop1000ByDatabaseIdInAndNameLike() {
        DBColumnEntity entity1 = TestRandom.nextObject(DBColumnEntity.class);
        entity1.setDatabaseId(1L);
        entity1.setName("database_for_test_1");
        DBColumnEntity entity2 = TestRandom.nextObject(DBColumnEntity.class);
        entity2.setDatabaseId(2L);
        entity2.setName("database_for_test_2");
        dbColumnRepository.saveAll(Arrays.asList(entity1, entity2));
        List<DBColumnEntity> entities =
                dbColumnRepository.findTop1000ByDatabaseIdInAndNameLike(Arrays.asList(1L, 2L, 3L), "%test_1");
        Assert.assertEquals(1, entities.size());
        entities = dbColumnRepository.findTop1000ByDatabaseIdInAndNameLike(Arrays.asList(1L, 2L, 3L), "%test%");
        Assert.assertEquals(2, entities.size());
    }

}
