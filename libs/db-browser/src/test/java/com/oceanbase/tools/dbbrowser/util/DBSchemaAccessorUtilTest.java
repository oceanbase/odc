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
package com.oceanbase.tools.dbbrowser.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author jingtian
 */
public class DBSchemaAccessorUtilTest {
    @Test
    public void parseListRangePartitionDescription_MultiValue_Equals() {
        String description = "(1,2),(3,4)";
        List<List<String>> valuesList = DBSchemaAccessorUtil.parseListRangePartitionDescription(description);
        Assert.assertEquals(2, valuesList.size());
        Assert.assertEquals(2, valuesList.get(0).size());
        Assert.assertEquals("1", valuesList.get(0).get(0));
        Assert.assertEquals("2", valuesList.get(0).get(1));
    }

    @Test
    public void parseListRangePartitionDescription_MultiValueNotSame_Equals() {
        String description = "(1,2),(3,4,5)";
        List<List<String>> valuesList = DBSchemaAccessorUtil.parseListRangePartitionDescription(description);
        Assert.assertEquals(2, valuesList.size());
        Assert.assertEquals(2, valuesList.get(0).size());
        Assert.assertEquals(3, valuesList.get(1).size());
        Assert.assertEquals("1", valuesList.get(0).get(0));
        Assert.assertEquals("3", valuesList.get(1).get(0));
    }

    @Test
    public void parseListRangePartitionDescription_SingleValue_Equals() {
        String description = "1,2";
        List<List<String>> valuesList = DBSchemaAccessorUtil.parseListRangePartitionDescription(description);
        Assert.assertEquals(2, valuesList.size());
        Assert.assertEquals(1, valuesList.get(0).size());
        Assert.assertEquals("1", valuesList.get(0).get(0));
        Assert.assertEquals("2", valuesList.get(1).get(0));
    }

    @Test
    public void parseListRangeValuesList_Empty_Equals() {
        List<List<String>> valuesList = new ArrayList<>();
        Assert.assertEquals("", DBSchemaAccessorUtil.parseListRangeValuesList(valuesList));
    }

    @Test
    public void parseListRangeValuesList_EmptySingleValue_Equals() {
        List<List<String>> valuesList = new ArrayList<>();
        valuesList.add(new ArrayList<>());
        Assert.assertEquals("", DBSchemaAccessorUtil.parseListRangeValuesList(valuesList));
    }

    @Test
    public void parseListRangeValuesList_EmptySingleValueAndNotEmptyValues_Equals() {
        List<List<String>> valuesList = new ArrayList<>();
        valuesList.add(new ArrayList<>());
        valuesList.add(Arrays.asList("1", "2"));
        Assert.assertEquals(",(1,2)", DBSchemaAccessorUtil.parseListRangeValuesList(valuesList));
    }


    @Test
    public void parseListRangeValuesList_ListRange_Equals() {
        List<List<String>> valuesList = new ArrayList<>();
        valuesList.add(Arrays.asList("1", "2"));
        valuesList.add(Arrays.asList("3", "4"));
        Assert.assertEquals("(1,2),(3,4)", DBSchemaAccessorUtil.parseListRangeValuesList(valuesList));
    }

    @Test
    public void parseListRangeValuesList_ListRangeNotSameSize_Equals() {
        List<List<String>> valuesList = new ArrayList<>();
        valuesList.add(Arrays.asList("1"));
        valuesList.add(Arrays.asList("3", "4"));
        Assert.assertEquals("1,(3,4)", DBSchemaAccessorUtil.parseListRangeValuesList(valuesList));
    }

    @Test
    public void parseListRangeValuesList_ListRangeNotMultiSameSize_Equals() {
        List<List<String>> valuesList = new ArrayList<>();
        valuesList.add(Arrays.asList("1", "2", "3"));
        valuesList.add(Arrays.asList("4", "5"));
        Assert.assertEquals("(1,2,3),(4,5)", DBSchemaAccessorUtil.parseListRangeValuesList(valuesList));
    }

    @Test
    public void parseListRangeValuesList_List_Equals() {
        List<List<String>> valuesList = new ArrayList<>();
        valuesList.add(Arrays.asList("1"));
        valuesList.add(Arrays.asList("2"));
        Assert.assertEquals("1,2", DBSchemaAccessorUtil.parseListRangeValuesList(valuesList));
    }

}
