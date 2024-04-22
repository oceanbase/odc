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
package com.oceanbase.odc.metadb.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.jpa.JsonExampleEntity.JsonExampleNested;

public class JsonTypeTest extends ServiceTestEnv {

    @Autowired
    JsonExampleRepository jsonExampleRepository;


    /**
     * CREATE TABLE IF NOT EXISTS `json_example_entity` ( `id` INT(11) NOT NULL PRIMARY KEY,
     * `json_example` TEXT DEFAULT NULL, `json_array` TEXT NULL DEFAULT NULL, );
     */
    @Before
    public void init() {
        jsonExampleRepository.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS json_example_entity (\n"
                + "    id INT NOT NULL PRIMARY KEY,\n"
                + "    json_example TEXT DEFAULT NULL,\n"
                + "    json_array TEXT NULL DEFAULT NULL\n"
                + "    );");
        JsonExampleEntity entity = new JsonExampleEntity();
        entity.setId(1L);
        entity.setJsonExample(new JsonExampleNested("111", 222));
        ArrayList<List<String>> lists = new ArrayList<>();
        lists.add(Arrays.asList("123", "456"));
        entity.setJsonArray(lists);
        jsonExampleRepository.save(entity);
    }

    @Test
    public void test_jsonExampleEntity_deserialization() {
        Optional<JsonExampleEntity> byId = jsonExampleRepository.findById(1L);
        Assert.assertTrue(byId.isPresent());
        JsonExampleNested jsonExampleEntity = byId.get().getJsonExample();
        Assert.assertEquals("111", jsonExampleEntity.getNestedField1());
        Assert.assertEquals(222, jsonExampleEntity.getNestedField2());
        List<List<String>> jsonArray = byId.get().getJsonArray();
        Assert.assertEquals(Arrays.asList("123", "456"), jsonArray.get(0));
    }
}
