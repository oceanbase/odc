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
package com.oceanbase.odc.service.queryprofile.helper;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Comparators;
import com.oceanbase.odc.common.graph.GraphEdge;
import com.oceanbase.odc.core.shared.model.Operator;
import com.oceanbase.odc.core.shared.model.SqlPlanGraph;
import com.oceanbase.odc.service.queryprofile.display.PlanGraph;
import com.oceanbase.odc.service.queryprofile.display.PlanGraphEdge;
import com.oceanbase.odc.service.queryprofile.display.PlanGraphOperator;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/12
 */
public class PlanGraphMapper {

    public static PlanGraph toVO(SqlPlanGraph graph) {
        PlanGraph vo = new PlanGraph();
        vo.setOverview(graph.getOverview());
        vo.setStatistics(graph.getStatistics());
        vo.setVertexes(graph.getVertexList().stream()
                .map(vertex -> mapVertex((Operator) vertex)).collect(Collectors.toList()));
        return vo;
    }

    private static PlanGraphEdge mapEdge(GraphEdge edge) {
        PlanGraphEdge vo = new PlanGraphEdge();
        vo.setFrom(edge.getFrom().getGraphId());
        vo.setTo(edge.getTo().getGraphId());
        vo.setWeight(edge.getWeight());
        return vo;
    }

    private static PlanGraphOperator mapVertex(Operator vertex) {
        PlanGraphOperator vo = new PlanGraphOperator();
        vo.setGraphId(vertex.getGraphId());
        vo.setName(vertex.getName());
        vo.setTitle(vertex.getTitle());
        vo.setStatus(vertex.getStatus());
        vo.setDuration(vertex.getDuration());
        vo.setAttributes(vertex.getAttributes());
        vo.setOverview(vertex.getOverview());
        vo.setStatistics(vertex.getStatistics());
        vo.setInEdges(vertex.getInEdges().stream().map(PlanGraphMapper::mapEdge).collect(Collectors.toList()));
        vo.setOutEdges(vertex.getOutEdges().stream().map(PlanGraphMapper::mapEdge).collect(Collectors.toList()));
        return vo;
    }

    private static <T> List<T> sort(int k, List<PlanGraphOperator> vertexes, Comparator<PlanGraphOperator> comparator,
            Function<PlanGraphOperator, T> mapper) {
        return vertexes.stream()
                .collect(Comparators.greatest(k, comparator))
                .stream().map(mapper)
                .collect(Collectors.toList());
    }

}
