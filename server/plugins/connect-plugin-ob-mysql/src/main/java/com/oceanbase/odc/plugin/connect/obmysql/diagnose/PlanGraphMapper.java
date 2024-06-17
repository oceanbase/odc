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
package com.oceanbase.odc.plugin.connect.obmysql.diagnose;

import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.graph.Graph;
import com.oceanbase.odc.common.graph.GraphEdge;
import com.oceanbase.odc.core.shared.model.Operator;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraph;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraphEdge;
import com.oceanbase.odc.plugin.connect.model.diagnose.PlanGraphOperator;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/12
 */
public class PlanGraphMapper {

    public static PlanGraph toVO(Graph graph) {
        PlanGraph vo = new PlanGraph();
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
        vo.setAttributes((Map) vertex.getAttributes());
        vo.setOverview(vertex.getOverview());
        vo.setStatistics(vertex.getStatistics());
        vo.setInEdges(vertex.getInEdges().stream().map(PlanGraphMapper::mapEdge).collect(Collectors.toList()));
        vo.setOutEdges(vertex.getOutEdges().stream().map(PlanGraphMapper::mapEdge).collect(Collectors.toList()));
        return vo;
    }

}
