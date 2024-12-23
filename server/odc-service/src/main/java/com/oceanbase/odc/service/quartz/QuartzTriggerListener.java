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
package com.oceanbase.odc.service.quartz;

import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/12/24
 */
@Slf4j
@Component
public class QuartzTriggerListener extends TriggerListenerSupport {
    @Override
    public String getName() {
        return "QUARTZ_TRIGGER_LISTENER";
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        log.warn("Job is misfired, job key:" + trigger.getJobKey());
    }
}
