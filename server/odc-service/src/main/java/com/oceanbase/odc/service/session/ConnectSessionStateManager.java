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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.connection.model.CreateSessionReq;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionIdGenerator;
import com.oceanbase.odc.service.state.model.RouteInfo;
import com.oceanbase.odc.service.state.model.StateManager;

@Component
public class ConnectSessionStateManager implements StateManager {

    @Autowired
    private HostProperties hostProperties;

    @Override
    public RouteInfo getRouteInfo(Object stateId) {
        Preconditions.checkArgument(stateId instanceof String, "stateId");
        CreateSessionReq req =
                new DefaultConnectSessionIdGenerator().getKeyFromId(SidUtils.getSessionId((String) stateId));
        return new RouteInfo(req.getFrom(), hostProperties.getRequestPort());
    }
}
