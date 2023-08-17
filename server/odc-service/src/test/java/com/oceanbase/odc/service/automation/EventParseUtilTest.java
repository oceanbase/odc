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
package com.oceanbase.odc.service.automation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.automation.model.AutomationRule;
import com.oceanbase.odc.service.automation.util.EventParseUtil;

public class EventParseUtilTest {

    @Test
    public void test_Parse_SimpleObject() {
        Object value = EventParseUtil.parseObject("value", "value");
        Assert.assertEquals("value", value);
    }

    @Test
    public void test_Parse_Array() {
        String[] array = new String[] {"a", "b"};
        Object value = EventParseUtil.parseObject(array, "[1]");
        Assert.assertEquals("b", value);
    }

    @Test
    public void test_Parse_List() {
        List<String> list = Collections.singletonList("value");
        Object value = EventParseUtil.parseObject(list, "[0]");
        Assert.assertEquals("value", value);
    }

    @Test
    public void test_Parse_FlatMap() {
        HashMap<String, Object> source = new HashMap<>();
        source.put("deptName", "Research Lab");
        Object deptName = EventParseUtil.parseObject(source, "deptName");
        Assert.assertEquals("Research Lab", deptName);
    }

    @Test
    public void test_Parse_NestedMap() {
        LinkedHashMap<String, Object> BG = new LinkedHashMap<>();
        LinkedHashMap<String, Object> BU = new LinkedHashMap<>();
        LinkedHashMap<String, List<Object>> group = new LinkedHashMap<>();
        List<Object> members = new ArrayList<>();
        members.add("leo");
        members.add(114);
        members.add(new String[] {"Hello", "World"});
        group.put("member", members);
        BU.put("research", group);
        BG.put("IT", BU);

        Object value0 = EventParseUtil.parseObject(BG, "IT.research.member[0]");
        Object value1 = EventParseUtil.parseObject(BG, "IT.research.member[1]");
        Object value2 = EventParseUtil.parseObject(BG, "IT.research.member[2][0]");
        Assert.assertEquals("leo", value0);
        Assert.assertEquals(114, value1);
        Assert.assertEquals("Hello", value2);
    }

    @Test
    public void test_Operate_Number() {
        boolean contains = EventParseUtil.validate(1234567, "contains", 123);
        Assert.assertTrue(contains);
    }

    @Test
    public void test_Operate_String() {
        boolean contains = EventParseUtil.validate("Hello world!", "equals", "Hello world!");
        Assert.assertTrue(contains);
    }

    @Test
    public void test_Operate_Regex() {
        boolean contains = EventParseUtil.validate("Hello world!", "matches", "Hello.*");
        Assert.assertTrue(contains);
    }

    @Test
    public void test_Operate_Array() {
        String[] array = new String[] {"DeptA"};
        boolean contains = EventParseUtil.validate(array, "contains", "Dept");
        Assert.assertTrue(contains);
    }

    @Test
    public void test_Operate_EmptyList() {
        boolean contains = EventParseUtil.validate(Collections.emptyList(), "contains", "some value");
        Assert.assertFalse(contains);
    }

    @Test
    public void test_Operate_BlankValue() {
        boolean contains = EventParseUtil.validate(Collections.singletonList("Hello World!"), "contains", "");
        Assert.assertTrue(contains);
    }

    // public

    @Test
    public void test_CastToMap() throws IllegalAccessException {
        AutomationRule rule = new AutomationRule();
        rule.setName("test");
        Map<String, Object> objectMap = EventParseUtil.castToMap(rule);
        Assert.assertEquals("test", objectMap.get("name"));
    }

    @Test
    public void test_Parse_NestedArray() {
        Map<String, Object> source = new HashMap<>();
        Map<String, String> dept0 = new HashMap<>();
        dept0.put("name", "deptA");
        dept0.put("description", "this is a serious description");
        Map<String, String> dept1 = new HashMap<>();
        dept1.put("name", "deptB");
        dept1.put("description", "this is a serious description");
        Map[] departments = new Map[] {dept0, dept1};
        source.put("departments", departments);

        String expression = "departments[*].name";
        List result = (List) EventParseUtil.parseObject(source, expression);
        Assert.assertEquals("deptA", result.get(0));
        Assert.assertEquals("deptB", result.get(1));
    }

}
