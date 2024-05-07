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
package com.oceanbase.odc.service.queryprofile.display;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.common.util.StringUtils;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/11
 */
@Data
public class PlanGraph {
    private List<PlanGraphOperator> vertexes;
    private Map<String, String> statistics;
    private Map<String, String> overview;
    private Map<String, List<String>> topNodes;
    @JsonIgnore
    private final Map<String, PlanGraphOperator> graphId2Operator = new HashMap<>();

    public void setVertexes(List<PlanGraphOperator> vertexes) {
        this.vertexes = vertexes;
        vertexes.forEach(v -> graphId2Operator.put(v.getGraphId(), v));
    }

    public void putStatistics(String key, String value) {
        if (value == null) {
            return;
        }
        if (StringUtils.isNumeric(value)) {
            long val = Long.parseLong(value) + Long.parseLong(statistics.getOrDefault(key, "0"));
            statistics.put(key, val + "");
        } else {
            statistics.put(key, value);
        }
    }

    public void putOverview(String key, String value) {
        if (value != null) {
            overview.put(key, value);
        }
    }

    public void clearStatistics() {
        statistics.clear();
        for (PlanGraphOperator vertex : vertexes) {
            vertex.getStatistics().clear();
        }
    }

    public PlanGraphOperator getOperator(String id) {
        return graphId2Operator.get(id);
    }
}
