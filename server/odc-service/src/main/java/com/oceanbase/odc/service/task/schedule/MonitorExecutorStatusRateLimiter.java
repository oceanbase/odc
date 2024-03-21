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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;

import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
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
public class MonitorExecutorStatusRateLimiter implements StartJobRateLimiter {

    private final Supplier<TaskFrameworkProperties> taskFrameworkProperties;
    private final TaskFrameworkService taskFrameworkService;
    private long runningJobCountLimit;
    private long totalPhysicMemory;

    public MonitorExecutorStatusRateLimiter(Supplier<TaskFrameworkProperties> taskFrameworkProperties,
            TaskFrameworkService taskFrameworkService) {
        this.taskFrameworkProperties = taskFrameworkProperties;
        this.taskFrameworkService = taskFrameworkService;
        if (taskFrameworkProperties.get().getRunMode().isProcess() && SystemUtils.isOnLinux()) {
            this.totalPhysicMemory =
                    SystemUtils.getSystemTotalPhysicalMemory().convert(BinarySizeUnit.MB).getSizeDigit();
            this.runningJobCountLimit = calculateRunningJobCountLimit();
        }
    }

    @Override
    public boolean tryAcquire() {
        if (taskFrameworkProperties.get().getRunMode().isProcess() && SystemUtils.isOnLinux()) {
            long count = taskFrameworkService.countRunningJobs(TaskRunMode.PROCESS);
            if (count >= runningJobCountLimit && log.isDebugEnabled()) {
                log.debug("Amount of executor running jobs exceed limit, wait next schedule, limit={}, runningJobs={}.",
                        runningJobCountLimit, count);
                return false;
            }
            long systemFreeMemory = SystemUtils.getSystemFreePhysicalMemory().convert(BinarySizeUnit.MB).getSizeDigit();
            long processMemoryMinSize = taskFrameworkProperties.get().getJobProcessMinMemorySizeInMB();
            long memoryReserveSize = taskFrameworkProperties.get().getSystemReserveMinFreeMemorySizeInMB();

            if (systemFreeMemory < (memoryReserveSize + processMemoryMinSize) && log.isDebugEnabled()) {
                log.debug("Free memory lack, systemFreeMemory={}, processMemoryMinSize={}, "
                        + "memoryReserveSize={}", systemFreeMemory, processMemoryMinSize, memoryReserveSize);
                return false;
            }
            return true;
        } else {
            return isExecutorWaitingToRunNotExceedThreshold();
        }
    }

    private long calculateRunningJobCountLimit() {
        long minProcessMem = taskFrameworkProperties.get().getJobProcessMinMemorySizeInMB();
        long jvmXmx = SystemUtils.getJvmXmxMemory().convert(BinarySizeUnit.MB).getSizeDigit();
        // limitCount = (totalPhysicMemory*0.8 - jvmXmx)/jobProcessMinMemorySize
        return new BigDecimal(totalPhysicMemory * 0.8).subtract(BigDecimal.valueOf(jvmXmx))
                .divide(new BigDecimal(minProcessMem), RoundingMode.FLOOR)
                .longValue();
    }

    private boolean isExecutorWaitingToRunNotExceedThreshold() {
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkProperties taskFrameworkProperties = jobConfiguration.getTaskFrameworkProperties();
        long count = jobConfiguration.getTaskFrameworkService().countRunningNeverHeartJobs(
                taskFrameworkProperties.getExecutorWaitingToRunThresholdSeconds());

        boolean continued = taskFrameworkProperties.getExecutorWaitingToRunThresholdCount() - count > 0;
        if (!continued && log.isDebugEnabled()) {
            log.debug("Amount of executors waiting to run exceed threshold, wait next schedule,"
                    + " threshold={}, waitingJobs={}.",
                    taskFrameworkProperties.getExecutorWaitingToRunThresholdCount(), count);
        }
        return continued;
    }

}
