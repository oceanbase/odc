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
package com.oceanbase.odc.plugin.connect.obmysql.queryprofile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.oceanbase.odc.common.graph.GraphVertex;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.model.OBSqlPlan;
import com.oceanbase.odc.core.shared.model.PredicateKey;
import com.oceanbase.odc.core.shared.model.SqlPlanGraph;
import com.oceanbase.odc.plugin.connect.obmysql.diagnose.PlanGraphBuilder;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/12
 */
public class PlanGraphBuilderTest {
    private static final Map<String, String> PARAMS = new LinkedHashMap<>();

    @BeforeClass
    public static void setUp() {
        PARAMS.put(":1", "'ASIA'");
        PARAMS.put(":2", "1");
        PARAMS.put(":3", "'1995-01-01 00:00:00'");
        PARAMS.put(":4", "'1996-12-30 00:00:00'");
    }

    @Test
    public void test_BuildPlanGraph_BuildSuccess() {
        List<OBSqlPlan> records = getPlanRecords();
        SqlPlanGraph graph = PlanGraphBuilder.buildPlanGraph(records);
        List<GraphVertex> vertices = graph.getTopoOrderedVertices();
        Assert.assertEquals(vertices.size(), records.size());
    }

    @Test
    public void test_ParseAccessPredicates() {
        String accessPredicates = "access([ORDERS.O_ORDERKEY], [ORDERS.O_CUSTKEY])";
        Map<String, List<String>> access = PlanGraphBuilder.parsePredicates(accessPredicates, PARAMS);
        List<String> columns = access.get(PredicateKey.getLabel("access"));
        Assert.assertEquals(Arrays.asList("ORDERS.O_ORDERKEY", "ORDERS.O_CUSTKEY"), columns);
    }

    @Test
    public void test_ParseFilterPredicates() {
        String filterPredicates = "filter([ORDERS.O_ORDERDATE >= :3], [ORDERS.O_ORDERDATE <= :4])";
        Map<String, List<String>> filter = PlanGraphBuilder.parsePredicates(filterPredicates, PARAMS);
        List<String> colums = filter.get(PredicateKey.getLabel("filter"));
        Assert.assertEquals(Arrays.asList(
                "ORDERS.O_ORDERDATE >= '1995-01-01 00:00:00'", "ORDERS.O_ORDERDATE <= '1996-12-30 00:00:00'"),
                colums);
    }

    @Test
    public void test_BuildPlanGraphByJsonMap() {
        String json = "{\n"
                + "  \"ID\": 0,\n"
                + "  \"OPERATOR\": \"SORT\",\n"
                + "  \"NAME\": \"\",\n"
                + "  \"EST.ROWS\": 697,\n"
                + "  \"EST.TIME(us)\": 817249,\n"
                + "  \"CHILD_1\": {\n"
                + "    \"ID\": 1,\n"
                + "    \"OPERATOR\": \"HASH GROUP BY\",\n"
                + "    \"NAME\": \"\",\n"
                + "    \"EST.ROWS\": 697,\n"
                + "    \"EST.TIME(us)\": 817050\n"
                + "  },\n"
                + "  \"CHILD_2\": {\n"
                + "    \"ID\": 2,\n"
                + "    \"OPERATOR\": \"MATERIAL\",\n"
                + "    \"NAME\": \"\",\n"
                + "    \"EST.ROWS\": 25,\n"
                + "    \"EST.TIME(us)\": 5,\n"
                + "    \"output\": \"output([N2.N_NAME])\"\n"
                + "  }\n"
                + "}";
        Map<String, String> outputFilters = ImmutableMap.of("0",
                "output([ORDERS.O_ORDERKEY], [ORDERS.O_ORDERDATE], [ORDERS.O_CUSTKEY]), filter([ORDERS.O_ORDERDATE >= '1995-01-01 00:00:00'], [ORDERS.O_ORDERDATE <= \n"
                        + "      '1996-12-30 00:00:00']), rowset=256\n"
                        + "      access([ORDERS.O_ORDERKEY], [ORDERS.O_ORDERDATE], [ORDERS.O_CUSTKEY]), partitions(p0)\n"
                        + "      is_index_back=false, is_global_index=false, filter_before_indexback[false,false], \n"
                        + "      range_key([ORDERS.O_ORDERKEY], [ORDERS.O_ORDERDATE], [ORDERS.O_CUSTKEY]), range(MIN,MIN,MIN ; MAX,MAX,MAX)always true");
        SqlPlanGraph graph = PlanGraphBuilder.buildPlanGraph(
                JsonUtils.fromJsonMap(json, String.class, Object.class), outputFilters);
        Assert.assertEquals(3, graph.getVertexList().size());
        Assert.assertEquals(2, graph.getEdgeList().size());
    }

    private List<OBSqlPlan> getPlanRecords() {
        OBSqlPlan plan1 = new OBSqlPlan();
        plan1.setId("0");
        plan1.setParentId("-1");
        plan1.setObjectName("O1");

        OBSqlPlan plan2 = new OBSqlPlan();
        plan2.setId("1");
        plan2.setParentId("0");
        plan2.setObjectName("O2");

        OBSqlPlan plan3 = new OBSqlPlan();
        plan3.setId("2");
        plan3.setParentId("0");
        plan3.setObjectName("O3");

        OBSqlPlan plan4 = new OBSqlPlan();
        plan4.setId("3");
        plan4.setParentId("2");
        plan4.setObjectName("O4");

        return Arrays.asList(plan1, plan2, plan3, plan4);
    }

}
