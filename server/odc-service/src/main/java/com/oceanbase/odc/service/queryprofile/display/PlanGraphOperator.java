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

import java.util.List;
import java.util.Map;

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
    private Map<String, Object> attributes;
    private Map<String, String> statistics;
    private Map<String, String> overview;
    private List<PlanGraphEdge> inEdges;
    private List<PlanGraphEdge> outEdges;

    public void putAttribute(@NonNull String key, Object value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }

    public void putOverview(@NonNull String key, String value) {
        if (value != null) {
            overview.put(key, value);
        }
    }

    public void putStatistics(@NonNull String key, String value) {
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
}
