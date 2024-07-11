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
package com.oceanbase.odc.service.state;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.state.model.RouteInfo;
import com.oceanbase.odc.service.state.model.StateManager;
import com.oceanbase.odc.service.state.model.StatefulUuidStateId;

@Component
@ConditionalOnProperty(value = {"odc.web.stateful-route.enabled"}, havingValue = "true")
public class StatefulUuidStateIdManager implements StateManager {

    @Autowired
    private HostProperties hostProperties;

    @Override
    public RouteInfo getRouteInfo(Object stateId) {
        Preconditions.checkArgument(stateId instanceof String, "stateId");
        StatefulUuidStateId statefulUuidStateId =
                JsonUtils.fromJson(new String(Base64.getDecoder().decode((String) stateId)),
                        StatefulUuidStateId.class);
        return new RouteInfo(statefulUuidStateId.getFrom(), hostProperties.getRequestPort());
    }
}
