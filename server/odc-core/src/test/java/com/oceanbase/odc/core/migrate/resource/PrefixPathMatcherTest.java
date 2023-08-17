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
package com.oceanbase.odc.core.migrate.resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.MapperUtils;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.migrate.resource.util.PrefixPathMatcher;
import com.oceanbase.odc.core.migrate.tool.TestObject;
import com.oceanbase.odc.core.migrate.tool.TestObjectMeta;

/**
 * Test cases for {@link MapperUtils}
 *
 * @author yh263208
 * @date 2022-04-21 10:26
 * @since ODC_release_3.3.1
 */
public class PrefixPathMatcherTest {

    @Test
    public void get_rightPrefix_returnNotNull() throws IOException {
        Map<Object, Object> map = getResource();
        Object value = MapperUtils.get(map,
                new PrefixPathMatcher("templates.2.specs.1.value_from.db_ref.filters.1.value_from.db_ref.ref_table"));
        Assert.assertEquals("iam_user_role", value);
    }

    @Test
    public void get_wildcardPerfix_returnListOfResult() throws IOException {
        Map<Object, Object> map = getResource();
        Object value = MapperUtils.get(map, new PrefixPathMatcher("templates.*.specs.0.value"));
        Assert.assertTrue(value instanceof List);
    }

    @Test
    public void get_wrongPrefix_returnNull() throws IOException {
        Map<Object, Object> map = getResource();
        Object value = MapperUtils.get(map,
                new PrefixPathMatcher("templates.2.specs.1.aaas.db_ref.filters.1.value_from.db_ref.ref_table"));
        Assert.assertNull(value);
    }

    @Test
    public void getOrDefault_rightPrefix_returnNotNull() throws IOException {
        Map<Object, Object> map = getResource();
        Object value = MapperUtils.getOrDefault(map, null,
                new PrefixPathMatcher("templates.2.specs.1.value_from.db_ref.filters.1.value_from.db_ref.ref_table"));
        Assert.assertEquals("iam_user_role", value);
    }

    @Test
    public void getOrDefault_wrongPrefix_returnDefaultVal() throws IOException {
        Map<Object, Object> map = getResource();
        String defaultStr = "Helllo, world";
        Object value = MapperUtils.getOrDefault(map, defaultStr,
                new PrefixPathMatcher("templates.2.specs.1.aaas.db_ref.filters.1.value_from.db_ref.ref_table"));
        Assert.assertEquals(defaultStr, value);
    }

    @Test
    public void get_rightSerializedType_returnNotNull() throws IOException {
        Map<Object, Object> map = getResource();
        TestObject value = MapperUtils.get(map, TestObject.class, new PrefixPathMatcher("templates.0.specs.0"));
        Assert.assertEquals("id", value.getName());
    }

    @Test(expected = RuntimeException.class)
    public void get_wrongSerializedType_ExpThrown() throws IOException {
        Map<Object, Object> map = getResource();
        TestObjectMeta value = MapperUtils.get(map, TestObjectMeta.class, new PrefixPathMatcher("templates.0.specs.0"));
    }

    @Test
    public void get_listOfSerializedType_returnNotNull() throws IOException {
        Map<Object, Object> map = getResource();
        List<TestObject> values =
                MapperUtils.get(map, new TypeReference<List<TestObject>>() {},
                        new PrefixPathMatcher("templates.0.specs"));
        Assert.assertEquals(2, values.size());
    }

    @Test
    public void get_modifyData_successModified() throws IOException {
        Map<Object, Object> map = getResource();
        List<Map<String, Object>> values =
                (List<Map<String, Object>>) MapperUtils.get(map, new PrefixPathMatcher("templates.0.specs"));
        String newName = "Hello,world";
        values.get(0).put("column_name", newName);

        List<TestObject> objs =
                MapperUtils.get(map, new TypeReference<List<TestObject>>() {},
                        new PrefixPathMatcher("templates.0.specs"));
        Assert.assertEquals(newName, objs.get(0).getName());
    }

    @Test
    public void get_rangeMatch_retrunRightResult() throws IOException {
        Map<Object, Object> map = getResource();
        Object value = MapperUtils.get(map, new PrefixPathMatcher("templates.[0-1].metadata.table_name"));
        Assert.assertTrue(value instanceof List);
        List<?> list = (List<?>) value;
        Assert.assertTrue(list.contains("iam_user"));
        Assert.assertTrue(list.contains("iam_role"));
    }

    @Test
    public void get_rexMatch_retrunRightResult() throws IOException {
        Map<Object, Object> map = getResource();
        Object value = MapperUtils.get(map, new PrefixPathMatcher("templates.[2].metadata.table_name"));
        Assert.assertEquals("iam_user_role", value);
    }

    private Map<Object, Object> getResource() throws IOException {
        URL url = this.getClass().getClassLoader().getResource("migrate/resource/test-maputils.yml");
        Assert.assertNotNull(url);
        File file = new File(url.getPath());
        Assert.assertTrue(file.exists());
        return YamlUtils.from(FileUtils.readFileToString(file), new TypeReference<Map<Object, Object>>() {});
    }

}
