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
package com.oceanbase.odc.service.queryprofile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oceanbase.odc.core.flow.graph.GraphVertex;
import com.oceanbase.odc.service.queryprofile.helper.PlanGraphBuilder;
import com.oceanbase.odc.service.queryprofile.model.OBPlanDO;
import com.oceanbase.odc.service.queryprofile.model.PredicateKey;
import com.oceanbase.odc.service.queryprofile.model.SqlPlanGraph;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/12
 */
public class PlanGraphBuilderTest {
    private static final Map<String, String> PARAMS = new LinkedHashMap<>();

    @BeforeClass
    public static void setUp() {
        PARAMS.put(":1", "ASIA");
        PARAMS.put(":2", "1");
        PARAMS.put(":3", "'1995-01-01 00:00:00'");
        PARAMS.put(":4", "'1996-12-30 00:00:00'");
    }

    @Test
    public void test_BuildPlanGraph_BuildSuccess() {
        List<OBPlanDO> records = getPlanRecords();
        SqlPlanGraph graph = PlanGraphBuilder.buildPlanGraph(records);
        List<GraphVertex> vertices = graph.getTopoOrderedVertices();
        Assert.assertEquals(vertices.size(), records.size());
    }

    @Test
    public void test_ParseAccessPredicates() {
        String accessPredicates = "access([ORDERS.O_ORDERKEY], [ORDERS.O_CUSTKEY])";
        Map<String, List<String>> access = PlanGraphBuilder.parsePredicates(accessPredicates, PARAMS);
        List<String> columns = access.get(PredicateKey.access.getDisplayName());
        Assert.assertEquals(Arrays.asList("ORDERS.O_ORDERKEY", "ORDERS.O_CUSTKEY"), columns);
    }

    @Test
    public void test_ParseFilterPredicates() {
        String filterPredicates = "filter([ORDERS.O_ORDERDATE >= :3], [ORDERS.O_ORDERDATE <= :4])";
        Map<String, List<String>> filter = PlanGraphBuilder.parsePredicates(filterPredicates, PARAMS);
        List<String> colums = filter.get(PredicateKey.filter.getDisplayName());
        Assert.assertEquals(Arrays.asList(
                "ORDERS.O_ORDERDATE >= '1995-01-01 00:00:00'", "ORDERS.O_ORDERDATE <= '1996-12-30 00:00:00'"),
                colums);
    }

    private List<OBPlanDO> getPlanRecords() {
        OBPlanDO plan1 = new OBPlanDO();
        plan1.setId("0");
        plan1.setParentId("-1");
        plan1.setObjectName("O1");

        OBPlanDO plan2 = new OBPlanDO();
        plan2.setId("1");
        plan2.setParentId("0");
        plan2.setObjectName("O2");

        OBPlanDO plan3 = new OBPlanDO();
        plan3.setId("2");
        plan3.setParentId("0");
        plan3.setObjectName("O3");

        OBPlanDO plan4 = new OBPlanDO();
        plan4.setId("3");
        plan4.setParentId("2");
        plan4.setObjectName("O4");

        return Arrays.asList(plan1, plan2, plan3, plan4);
    }

}
