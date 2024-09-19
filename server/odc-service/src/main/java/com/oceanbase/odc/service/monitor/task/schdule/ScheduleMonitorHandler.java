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
package com.oceanbase.odc.service.monitor.task.schdule;

import static com.oceanbase.odc.service.monitor.MeterName.SCHEDULE_FAILED_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.SCHEDULE_INTERRUPTED_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.SCHEDULE_START_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.SCHEDULE_SUCCESS_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.SCHEDULE_TASK_DURATION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.MeterHolder;
import com.oceanbase.odc.service.monitor.MeterHolder.MeterKey;
import com.oceanbase.odc.service.monitor.MeterName;
import com.oceanbase.odc.service.monitor.MonitorEventHandler;
import com.oceanbase.odc.service.monitor.task.schdule.ScheduleMonitorEvent.Action;

import io.micrometer.core.instrument.Tag;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class ScheduleMonitorHandler implements MonitorEventHandler<ScheduleMonitorEvent> {

    @Autowired
    private MeterHolder meterHolder;

    @Override
    public void handle(ScheduleMonitorEvent source) {
        Action action = source.getAction();
        MeterKey durationMeterKey =
                MeterKey.ofNeedRemoveMeter(SCHEDULE_TASK_DURATION, Tag.of("taskType", source.getTaskType()),
                        Tag.of("scheduleId", source.getScheduleId()));
        if (action.equals(Action.SCHEDULE_TASK_START)) {
            meterHolder.getCounterHolder().increment(getMeterKey(SCHEDULE_START_COUNT, source));
            meterHolder.getTimerHolder().start(durationMeterKey);
        }
        if (action.equals(Action.SCHEDULE_TASK_END)) {
            meterHolder.getTimerHolder().record(durationMeterKey);
            meterHolder.getCounterHolder().increment(getMeterKey(SCHEDULE_SUCCESS_COUNT, source));
        }
        if (action.equals(Action.SCHEDULE_TASK_FAILED)) {
            meterHolder.getTimerHolder().record(durationMeterKey);
            meterHolder.getCounterHolder().increment(getMeterKey(SCHEDULE_FAILED_COUNT, source));
        }
        if (action.equals(Action.SCHEDULE_TASK_INTERRUPT)) {
            meterHolder.getTimerHolder().record(durationMeterKey);
            meterHolder.getCounterHolder().increment(getMeterKey(SCHEDULE_INTERRUPTED_COUNT, source));
        }
    }

    public MeterKey getMeterKey(MeterName meterName, ScheduleMonitorEvent source) {
        return MeterKey.ofMeter(meterName, Tag.of("taskType", source.getTaskType()));
    }
}
