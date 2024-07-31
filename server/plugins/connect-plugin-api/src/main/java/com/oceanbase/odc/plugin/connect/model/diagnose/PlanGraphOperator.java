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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.model.QueryStatus;

import lombok.Data;
import lombok.NonNull;

/**
 * @author liuyizhuo.lyz
 * @date 2024/4/11
 */
@Data
public class PlanGraphOperator {
    private String graphId;
    private String name;
    private String title;
    private QueryStatus status;
    private Long duration;
    private Map<String, List<String>> attributes;
    private Map<String, String> statistics;
    /**
     * Represents information of the execution overview. The reason for using {@link Object} as the
     * parameter type here is that value may be a number or a String. Only when key="CPU time" or "I/O
     * wait time", value is of numerical type.
     */
    private Map<String, Object> overview;
    private List<PlanGraphEdge> inEdges;
    private List<PlanGraphEdge> outEdges;
    private Map<String, PlanGraphOperator> subNodes;

    public void putAttribute(@NonNull String key, List<String> value) {
        if (attributes == null) {
            attributes = new LinkedHashMap<>();
        }
        if (value != null) {
            attributes.put(key, value);
        }
    }

    public void putOverview(@NonNull String key, Object value) {
        if (overview == null) {
            overview = new LinkedHashMap<>();
        }
        if (value != null) {
            overview.put(key, value);
        }
    }

    public void putStatistics(@NonNull String key, String value) {
        if (statistics == null) {
            statistics = new LinkedHashMap<>();
        }
        if (StringUtils.isNotEmpty(value)) {
            statistics.put(key, value);
        }
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
