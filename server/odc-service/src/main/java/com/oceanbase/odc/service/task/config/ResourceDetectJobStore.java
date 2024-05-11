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
package com.oceanbase.odc.service.task.config;

import java.util.LinkedList;
import java.util.List;

import org.quartz.JobPersistenceException;
import org.quartz.TriggerKey;
import org.quartz.spi.OperableTrigger;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;

import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.schedule.ResourceDetectUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-05-10
 * @since 4.2.4
 */
@Slf4j
public class ResourceDetectJobStore extends LocalDataSourceJobStore {

    @Override
    public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow)
            throws JobPersistenceException {

        List<OperableTrigger> triggers = super.acquireNextTriggers(noLaterThan, maxCount, timeWindow);
        if (triggers == null || triggers.isEmpty() ||
                !SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class).isEnabled()) {
            return triggers;
        }
        List<OperableTrigger> filteredTriggers = new LinkedList<>();
        for (OperableTrigger trigger : triggers) {
            if (shouldAcquire(trigger)) {
                filteredTriggers.add(trigger);
            } else {
                releaseAcquiredTrigger(trigger);
            }
        }
        return filteredTriggers;
    }

    private boolean shouldAcquire(OperableTrigger trigger) {
        TaskFrameworkProperties taskFrameworkProperties = TaskFrameworkPropertiesSupplier.getSupplier().get();
        // If resource is not available return false and give a chance to other node acquire trigger
        if (!ResourceDetectUtil.isResourceAvailable(taskFrameworkProperties)) {
            if (TriggerKey.triggerKey("startPreparingJob", JobConstants.ODC_JOB_MONITORING)
                    .equals(trigger.getKey())) {
                log.debug("StartPreparingJob trigger is discarded because no resource.");
                return false;
            }
        }
        return true;
    }

}
