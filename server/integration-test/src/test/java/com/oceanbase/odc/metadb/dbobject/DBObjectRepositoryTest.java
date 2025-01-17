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

import java.util.ArrayList;
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

    private static final Long DATABASE_ID = 1L;
    private static final DBObjectType DATASOURCE_TYPE = DBObjectType.SCHEMA;
    private static final List<String> INSERT_ORDER_NAME_LIST = Arrays.asList("C", "B", "A");
    private static final List<String> LEXICAL_ORDER_NAME_LIST = Arrays.asList("A", "B", "C");

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
    public void test_batchCreate() {
        List<DBObjectEntity> entities = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            DBObjectEntity entity = TestRandom.nextObject(DBObjectEntity.class);
            entity.setId(null);
            entity.setDatabaseId(1L);
            entity.setType(DBObjectType.TABLE);
            entity.setName("table_for_test_" + i);
            entities.add(entity);
        }
        List<DBObjectEntity> saved = dbObjectRepository.batchCreate(entities, 200);
        Assert.assertEquals(entities.size(), saved.size());
    }

    @Test
    public void findByDatabaseIdAndType_getListByDatabaseIdAndType_success() {
        List<DBObjectEntity> entities = new ArrayList<>();
        for (String name : INSERT_ORDER_NAME_LIST) {
            DBObjectEntity entity = TestRandom.nextObject(DBObjectEntity.class);
            entity.setName(name);
            entity.setDatabaseId(DATABASE_ID);
            entity.setType(DATASOURCE_TYPE);
            entities.add(entity);
        }
        List<DBObjectEntity> savedEntities = dbObjectRepository.saveAll(entities);
        List<DBObjectEntity> list = dbObjectRepository.findByDatabaseIdAndType(
                DATABASE_ID, DATASOURCE_TYPE);
        Assert.assertEquals(savedEntities.size(), list.size());
        for (int i = 0; i < list.size(); i++) {
            Assert.assertEquals(list.get(i).getDatabaseId(), DATABASE_ID);
            Assert.assertEquals(list.get(i).getType(), DATASOURCE_TYPE);
        }
    }

    @Test
    public void findByDatabaseIdAndTypeOrderByNameAsc_getListSortedByName_success() {
        List<DBObjectEntity> entities = new ArrayList<>();
        for (String name : INSERT_ORDER_NAME_LIST) {
            DBObjectEntity entity = TestRandom.nextObject(DBObjectEntity.class);
            entity.setName(name);
            entity.setDatabaseId(DATABASE_ID);
            entity.setType(DATASOURCE_TYPE);
            entities.add(entity);
        }
        List<DBObjectEntity> savedEntities = dbObjectRepository.saveAll(entities);
        List<DBObjectEntity> list = dbObjectRepository.findByDatabaseIdAndTypeOrderByNameAsc(
                DATABASE_ID, DATASOURCE_TYPE);
        Assert.assertEquals(savedEntities.size(), list.size());
        for (int i = 0; i < list.size(); i++) {
            Assert.assertEquals(list.get(i).getDatabaseId(), DATABASE_ID);
            Assert.assertEquals(list.get(i).getType(), DATASOURCE_TYPE);
            Assert.assertEquals(list.get(i).getName(), LEXICAL_ORDER_NAME_LIST.get(i));
        }
    }
}
