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

package com.oceanbase.odc.service.task.config;

import java.util.List;

import org.quartz.JobPersistenceException;
import org.quartz.spi.OperableTrigger;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;

import com.oceanbase.odc.service.task.schedule.ResourceDetectUtil;

/**
 * @author yaobin
 * @date 2024-05-10
 * @since 4.2.4
 */
public class JobStoreSupportDelegate extends LocalDataSourceJobStore {

    private final TaskFrameworkEnabledProperties taskFrameworkEnabledProperties;

    public JobStoreSupportDelegate(TaskFrameworkEnabledProperties taskFrameworkEnabledProperties) {
        this.taskFrameworkEnabledProperties = taskFrameworkEnabledProperties;
    }

    @Override
    public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow)
            throws JobPersistenceException {
        TaskFrameworkProperties taskFrameworkProperties = TaskFrameworkPropertiesSupplier.getSupplier().get();
        // if resource is not available return null and give a chance to other node acquire trigger
        if (taskFrameworkEnabledProperties.isEnabled()
                && !ResourceDetectUtil.isResourceAvailable(taskFrameworkProperties)) {
            return null;
        }

        return super.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
    }
}
