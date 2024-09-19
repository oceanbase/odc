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

import static com.oceanbase.odc.service.monitor.MeterName.SCHEDULE_ENABLED_COUNT;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.service.monitor.MonitorAutoConfiguration.BusinessMeterRegistry;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

import io.micrometer.core.instrument.Gauge;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class TaskMetrics implements InitializingBean {
    @Autowired
    BusinessMeterRegistry meterRegistry;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        Gauge.builder(SCHEDULE_ENABLED_COUNT.getMeterName(),
                () -> scheduleRepository.countByStatus(ScheduleStatus.ENABLED))
                .description(SCHEDULE_ENABLED_COUNT.getDescription())
                .register(meterRegistry);
    }
}
