/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.task;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

/**
 * @author yaobin
 * @date 2024-03-13
 * @since 4.2.4
 */
public class Test1 {

    public static void main(String[] args) {

        System.out.println("System memory is = " + getSystemFreeMemory());
    }

    public static long getSystemFreeMemory() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        return (osBean.getFreeSwapSpaceSize() + osBean.getFreePhysicalMemorySize()) / 1024 / 1024;
    }
}
