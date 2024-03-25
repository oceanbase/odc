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
package com.oceanbase.odc.service.session;

import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.DefaultSessionEventListener;

/**
 * {@link SessionLockRemoveListener}
 *
 * @author yh263208
 * @date 2024-03-22 16:59
 * @since ODC_release_4.2.4
 * @see DefaultSessionEventListener
 */
public class SessionLockRemoveListener extends DefaultSessionEventListener {

    private final Map<String, Lock> sessionId2Lock;

    public SessionLockRemoveListener(Map<String, Lock> sessionId2Lock) {
        this.sessionId2Lock = sessionId2Lock;
    }

    @Override
    public void onExpire(ConnectionSession session) {
        this.sessionId2Lock.remove(session.getId());
    }

}
