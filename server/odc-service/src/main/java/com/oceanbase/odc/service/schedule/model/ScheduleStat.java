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
    private ScheduleTaskStat taskStat;

    public static ScheduleStat init(@NonNull ScheduleType type) {
        return ScheduleStat.builder()
                .type(type)
                .successEnabledCount(0)
                .taskStat(ScheduleTaskStat.init(type))
                .build();
    }
}
