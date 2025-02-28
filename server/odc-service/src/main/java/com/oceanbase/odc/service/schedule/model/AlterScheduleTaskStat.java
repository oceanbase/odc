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

import cn.hutool.core.util.ObjectUtil;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * @Author: ysj
 * @Date: 2025/2/24 16:12
 * @Since: 4.3.4
 * @Description: Is used to collect statistics on Alter Schedule Sub tasks
 */
@Data
@Builder
@Accessors(chain = true)
public class AlterScheduleTaskStat {
    private ScheduleType type;
    private Integer successExecutionCount;
    private Integer failedExecutionCount;
    private Integer waitingExecutionCount;
    private Integer executingCount;

    public static AlterScheduleTaskStat init(@NonNull ScheduleType type) {
        return empty().setType(type);
    }

    public void merge(AlterScheduleTaskStat stat) {
        if (stat == null || stat.getType() != type) {
            return;
        }
        this.successExecutionCount += ObjectUtil.defaultIfNull(stat.getSuccessExecutionCount(), 0);
        this.failedExecutionCount += ObjectUtil.defaultIfNull(stat.getFailedExecutionCount(), 0);
        this.waitingExecutionCount += ObjectUtil.defaultIfNull(stat.getWaitingExecutionCount(), 0);
        this.executingCount += ObjectUtil.defaultIfNull(stat.getExecutingCount(), 0);
    }

    public static AlterScheduleTaskStat empty() {
        return AlterScheduleTaskStat.builder()
                .successExecutionCount(0)
                .failedExecutionCount(0)
                .waitingExecutionCount(0)
                .executingCount(0)
                .build();
    }

    public void addSuccessExecutionCount() {
        this.successExecutionCount = ObjectUtil.defaultIfNull(this.getSuccessExecutionCount(), 0) + 1;
    }

    public void addFailedExecutionCount() {
        this.failedExecutionCount = ObjectUtil.defaultIfNull(this.getFailedExecutionCount(), 0) + 1;
    }

    public void addWaitingExecutionCount() {
        this.waitingExecutionCount = ObjectUtil.defaultIfNull(this.getWaitingExecutionCount(), 0) + 1;
    }

    public void addExecutingCount() {
        this.executingCount = ObjectUtil.defaultIfNull(this.getExecutingCount(), 0) + 1;
    }
}
