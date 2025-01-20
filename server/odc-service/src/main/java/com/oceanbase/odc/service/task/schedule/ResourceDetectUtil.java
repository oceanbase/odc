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

import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-03-29
 * @since 4.2.4
 */
@Slf4j
public class ResourceDetectUtil {

    public static boolean isResourceAvailable(TaskFrameworkProperties taskFrameworkProperties) {
        boolean result = true;
        if (taskFrameworkProperties.getRunMode().isProcess() && SystemUtils.isOnLinux()) {
            long systemFreeMemory = SystemUtils.getSystemFreePhysicalMemory().convert(BinarySizeUnit.MB).getSizeDigit();
            long processMemoryMinSize = taskFrameworkProperties.getJobProcessMinMemorySizeInMB();
            long memoryReserveSize = taskFrameworkProperties.getSystemReserveMinFreeMemorySizeInMB();

            if (systemFreeMemory < (memoryReserveSize + processMemoryMinSize)) {
                if (log.isDebugEnabled()) {
                    log.debug("Free memory lack, systemFreeMemory={}, processMemoryMinSize={}, "
                            + "memoryReserveSize={}", systemFreeMemory, processMemoryMinSize, memoryReserveSize);
                }
                result = false;
            }
        }
        return result;
    }
}
