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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class JsonPathUtilsTest {
    private String json = "{\"store\":{\"book\":[{\"title\":\"Hello Java\",\"price\":1.0}]}}";

    @Test
    public void read_ReadObjectPathExists_ValueMatch() {
        String path = "$.store.book[0].title";
        String value = JsonPathUtils.read(json, path, String.class);
        Assert.assertEquals("Hello Java", value);
    }

    @Test(expected = PathNotFoundException.class)
    public void read_ReadObjectPathNotExists_ThrowException() {
        String path = "$.store.book[0].otherElement";
        JsonPathUtils.read(json, path, String.class);
    }

    @Test
    public void read_ReadListPathExists_ValueMatch() {
        List<Book> expected = new ArrayList<>();
        expected.add(new Book("Hello Java", 1.0f));

        String path = "$.store.book";
        List<Book> books = JsonPathUtils.read(json, path, new TypeRef<List<Book>>() {});

        Assert.assertArrayEquals(expected.toArray(), books.toArray());
    }

    @Test
    public void readList_PathExists_ValueMatch() {
        List<Book> expected = new ArrayList<>();
        expected.add(new Book("Hello Java", 1.0f));

        String path = "$.store.book";
        List<Book> books = JsonPathUtils.readList(json, path, Book.class);

        Assert.assertArrayEquals(expected.toArray(), books.toArray());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Book {
        private String title;
        private Float price;
    }
}
