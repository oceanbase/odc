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

import static com.oceanbase.odc.service.monitor.MeterName.DATASOURCE_GET_CONNECTION_FAILED_COUNT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.MeterHolder;
import com.oceanbase.odc.service.monitor.MeterHolder.MeterKey;
import com.oceanbase.odc.service.monitor.MonitorEventHandler;
import com.oceanbase.odc.service.monitor.datasource.DatasourceMonitorEventContext.Action;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
public class DataSourseMonitorEventHandler implements MonitorEventHandler<DatasourceMonitorEventContext> {

    @Autowired
    private MeterHolder meterHolder;

    @Override
    public void handle(DatasourceMonitorEventContext source) {
        if (source.getAction().equals(Action.GET_CONNECTION_FAILED)) {
            meterHolder.getCounterHolder().increment(MeterKey.ofMeter(DATASOURCE_GET_CONNECTION_FAILED_COUNT));
        }
    }
}
