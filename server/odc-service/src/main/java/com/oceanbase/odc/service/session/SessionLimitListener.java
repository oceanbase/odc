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

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.session.DefaultSessionEventListener;

import lombok.NonNull;

public class SessionLimitListener extends DefaultSessionEventListener {

    private final SessionLimitService limitService;

    public SessionLimitListener(@NonNull SessionLimitService limitService) {
        this.limitService = limitService;
    }

    @Override
    public void onExpire(ConnectionSession session) {
        Long userId = ConnectionSessionUtil.getUserId(session);
        if (userId == null) {
            return;
        }
        this.limitService.decrementSessionCount(userId + "");
    }

}
