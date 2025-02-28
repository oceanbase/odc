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

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;

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
    private Integer otherCount;

    public static AlterScheduleTaskStat init(@NonNull ScheduleType type) {
        return empty().setType(type);
    }

    public static AlterScheduleTaskStat init(@NonNull TaskType type) {
        if (type == TaskType.ASYNC) {
            return empty().setType(ScheduleType.SQL_PLAN);
        } else if (type == TaskType.PARTITION_PLAN) {
            return empty().setType(ScheduleType.PARTITION_PLAN);
        }
        throw new UnsupportedException("Unsupported task type: " + type);
    }

    public void merge(AlterScheduleTaskStat stat) {
        if (stat == null || stat.getType() != type) {
            return;
        }
        this.successExecutionCount += ObjectUtil.defaultIfNull(stat.getSuccessExecutionCount(), 0);
        this.failedExecutionCount += ObjectUtil.defaultIfNull(stat.getFailedExecutionCount(), 0);
        this.waitingExecutionCount += ObjectUtil.defaultIfNull(stat.getWaitingExecutionCount(), 0);
        this.executingCount += ObjectUtil.defaultIfNull(stat.getExecutingCount(), 0);
        this.otherCount += ObjectUtil.defaultIfNull(stat.getOtherCount(), 0);
    }

    public static AlterScheduleTaskStat empty() {
        return AlterScheduleTaskStat.builder()
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
                this.addWaitingExecutionCount();
                break;
            case RUNNING:
                this.addExecutingCount();
                break;
            case ABNORMAL:
            case FAILED:
                this.addFailedExecutionCount();
                break;
            case DONE:
                this.addSuccessExecutionCount();
                break;
            default:
                this.addOtherCount();
                break;
        }
    }

    public void count(@NonNull FlowStatus flowInstanceStatus) {
        switch (flowInstanceStatus) {
            case PRE_CHECK_FAILED:
            case EXECUTION_ABNORMAL:
            case EXECUTION_FAILED:
            case ROLLBACK_FAILED:
                this.addFailedExecutionCount();
                break;
            case COMPLETED:
            case EXECUTION_SUCCEEDED:
                this.addSuccessExecutionCount();
                break;
            case CREATED:
            case WAIT_FOR_EXECUTION:
            case WAIT_FOR_CONFIRM:
                this.addWaitingExecutionCount();
                break;
            case EXECUTING:
                this.addExecutingCount();
                break;
            default:
                this.addOtherCount();
                break;
        }
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

    public void addOtherCount() {
        this.otherCount = ObjectUtil.defaultIfNull(this.getOtherCount(), 0) + 1;
    }
}
