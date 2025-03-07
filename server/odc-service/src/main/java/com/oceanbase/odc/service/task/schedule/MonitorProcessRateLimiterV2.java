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
package com.oceanbase.odc.service.task.schedule;

import java.util.function.Supplier;

import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-02-05
 * @since 4.2.4
 */
@Slf4j
public class MonitorProcessRateLimiterV2 extends MonitorProcessRateLimiter {
    private final int maxAllowRunningJobs;

    public MonitorProcessRateLimiterV2(Supplier<TaskFrameworkProperties> taskFrameworkProperties,
            TaskFrameworkService taskFrameworkService, int maxAllowRunningJobs) {
        super(taskFrameworkProperties, taskFrameworkService);
        this.maxAllowRunningJobs = Math.max(0, maxAllowRunningJobs);
    }

    @Override
    protected boolean doTryAcquire() {
        long count = taskFrameworkService.countRunningJobs(TaskRunMode.PROCESS);
        if (count >= maxAllowRunningJobs) {
            if (log.isDebugEnabled()) {
                log.debug("Amount of executor running jobs exceed limit, wait next schedule,"
                        + " limit={}, runningJobs={}.", runningJobCountLimit, count);
            }
            return false;
        } else {
            return true;
        }
    }
}
