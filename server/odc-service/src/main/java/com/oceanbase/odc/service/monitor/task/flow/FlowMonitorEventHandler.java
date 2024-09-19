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
package com.oceanbase.odc.service.monitor.task.flow;

import static com.oceanbase.odc.service.monitor.MeterName.FLOW_TASK_DURATION;
import static com.oceanbase.odc.service.monitor.MeterName.FLOW_TASK_FAILED_COUNT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.MeterHolder;
import com.oceanbase.odc.service.monitor.MeterHolder.MeterKey;
import com.oceanbase.odc.service.monitor.MeterName;
import com.oceanbase.odc.service.monitor.MonitorEventHandler;
import com.oceanbase.odc.service.monitor.task.flow.FlowMonitorEvent.Action;

import io.micrometer.core.instrument.Tag;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class FlowMonitorEventHandler implements MonitorEventHandler<FlowMonitorEvent> {

    @Autowired
    private MeterHolder meterHolder;

    @Override
    public void handle(FlowMonitorEvent source) {
        Action action = source.getAction();
        if (action.equals(Action.FLOW_CREATED)) {
            MeterKey meterKey = MeterKey.ofMeter(MeterName.FLOW_CREATED_COUNT,
                    Tag.of("organizationId", source.getOrganizationId().toString()));
            meterHolder.getCounterHolder()
                    .increment(meterKey);
            return;
        }

        MeterKey durationKey = getTaskMeterKey(FLOW_TASK_DURATION, source.getTaskId(), source);
        if (action.equals(Action.FLOW_TASK_STARTED)) {
            meterHolder.getCounterHolder()
                    .increment(getTaskMeterKey(MeterName.FLOW_TASK_START_COUNT, source.getTaskId(), source));
            meterHolder.getTimerHolder().start(durationKey);
        }

        if (action.equals(Action.FLOW_TASK_END)) {
            meterHolder.getCounterHolder()
                    .increment(getTaskMeterKey(MeterName.FLOW_TASK_SUCCESS_COUNT, source.getTaskId(), source));
            meterHolder.getTimerHolder().record(durationKey);
        }

        if (action.equals(Action.FLOW_TASK_FAILED)) {
            meterHolder.getCounterHolder().increment(getTaskMeterKey(FLOW_TASK_FAILED_COUNT, null, source));
            meterHolder.getTimerHolder().record(durationKey);

        }
    }

    public MeterKey getTaskMeterKey(MeterName meterName, String uniqueKey, FlowMonitorEvent source) {
        return MeterKey.ofMeter(meterName, uniqueKey, Tag.of("taskType", source.getTaskType()),
                Tag.of("organizationId", source.getOrganizationId().toString()));

    }

}
