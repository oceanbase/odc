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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterManager;
import com.oceanbase.odc.service.monitor.MeterName;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class ScheduleMetrics implements InitializingBean {
    @Autowired
    MeterManager meterManager;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        meterManager.registerGauge(MeterKey.ofMeter(MeterName.SCHEDULE_ENABLED_COUNT),
                () -> scheduleRepository.countByStatus(ScheduleStatus.ENABLED));
    }
}
