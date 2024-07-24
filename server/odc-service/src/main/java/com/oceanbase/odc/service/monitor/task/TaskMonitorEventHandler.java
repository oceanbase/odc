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
package com.oceanbase.odc.service.monitor.task;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.MonitorEventHandler;
import com.oceanbase.odc.service.monitor.task.MeterHandler.TaskLifecycleHandler;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class TaskMonitorEventHandler implements MonitorEventHandler<TaskMonitorEvent> {

    @Autowired
    private List<TaskLifecycleHandler> taskLifecycleHandlers;

    @Override
    public void handle(TaskMonitorEvent context) {
        for (TaskLifecycleHandler taskLifecycleHandler : taskLifecycleHandlers) {
            taskLifecycleHandler.handler(context);
        }
    }

}
