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

import java.util.stream.Collectors;

import com.oceanbase.odc.core.flow.graph.GraphEdge;
import com.oceanbase.odc.service.queryprofile.model.Operator;
import com.oceanbase.odc.service.queryprofile.model.SqlPlanGraph;
import com.oceanbase.odc.service.queryprofile.vo.GraphEdgeVO;
import com.oceanbase.odc.service.queryprofile.vo.OperatorVO;
import com.oceanbase.odc.service.queryprofile.vo.SqlPlanGraphVO;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/12
 */
public class PlanGraphMapper {

    public static SqlPlanGraphVO toVO(SqlPlanGraph graph) {
        SqlPlanGraphVO vo = new SqlPlanGraphVO();
        vo.setOverview(graph.getOverview());
        vo.setStatistics(graph.getStatistics());
        vo.setVertexList(graph.getVertexList().stream()
                .map(vertex -> mapVertex((Operator) vertex)).collect(Collectors.toList()));
        vo.setEdgeList(graph.getEdgeList().stream()
                .map(PlanGraphMapper::mapEdge).collect(Collectors.toList()));
        return vo;
    }

    private static GraphEdgeVO mapEdge(GraphEdge edge) {
        GraphEdgeVO vo = new GraphEdgeVO();
        vo.setFrom(edge.getFrom().getGraphId());
        vo.setTo(edge.getTo().getGraphId());
        vo.setWeight(edge.getWeight());
        return vo;
    }

    private static OperatorVO mapVertex(Operator vertex) {
        OperatorVO vo = new OperatorVO();
        vo.setGraphId(vertex.getGraphId());
        vo.setName(vertex.getName());
        vo.setTitle(vertex.getTitle());
        vo.setStatus(vertex.getStatus());
        vo.setAttributes(vertex.getAttributes());
        vo.setOverview(vertex.getOverview());
        vo.setStatistics(vertex.getStatistics());
        return vo;
    }

}
