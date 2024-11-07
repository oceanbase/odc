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

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.core.datasource.event.GetConnectionFailedEvent;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetConnectionFailedEventListener extends AbstractEventListener<GetConnectionFailedEvent> {

    private final MeterManager meterManager;

    public GetConnectionFailedEventListener(MeterManager meterManager) {
        this.meterManager = meterManager;
    }

    @Override
    public void onEvent(GetConnectionFailedEvent event) {
        meterManager.incrementCounter(MeterKey.ofMeter(DATASOURCE_GET_CONNECTION_FAILED_COUNT));
    }

}
