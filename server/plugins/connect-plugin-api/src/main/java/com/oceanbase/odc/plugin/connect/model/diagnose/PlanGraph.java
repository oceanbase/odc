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
package com.oceanbase.odc.plugin.connect.model.diagnose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.model.QueryStatus;

import lombok.Data;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/11
 */
@Data
public class PlanGraph {
    private String traceId;
    private String planId;
    private Long duration;
    private QueryStatus status;
    private List<PlanGraphOperator> vertexes = new ArrayList<>();
    /**
     * Represents information of the execution overview. The reason for using {@link Object} as the
     * parameter type here is that value may be a number or a String. Only when key="CPU time" or "I/O
     * wait time", value is of numerical type.
     */
    private Map<String, Object> overview = new LinkedHashMap<>();
    private Map<String, String> statistics;
    private Map<String, List<String>> topNodes;

    @JsonIgnore
    private final Map<String, PlanGraphOperator> graphId2Operator = new HashMap<>();

    public void setVertexes(List<PlanGraphOperator> vertexes) {
        this.vertexes = vertexes;
        vertexes.forEach(v -> graphId2Operator.put(v.getGraphId(), v));
    }

    public void addStatistics(String key, String value) {
        if (value == null) {
            return;
        }
        if (statistics == null) {
            statistics = new LinkedHashMap<>();
        }
        if (StringUtils.isNumeric(value)) {
            long val = Long.parseLong(value) + Long.parseLong(statistics.getOrDefault(key, "0"));
            statistics.put(key, val + "");
        } else {
            statistics.put(key, value);
        }
    }

    public void putOverview(String key, Object value) {
        if (value != null) {
            overview.put(key, value);
        }
    }

    public PlanGraphOperator getOperator(String id) {
        return graphId2Operator.get(id);
    }

    @JsonGetter
    public Map<String, String> getStatistics() {
        if (statistics == null || statistics.isEmpty()) {
            return statistics;
        }
        for (Entry<String, String> entry : statistics.entrySet()) {
            if (entry.getKey().contains("byte") && StringUtils.isNumeric(entry.getValue())) {
                long bytes = Long.parseLong(entry.getValue());
                entry.setValue(BinarySizeUnit.B.of(bytes).toString());
            }
        }
        return statistics;
    }

}
