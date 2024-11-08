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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.core.datasource.event.GetConnectionFailedEvent;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterManager;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
class ConnectFailedListenerDelegate implements InitializingBean {

    private final EventStore eventStore = GetConnectionFailedEventListener.eventStore;
    private final ScheduledExecutorService scheduledExecutorService;
    @Autowired
    private MeterManager meterManager;

    public ConnectFailedListenerDelegate() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    @PreDestroy
    public void preDestroy() {
        ExecutorUtils.gracefulShutdown(scheduledExecutorService, "ConnectFailedListenerDelegate", 5);
    }

    @Override
    public void afterPropertiesSet() {
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(
                this::doIncrease, 1, 1, TimeUnit.SECONDS);
    }


    public void doIncrease() {
        List<GetConnectionFailedEvent> events = eventStore.eventDrainTo(1000);
        meterManager.incrementCounter(MeterKey.ofMeter(DATASOURCE_GET_CONNECTION_FAILED_COUNT), events.size());
    }
}
