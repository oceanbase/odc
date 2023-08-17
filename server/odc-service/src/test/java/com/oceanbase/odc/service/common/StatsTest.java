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
package com.oceanbase.odc.service.common;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.model.Stats.Aggregation;
import com.oceanbase.odc.service.common.model.Stats.Function;
import com.oceanbase.odc.test.tool.TestCollections;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatsTest {

    @Test
    public void json_ForGenerateSample() {
        Stats stats = new Stats();
        Aggregation clusterAgg = new Aggregation();
        clusterAgg.setDistinct(Sets.newHashSet("C1", "C2"));

        Aggregation tenantAgg = new Aggregation();
        tenantAgg.setDistinct(Sets.newHashSet("T1", "T2"));

        Map<String, Double> tenant2Count = TestCollections.asMap("T1=1,T2=3").entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, t -> Double.valueOf(t.getValue())));
        tenantAgg.add(Function.count, tenant2Count);

        stats.put("clusterName", clusterAgg);
        stats.put("tenantName", tenantAgg);
        String json = JsonUtils.prettyToJson(stats);

        log.info("stats: {}", json);
    }

}
