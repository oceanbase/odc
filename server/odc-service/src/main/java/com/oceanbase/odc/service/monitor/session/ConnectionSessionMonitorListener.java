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

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionEventListener;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.monitor.MonitorEvent;
import com.oceanbase.odc.service.monitor.session.SessionMonitorContext.SessionAction;

public class ConnectionSessionMonitorListener implements ConnectionSessionEventListener {

    @Override
    public void onCreateSucceed(ConnectionSession session) {
        publishEvent(SessionAction.CREATED_SUCCESS, session.getId());
    }

    @Override
    public void onCreateFailed(ConnectionSession session, Throwable e) {
        publishEvent(SessionAction.CREATED_FAILED, session.getId());
    }

    @Override
    public void onDeleteSucceed(ConnectionSession session) {
        publishEvent(SessionAction.DELETE_SUCCESS, session.getId());

    }

    @Override
    public void onDeleteFailed(String id, Throwable e) {
        publishEvent(SessionAction.DELETED_FAILED, id);

    }

    @Override
    public void onGetSucceed(ConnectionSession session) {
        publishEvent(SessionAction.GET, session.getId());

    }

    @Override
    public void onGetFailed(String id, Throwable e) {
        publishEvent(SessionAction.GET_FAILED, id);

    }

    @Override
    public void onExpire(ConnectionSession session) {}

    @Override
    public void onExpireSucceed(ConnectionSession session) {
        publishEvent(SessionAction.EXPIRED_SUCCESS, session.getId());
    }

    @Override
    public void onExpireFailed(ConnectionSession session, Throwable e) {
        publishEvent(SessionAction.EXPIRED_FAILED, session.getId());

    }

    public void publishEvent(SessionAction action, String sessionId) {
        SpringContextUtil.publishEvent(
                MonitorEvent.createSessionMonitor(
                        new SessionMonitorContext(action, sessionId)));
    }
}
