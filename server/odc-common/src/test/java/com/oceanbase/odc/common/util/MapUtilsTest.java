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
package com.oceanbase.odc.common.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class MapUtilsTest {

    @Test(expected = IllegalArgumentException.class)
    public void newMap_isEmpty() {
        MapUtils.newMap(String.class, String.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newMap_isNotEven() {
        String[] entries = {"key1", "val1", "key2"};
        MapUtils.newMap(String.class, String.class, entries);
    }

    @Test
    public void newMap_isEven() {
        String[] entries = {"key1", "val1", "key2", "val2"};
        MapUtils.newMap(String.class, String.class, entries);
    }

    @Test
    public void fromString_empty_expect_empty() {
        Map<String, String> stringStringMap = MapUtils.fromKvString("");

        Assert.assertArrayEquals(new String[0], stringStringMap.values().toArray());
    }

    @Test
    public void fromString_single_kv() {
        Map<String, String> stringStringMap = MapUtils.fromKvString("a=1");

        Assert.assertArrayEquals(new String[] {"1"}, stringStringMap.values().toArray());
    }

    @Test
    public void fromString_multiple_kv() {
        Map<String, String> stringStringMap = MapUtils.fromKvString("a=1,b=2");

        Assert.assertArrayEquals(new String[] {"1", "2"}, stringStringMap.values().toArray());
    }

    @Test
    public void formatKvString() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        String result = MapUtils.formatKvString(map);
        Assert.assertEquals("a=1,b=2", result);
    }
}
