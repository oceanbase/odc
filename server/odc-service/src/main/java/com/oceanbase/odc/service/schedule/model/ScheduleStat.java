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
package com.oceanbase.odc.service.schedule.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import cn.hutool.core.util.ObjectUtil;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * @Author: ysj
 * @Date: 2025/2/24 16:12
 * @Since: 4.3.4
 * @Description: Is used to collect statistics on Alter Schedule Task and its subTasks, Ref
 *               {@link ScheduleType}
 */
@Data
@Builder
@Accessors(chain = true)
public class ScheduleStat {
    private ScheduleType type;
    private Integer successEnabledCount;
    // The number of all states, including those that have been deleted
    private Integer totalCount;
    private Set<ScheduleTaskStat> taskStats;

    public static ScheduleStat init(@NonNull ScheduleType type) {
        return ScheduleStat.builder()
                .type(type)
                .successEnabledCount(0)
                .totalCount(0)
                .taskStats(new HashSet<>())
                .build();
    }

    public void merge(@NonNull Set<ScheduleTaskStat> subTaskStats) {
        if (CollectionUtils.isEmpty(subTaskStats)) {
            return;
        }
        Map<ScheduleTaskType, ScheduleTaskStat> taskType2Stat = subTaskStats.stream().collect(
                Collectors.toMap(ScheduleTaskStat::getType, Function.identity()));
        this.setTaskStats(ObjectUtil.defaultIfNull(this.getTaskStats(), new HashSet<>()));
        for (ScheduleTaskStat taskStat : taskStats) {
            taskStat.merge(taskType2Stat.remove(taskStat.getType()));
        }
        taskType2Stat.forEach((type, stat) -> {
            this.taskStats.add(stat);
        });
    }
}
