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
package com.oceanbase.odc.common.json;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtilsTest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class A {

        private String col1;
    }

    @Test
    public void fromJson() {
        A a = JsonUtils.fromJson("{\"col1\":\"abc\"}", A.class);

        Assert.assertEquals("abc", a.getCol1());
    }

    @Test
    public void fromJson_null() {
        A a = JsonUtils.fromJson(null, A.class);

        Assert.assertNull(a);
    }

    @Test
    public void fromJsonList() {
        String json = "[{\"col1\":\"abc\"}]";

        List<A> aList = JsonUtils.fromJsonList(json, A.class);

        List<A> expected = new ArrayList<>();
        expected.add(new A("abc"));
        Assert.assertEquals(expected, aList);
    }

    @Test
    public void fromJson_genericType() {
        Generic<A> g = JsonUtils.fromJson("{}", new TypeReference<Generic<A>>() {});

        Assert.assertNotNull(g);
    }

    @Test
    public void fromJsonList_json_null_expect_null() {
        List<A> aList = JsonUtils.fromJsonList(null, A.class);

        Assert.assertNull(aList);
    }

    @Test
    public void fromJsonMap() {
        String json = "{\"a\":{\"col1\":\"abc\"}}";

        Map<String, A> aMap = JsonUtils.fromJsonMap(json, String.class, A.class);

        Map<String, A> expected = new HashMap<>();
        expected.put("a", new A("abc"));

        Assert.assertEquals(expected, aMap);
    }

    @Test
    public void fromJsonMap_json_null_expect_null() {
        Map<A, A> aMap = JsonUtils.fromJsonMap(null, A.class, A.class);

        Assert.assertNull(aMap);
    }

    @Test
    public void toJson() {
        String json = JsonUtils.toJson(new A("abc"));

        Assert.assertEquals("{\"col1\":\"abc\"}", json);
    }

    @Test
    public void toJson_null() {
        String json = JsonUtils.toJson(null);

        Assert.assertNull(json);
    }

    @Test
    public void toJson_List() {
        String json = JsonUtils.toJson(Collections.singletonList(new A("abc")));

        Assert.assertEquals("[{\"col1\":\"abc\"}]", json);
    }

    @Test
    public void toJson_safety() {
        TTT t = new TTT();
        t.setPassword("password");
        t.setUsername("test");
        t.setA(TT.A);
        String s = JsonUtils.toJson(t);
        Assert.assertEquals("{\"a\":\"A\",\"password\":\"******\",\"username\":\"test\"}", s);
    }

    @Test
    public void toJson_unsafe() {
        TTT t = new TTT();
        t.setPassword("password");
        t.setUsername("test");
        t.setA(TT.A);
        String unsafe = JsonUtils.unsafeToJson(t);
        Assert.assertEquals(unsafe, "{\"a\":\"A\",\"password\":\"password\",\"username\":\"test\"}");
    }

    @Test
    @Ignore("2021-2-23 change to use timestamp format, may recover later")
    public void toJson_offsetDateTime_expectISO() {
        String json = JsonUtils.toJson(OffsetDateTime.parse("2021-02-05T20:29:44.86+08:00"));
        Assert.assertEquals("\"2021-02-05T20:29:44.86+08:00\"", json);
    }

    @Test
    public void toJson_offsetDateTime_expectTimestamp() {
        String json = JsonUtils.toJson(OffsetDateTime.parse("2021-02-05T20:29:44.86+08:00"));
        Assert.assertEquals("1612528184.860000000", json);
    }

    @Test
    public void fromJson_safety() {
        TTT ttt = JsonUtils.fromJson("{\"password\":\"******\",\"username\":\"test\"}", TTT.class);
        Assert.assertEquals("******", ttt.getPassword());
    }

    @Test
    public void convert_value() {
        ObjectMapper objectMapper = JacksonFactory.jsonMapper();
        Map<String, String> map = new HashMap<>();
        map.put("username", "username");
        map.put("firstName", "firstName");
        Foo foo = objectMapper.convertValue(map, Foo.class);
        Assert.assertEquals(foo.getFirstName(), map.get("firstName"));
        Assert.assertEquals(foo.getUsername(), map.get("username"));

    }

    @Test
    public void convert_xmlToJson() {
        String xml =
                "<bookstore><book><author>odc</author><price>99.99</price><title>oceanbase</title></book></bookstore>";
        String excepted = "{\"bookstore\":{\"book\":{\"author\":\"odc\",\"price\":99.99,\"title\":\"oceanbase\"}}}";
        String json = JsonUtils.xmlToJson(xml);
        Assert.assertEquals(excepted, json);
    }

    @Test
    public void convert_jsonToXml() {
        String json = "{\"bookstore\":{\"book\":{\"author\":\"odc\",\"price\":99.99,\"title\":\"oceanbase\"}}}";
        String excepted =
                "<bookstore><book><author>odc</author><price>99.99</price><title>oceanbase</title></book></bookstore>";
        String xml = JsonUtils.jsonToXml(json);
        Assert.assertEquals(excepted, xml);
    }

    @Data
    public static class Generic<T> {
        private T t;
    }

    @Data
    @ToString
    public static class Foo {

        String username;
        String firstName;
    }

    @Data
    public static class TTT {

        @MaskOutput
        private String password;

        private String username;

        private TT a;
    }

    enum TT {

        A;

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(A.name());
        }
    }
}
