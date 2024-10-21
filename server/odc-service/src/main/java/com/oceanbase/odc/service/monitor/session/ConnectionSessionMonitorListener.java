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
package com.oceanbase.odc.service.monitor.session;

import static com.oceanbase.odc.service.monitor.DefaultMeterName.CONNECT_SESSION_DURATION_TIME;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.CONNECT_SESSION_EXPIRED_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.CONNECT_SESSION_EXPIRED_FAILED_COUNT;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionEventListener;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.monitor.DefaultMeterName;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterManager;

public class ConnectionSessionMonitorListener implements ConnectionSessionEventListener {

    private final MeterManager meterManager;

    public ConnectionSessionMonitorListener() {
        this.meterManager = SpringContextUtil.getBean(MeterManager.class);
    }

    @Override
    public void onCreateSucceed(ConnectionSession session) {
        meterManager.incrementCounter(MeterKey.ofMeter(DefaultMeterName.CONNECT_SESSION_TOTAL));
        meterManager.startTimerSample(session.getId(), MeterKey.ofMeter(CONNECT_SESSION_DURATION_TIME));
    }

    @Override
    public void onCreateFailed(ConnectionSession session, Throwable e) {
        meterManager.incrementCounter(MeterKey.ofMeter(DefaultMeterName.CONNECT_SESSION_TOTAL));
        meterManager.incrementCounter(MeterKey.ofMeter(DefaultMeterName.CONNECT_SESSION_CREATED_FAILED_COUNT));
    }

    @Override
    public void onDeleteSucceed(ConnectionSession session) {
        meterManager.incrementCounter(MeterKey.ofMeter(DefaultMeterName.CONNECT_SESSION_DELETE_SUCCESS_COUNT));
        meterManager.recordTimerSample(session.getId(), MeterKey.ofMeter(CONNECT_SESSION_DURATION_TIME));

    }

    @Override
    public void onDeleteFailed(String id, Throwable e) {
        meterManager.incrementCounter(MeterKey.ofMeter(DefaultMeterName.CONNECT_SESSION_DELETE_FAILED_COUNT));
        meterManager.recordTimerSample(id, MeterKey.ofMeter(CONNECT_SESSION_DURATION_TIME));
    }

    @Override
    public void onGetSucceed(ConnectionSession session) {
        meterManager.incrementCounter(MeterKey.ofMeter(DefaultMeterName.CONNECT_SESSION_GET_COUNT));
    }

    @Override
    public void onGetFailed(String id, Throwable e) {
        meterManager.incrementCounter(MeterKey.ofMeter(DefaultMeterName.CONNECT_SESSION_GET_FAILED_COUNT));

    }

    @Override
    public void onExpire(ConnectionSession session) {
        meterManager.recordTimerSample(session.getId(), MeterKey.ofMeter(CONNECT_SESSION_DURATION_TIME));
    }

    @Override
    public void onExpireSucceed(ConnectionSession session) {
        meterManager.incrementCounter(MeterKey.ofMeter(CONNECT_SESSION_EXPIRED_COUNT));

    }

    @Override
    public void onExpireFailed(ConnectionSession session, Throwable e) {
        meterManager.incrementCounter(MeterKey.ofMeter(CONNECT_SESSION_EXPIRED_FAILED_COUNT));

    }
}
