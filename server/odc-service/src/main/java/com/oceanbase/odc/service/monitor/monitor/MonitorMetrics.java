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
package com.oceanbase.odc.service.monitor.monitor;

import static com.oceanbase.odc.service.monitor.MeterName.METER_COUNTER_HOLDER_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.METER_GAUGE_HOLDER_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.METER_TIMER_HOLDER_COUNT;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.MeterHolder;
import com.oceanbase.odc.service.monitor.MonitorAutoConfiguration.BusinessMeterRegistry;

import io.micrometer.core.instrument.Gauge;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class MonitorMetrics implements InitializingBean {

    @Autowired
    MeterHolder meterHolder;
    @Autowired
    private BusinessMeterRegistry meterRegistry;

    @Override
    public void afterPropertiesSet() throws Exception {
        Gauge.builder(METER_COUNTER_HOLDER_COUNT.getMeterName(),
                () -> meterHolder.getCounterHolder().getSize())
                .description(METER_COUNTER_HOLDER_COUNT.getDescription())
                .register(meterRegistry);

        Gauge.builder(METER_GAUGE_HOLDER_COUNT.getMeterName(),
                () -> meterHolder.getGaugeHolder().getSize())
                .description(METER_GAUGE_HOLDER_COUNT.getDescription())
                .register(meterRegistry);

        Gauge.builder(METER_TIMER_HOLDER_COUNT.getMeterName(),
                () -> meterHolder.getTimerHolder().getSize())
                .description(METER_TIMER_HOLDER_COUNT.getDescription())
                .register(meterRegistry);
    }
}
