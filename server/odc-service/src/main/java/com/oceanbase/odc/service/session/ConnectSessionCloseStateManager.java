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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.state.model.RouteInfo;
import com.oceanbase.odc.service.state.model.SingleNodeStateResponse;
import com.oceanbase.odc.service.state.model.StateManager;

@Component
public class ConnectSessionCloseStateManager implements StateManager {

    @Autowired
    private ConnectSessionStateManager connectSessionStateManager;

    @Override
    public RouteInfo getRouteInfo(Object stateId) {
        return null;
    }

    public boolean supportMultiRoute() {
        return true;
    }

    @Override
    public Set<RouteInfo> getAllRoutes(Object stateId) {
        Preconditions.checkArgument(stateId instanceof Set);
        Set<String> sessionIds = (Set<String>) stateId;
        return sessionIds.stream().map(connectSessionStateManager::getRouteInfo).collect(
                Collectors.toSet());
    }

    @Override
    public Object handleMultiResponse(List<SingleNodeStateResponse> otherNodeResponse, Object currentNodeResult)
            throws Throwable {
        Set<String> allSessionIds = new HashSet<>();
        if (currentNodeResult != null) {
            Set<String> currentSessionIds = ((SuccessResponse<Set<String>>) currentNodeResult).getData();
            allSessionIds.addAll(currentSessionIds);
        }
        for (SingleNodeStateResponse response : otherNodeResponse) {
            Throwable thrown = response.getThrown();
            if (thrown != null) {
                throw thrown;
            }
            Set<String> sessionIds = response.getDispatchResponse().getContentByType(
                    new TypeReference<SuccessResponse<Set<String>>>() {}).getData();
            allSessionIds.addAll(sessionIds);
        }
        return Responses.ok(allSessionIds);
    }


}
