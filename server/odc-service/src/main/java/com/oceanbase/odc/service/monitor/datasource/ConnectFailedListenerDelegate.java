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
package com.oceanbase.odc.service.monitor.datasource;

import static com.oceanbase.odc.service.monitor.DefaultMeterName.DATASOURCE_GET_CONNECTION_FAILED_COUNT;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.datasource.event.GetConnectionFailedEvent;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterManager;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
@Slf4j
class ConnectFailedListenerDelegate {

    private final EventStore eventStore = GetConnectionFailedEventListener.eventStore;

    @Autowired
    private MeterManager meterManager;

    @Scheduled(fixedDelay = 1000)
    public void doIncrease() {
        try {
            List<GetConnectionFailedEvent> events = eventStore.eventDrainTo(1000);
            if (CollectionUtils.isNotEmpty(events)) {
                meterManager.incrementCounter(MeterKey.ofMeter(DATASOURCE_GET_CONNECTION_FAILED_COUNT), events.size());
            }
        } catch (Exception e) {
            log.warn("Failed to increase connection failed", e);
        }
    }
}
