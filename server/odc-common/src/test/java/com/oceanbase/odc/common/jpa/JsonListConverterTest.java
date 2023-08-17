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
package com.oceanbase.odc.common.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class JsonListConverterTest {

    private JsonListConverter converter = new JsonListConverter();

    @Test
    public void test_convert_empty_to_database_column() {
        String s = converter.convertToDatabaseColumn(new ArrayList<>());

        Assert.assertEquals("[]", s);
    }

    @Test
    public void test_convert_three_strings_to_database_column() {
        String s = converter
                .convertToDatabaseColumn(Arrays.asList("abcd", "this is another sentence", "test list convert"));

        Assert.assertEquals("[\"abcd\",\"this is another sentence\",\"test list convert\"]", s);
    }

    @Test
    public void test_convert_empty_to_entity_attribute() {
        List<String> stringList = converter.convertToEntityAttribute("[]");

        List<String> expected = new ArrayList<>();
        Assert.assertEquals(expected, stringList);
    }

    @Test
    public void test_convert_three_strings_to_entity_attribute() {
        List<String> stringList =
                converter.convertToEntityAttribute("[\"abcd\",\"this is another sentence\",\"test list convert\"]");

        List<String> expected = new ArrayList<>();
        expected.add("abcd");
        expected.add("this is another sentence");
        expected.add("test list convert");
        Assert.assertEquals(expected, stringList);
    }
}
