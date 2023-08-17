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
package com.oceanbase.odc.service.common.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.common.model.Stats.Aggregation;

import lombok.Data;

/**
 * 统计数据，用于 列表、分页 API 场景，提供部分数据的聚合统计结果 <br>
 * 如前端的筛选交互可选项可以基于聚合统计结果获取。格式是 字段名称 --> 聚合结果 的映射。<br>
 * 样例：
 * 
 * <pre>
 *  {
 *   "tenantName" : {
 *     "count" : {
 *       "T1" : 1.00,
 *       "T2" : 3.00
 *     },
 *     "distinct" : [ "T1", "T2" ]
 *   },
 *   "clusterName" : {
 *     "distinct" : [ "C1", "C2" ]
 *   }
 * }
 * </pre>
 */
@Data
public class Stats extends HashMap<String, Aggregation> {

    public Stats andDistinct(String fieldName, Collection<String> values) {
        Aggregation aggregation = super.computeIfAbsent(fieldName, t -> new Aggregation());
        aggregation.setDistinct(new HashSet<>(values));
        return this;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Aggregation {
        private Set<String> distinct;
        private Map<String, Double> count;
        private Map<String, Double> max;
        private Map<String, Double> min;
        private Map<String, Double> avg;

        public void add(Function function, Map<String, Double> column2Value) {
            switch (function) {
                case count:
                    this.count = column2Value;
                    break;
                case max:
                    this.max = column2Value;
                    break;
                case min:
                    this.min = column2Value;
                    break;
                case avg:
                    this.avg = column2Value;
                    break;
                default:
                    throw new UnsupportedException("Aggregation function not supported");
            }
        }
    }

    public enum Function {
        count,
        max,
        min,
        avg,;
    }

}
