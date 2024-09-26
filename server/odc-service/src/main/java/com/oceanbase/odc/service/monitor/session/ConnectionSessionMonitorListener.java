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

import static com.oceanbase.odc.service.monitor.MeterName.CONNECT_SESSION_DURATION_TIME;
import static com.oceanbase.odc.service.monitor.MeterName.CONNECT_SESSION_EXPIRED_COUNT;
import static com.oceanbase.odc.service.monitor.MeterName.CONNECT_SESSION_EXPIRED_FAILED_COUNT;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionEventListener;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterName;
import com.oceanbase.odc.service.monitor.MetricManager;

public class ConnectionSessionMonitorListener implements ConnectionSessionEventListener {

    private final MetricManager metricManager;

    public ConnectionSessionMonitorListener() {
        this.metricManager = SpringContextUtil.getBean(MetricManager.class);
    }

    @Override
    public void onCreateSucceed(ConnectionSession session) {
        metricManager.incrementCounter(MeterKey.ofMeter(MeterName.CONNECT_SESSION_TOTAL));
        metricManager.startTimerSample(session.getId(), true, MeterKey.ofMeter(CONNECT_SESSION_DURATION_TIME));
    }

    @Override
    public void onCreateFailed(ConnectionSession session, Throwable e) {
        metricManager.incrementCounter(MeterKey.ofMeter(MeterName.CONNECT_SESSION_TOTAL));
        metricManager.incrementCounter(MeterKey.ofMeter(MeterName.CONNECT_SESSION_CREATED_FAILED_COUNT));
    }

    @Override
    public void onDeleteSucceed(ConnectionSession session) {
        metricManager.incrementCounter(MeterKey.ofMeter(MeterName.CONNECT_SESSION_DELETE_SUCCESS_COUNT));
        metricManager.recordTimerSample(session.getId(), MeterKey.ofMeter(CONNECT_SESSION_DURATION_TIME));

    }

    @Override
    public void onDeleteFailed(String id, Throwable e) {
        metricManager.incrementCounter(MeterKey.ofMeter(MeterName.CONNECT_SESSION_DELETE_FAILED_COUNT));
        metricManager.recordTimerSample(id, MeterKey.ofMeter(CONNECT_SESSION_DURATION_TIME));
    }

    @Override
    public void onGetSucceed(ConnectionSession session) {
        metricManager.incrementCounter(MeterKey.ofMeter(MeterName.CONNECT_SESSION_GET_COUNT));
    }

    @Override
    public void onGetFailed(String id, Throwable e) {
        metricManager.incrementCounter(MeterKey.ofMeter(MeterName.CONNECT_SESSION_GET_FAILED_COUNT));

    }

    @Override
    public void onExpire(ConnectionSession session) {
        metricManager.recordTimerSample(session.getId(), MeterKey.ofMeter(CONNECT_SESSION_DURATION_TIME));
    }

    @Override
    public void onExpireSucceed(ConnectionSession session) {
        metricManager.incrementCounter(MeterKey.ofMeter(CONNECT_SESSION_EXPIRED_COUNT));

    }

    @Override
    public void onExpireFailed(ConnectionSession session, Throwable e) {
        metricManager.incrementCounter(MeterKey.ofMeter(CONNECT_SESSION_EXPIRED_FAILED_COUNT));

    }
}
