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
package com.oceanbase.odc.service.monitor.task.MeterHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.task.TaskMetrics;
import com.oceanbase.odc.service.monitor.task.TaskMonitorEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
@Slf4j
public abstract class AbstractTaskExecuteCountHandlerTask implements TaskLifecycleHandler {

    @Autowired
    protected TaskMetrics taskMetrics;

    public void handler(TaskMonitorEvent event) {
        try {
            doHandler(event);
        } catch (Exception e) {
            log.error("doHandler error, className={}", this.getClass().getName(), e);
        }
    }

    abstract void doHandler(TaskMonitorEvent event);
}
