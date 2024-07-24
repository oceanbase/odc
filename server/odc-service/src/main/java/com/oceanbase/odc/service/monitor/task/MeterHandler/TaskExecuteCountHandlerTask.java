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

import static com.oceanbase.odc.service.monitor.task.TaskMeters.TASK_EXECUTE_COUNT;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.task.TaskMetrics.TaskMeterKey;
import com.oceanbase.odc.service.monitor.task.TaskMonitorEvent;
import com.oceanbase.odc.service.monitor.task.TaskMonitorEvent.TaskLifecycle;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class TaskExecuteCountHandlerTask extends AbstractTaskExecuteCountHandlerTask {

    @Override
    public void doHandler(TaskMonitorEvent event) {
        String taskType = event.getContext().getTaskType();
        if (TaskLifecycle.TASK_STARTED.equals(event.getTaskLifecycle())) {
            taskMetrics.getTaskCounterHolder().increment(TaskMeterKey.ofTaskType(TASK_EXECUTE_COUNT, taskType));
        }
    }

}
