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
package com.oceanbase.odc.service.monitor.session.meterhandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.service.monitor.MeterHolder.MeterKey;
import com.oceanbase.odc.service.monitor.MeterName;
import com.oceanbase.odc.service.monitor.session.SessionMonitorContext;
import com.oceanbase.odc.service.monitor.session.SessionMonitorContext.SessionAction;

@Component
@ConditionalOnProperty(value = "odc.system.monitor.actuator.enabled", havingValue = "true")
class GetHandler extends AbstractHandler {
    @Override
    void doHandle(SessionMonitorContext source) {
        if (source.getAction().equals(SessionAction.GET)) {
            meterHolder.getCounterHolder().increment(MeterKey.ofMeter(MeterName.CONNECT_SESSION_GET_COUNT));
        }
        if (source.getAction().equals(SessionAction.GET_FAILED)) {
            meterHolder.getCounterHolder().increment(MeterKey.ofMeter(MeterName.CONNECT_SESSION_GET_FAILED_COUNT));
        }
    }
}
