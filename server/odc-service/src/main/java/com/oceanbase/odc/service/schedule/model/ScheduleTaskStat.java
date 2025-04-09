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

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;

import cn.hutool.core.util.ObjectUtil;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ScheduleTaskStat {
    @EqualsAndHashCode.Include
    private ScheduleTaskType type;
    private Integer successExecutionCount;
    private Integer failedExecutionCount;
    private Integer waitingExecutionCount;
    private Integer executingCount;
    private Integer otherCount;

    public static ScheduleTaskStat init(@NonNull ScheduleTaskType type) {
        return empty().setType(type);
    }

    public static ScheduleTaskStat init(@NonNull TaskType type) {
        if (type == TaskType.ASYNC) {
            return empty().setType(ScheduleTaskType.SQL_PLAN);
        } else if (type == TaskType.PARTITION_PLAN) {
            return empty().setType(ScheduleTaskType.PARTITION_PLAN);
        }
        throw new UnsupportedException("Unsupported sub task type: " + type);
    }

    public void merge(ScheduleTaskStat stat) {
        if (stat == null || stat.getType() != type) {
            return;
        }
        this.successExecutionCount = safeAdd(this.successExecutionCount, stat.getSuccessExecutionCount());
        this.failedExecutionCount = safeAdd(this.failedExecutionCount, stat.getFailedExecutionCount());
        this.waitingExecutionCount = safeAdd(this.waitingExecutionCount, stat.getWaitingExecutionCount());
        this.executingCount = safeAdd(this.executingCount, stat.getExecutingCount());
        this.otherCount = safeAdd(this.otherCount, stat.getOtherCount());
    }

    public void merge(List<ScheduleTaskStat> stats) {
        if (CollectionUtils.isEmpty(stats)) {
            return;
        }
        stats.forEach(this::merge);
    }

    public static ScheduleTaskStat empty() {
        return ScheduleTaskStat.builder()
                .successExecutionCount(0)
                .failedExecutionCount(0)
                .waitingExecutionCount(0)
                .executingCount(0)
                .otherCount(0)
                .build();
    }

    public void count(@NonNull TaskStatus scheduleTaskStatus) {
        switch (scheduleTaskStatus) {
            case PREPARING:
                this.increaseWaitingExecutionCount();
                break;
            case RUNNING:
                this.increaseExecutingCount();
                break;
            case ABNORMAL:
            case FAILED:
                this.increaseFailedExecutionCount();
                break;
            case DONE:
                this.increaseSuccessExecutionCount();
                break;
            default:
                this.increaseOtherCount();
                break;
        }
    }

    public void count(@NonNull FlowStatus flowInstanceStatus) {
        switch (flowInstanceStatus) {
            case REJECTED:
            case APPROVAL_EXPIRED:
            case WAIT_FOR_EXECUTION_EXPIRED:
            case EXECUTION_ABNORMAL:
            case EXECUTION_FAILED:
            case EXECUTION_EXPIRED:
            case PRE_CHECK_FAILED:
                this.increaseFailedExecutionCount();
                break;
            case COMPLETED:
            case EXECUTION_SUCCEEDED:
                this.increaseSuccessExecutionCount();
                break;
            case CREATED:
            case APPROVING:
            case WAIT_FOR_EXECUTION:
            case WAIT_FOR_CONFIRM:
                this.increaseWaitingExecutionCount();
                break;
            case EXECUTING:
                this.increaseExecutingCount();
                break;
            default:
                this.increaseOtherCount();
                break;
        }
    }

    public void increaseSuccessExecutionCount() {
        this.successExecutionCount = safeAdd(this.successExecutionCount, 1);
    }

    public void increaseFailedExecutionCount() {
        this.failedExecutionCount = safeAdd(this.failedExecutionCount, 1);
    }

    public void increaseWaitingExecutionCount() {
        this.waitingExecutionCount = safeAdd(this.waitingExecutionCount, 1);
    }

    public void increaseExecutingCount() {
        this.executingCount = safeAdd(this.executingCount, 1);
    }

    public void increaseOtherCount() {
        this.otherCount = safeAdd(this.otherCount, 1);
    }

    private static Integer safeAdd(Integer source, Integer step) {
        return ObjectUtil.defaultIfNull(source, 0) + ObjectUtil.defaultIfNull(step, 0);
    }
}
