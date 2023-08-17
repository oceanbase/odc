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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * {@link ListUtilsTest}
 *
 * @author yh263208
 * @date 2022-04-27 18:41
 * @since ODC_release_3.3.1
 */
public class ListUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void cartesianProduct_correctInput_successGet() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("c");
        List<List<String>> result = ListUtils.cartesianProduct(Arrays.asList(a, b));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void cartesianProduct_emptyList_returnEmpty() {
        List<String> a = Arrays.asList("a", "b");
        List<List<String>> result = ListUtils.cartesianProduct(Arrays.asList(a, Collections.emptyList()));
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void sortByTopoOrder_noTopoRelation_originListReturn() {
        List<String> list = new LinkedList<>(Arrays.asList("a", "a", "c", "b", "b"));
        List<String> expect = new LinkedList<>(list);
        ListUtils.sortByTopoOrder(list, (o1, o2) -> 0);
        Assert.assertEquals(expect, list);
    }

    @Test
    public void sortByTopoOrder_topoRelationExists_sortedCorrectly() {
        Map<String, Set<String>> replyRelation = new HashMap<>();
        replyRelation.putIfAbsent("a", new HashSet<>(Arrays.asList("b", "h")));
        replyRelation.putIfAbsent("b", new HashSet<>(Collections.singletonList("c")));
        replyRelation.putIfAbsent("e", new HashSet<>(Collections.singletonList("f")));
        replyRelation.putIfAbsent("f", new HashSet<>(Collections.singletonList("g")));
        List<String> list = new LinkedList<>(Arrays.asList("a", "a", "b", "p", "h", "g", "f", "g", "e", "c", "a", "z"));
        ListUtils.sortByTopoOrder(list, new TopoOrderComparator<>(replyRelation));
        List<String> actual = Arrays.asList("a", "a", "a", "b", "p", "h", "e", "f", "g", "g", "c", "z");
        Assert.assertEquals(actual, list);
    }

    @Test
    public void sortByTopoOrder_circleRefExists_ecpThrown() {
        Map<String, Set<String>> replyRelation = new HashMap<>();
        replyRelation.putIfAbsent("a", new HashSet<>(Arrays.asList("b", "h")));
        replyRelation.putIfAbsent("b", new HashSet<>(Collections.singletonList("c")));
        replyRelation.putIfAbsent("e", new HashSet<>(Collections.singletonList("f")));
        replyRelation.putIfAbsent("f", new HashSet<>(Collections.singletonList("g")));
        replyRelation.putIfAbsent("g", new HashSet<>(Collections.singletonList("e")));
        List<String> list = new LinkedList<>(Arrays.asList("a", "a", "b", "p", "h", "g", "f", "g", "e", "c", "a", "z"));

        thrown.expectMessage("Circular reference detected");
        thrown.expect(IllegalStateException.class);
        ListUtils.sortByTopoOrder(list, new TopoOrderComparator<>(replyRelation));
    }
}
