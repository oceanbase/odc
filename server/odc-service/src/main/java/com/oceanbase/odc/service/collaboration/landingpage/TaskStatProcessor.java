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
package com.oceanbase.odc.service.collaboration.landingpage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;

import com.oceanbase.odc.metadb.flow.FlowInstanceRepository.FlowInstanceTaskTypeAndStatusCount;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository.ScheduleTypeCount;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository.ScheduleTaskTypeAndStatusCount;
import com.oceanbase.odc.service.collaboration.landingpage.model.FlowStatusCategory;
import com.oceanbase.odc.service.collaboration.landingpage.model.ScheduleStatusCategory;
import com.oceanbase.odc.service.collaboration.landingpage.model.ScheduleTaskStatusCategory;
import com.oceanbase.odc.service.collaboration.landingpage.model.TaskTodoCategory;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.model.Stats.Aggregation;
import com.oceanbase.odc.service.common.model.Stats.Function;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import lombok.NonNull;

/**
 * @Author: ysj
 * @Date: 2025/8/12 15:41
 * @Since: 4.4.1
 * @Description:
 */
public final class TaskStatProcessor {

    public static Stats toFlowInstanceStat(
            @NonNull List<FlowInstanceTaskTypeAndStatusCount> flowInstanceTaskTypeAndStatusCounts) {
        final Stats stats = Stats.empty();
        flowInstanceTaskTypeAndStatusCounts.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getTaskType().name(),
                        Collectors.groupingBy(
                                taskType -> FlowStatusCategory.getCategory(taskType.getStatus()).name(),
                                Collectors.summingDouble(FlowInstanceTaskTypeAndStatusCount::getCount))))
                .forEach((taskType, statusCount) -> {
                    Aggregation aggregation = stats.computeIfAbsent(taskType, k -> new Aggregation());
                    aggregation.add(Function.count, statusCount);
                });
        return stats;
    }

    public static Stats toScheduleStat(@NonNull List<ScheduleTypeCount> scheduleTypeCounts,
            @NonNull List<ScheduleTaskTypeAndStatusCount> scheduleTaskStatusCounts) {
        final Stats stats = Stats.empty();

        Map<ScheduleType, Long> scheduleType2EnabledCount = scheduleTypeCounts.stream().collect(
                Collectors.toMap(ScheduleTypeCount::getScheduleType, ScheduleTypeCount::getCount, (o, n) -> o));

        scheduleTaskStatusCounts.forEach(taskStatusCount -> {
            ScheduleType scheduleType = ScheduleTaskType.from(taskStatusCount.getScheduleTaskType());
            Aggregation countAgg = stats.computeIfAbsent(scheduleType.name(), k -> new Aggregation());
            Map<String, Double> countMap = Optional.ofNullable(countAgg.getCount()).orElse(new HashMap<>());
            countMap.compute(ScheduleTaskStatusCategory.getCategory(taskStatusCount.getScheduleStatus()).name(),
                    (k, v) -> v == null ? taskStatusCount.getCount() : v + taskStatusCount.getCount());
            countAgg.setCount(countMap);
        });

        scheduleType2EnabledCount.forEach((scheduleType, count) -> {
            Aggregation countAgg = stats.computeIfAbsent(scheduleType.name(), k -> new Aggregation());
            Map<String, Double> countMap = Optional.ofNullable(countAgg.getCount()).orElse(new HashMap<>());
            countMap.compute(ScheduleStatusCategory.ENABLED.name(),
                    (k, v) -> (v == null ? 0D : v) + scheduleType2EnabledCount.getOrDefault(scheduleType, 0L));
            countAgg.setCount(countMap);
        });

        return stats;
    }

    public static Stats toFlowAndScheduleTodoStat(Map<TaskTodoCategory, Long> category2Count) {
        final Stats stats = Stats.empty();
        if (MapUtils.isEmpty(category2Count)) {
            return stats;
        }
        category2Count.forEach((category, count) -> {
            Aggregation countAgg = stats.computeIfAbsent(category.getType(), k -> new Aggregation());
            Map<String, Double> categoryCount = Optional.ofNullable(countAgg.getCount()).orElse(new HashMap<>());
            categoryCount.put(category.name(), Double.valueOf(count));
            countAgg.setCount(categoryCount);
        });
        return stats;
    }
}
