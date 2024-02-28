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
package com.oceanbase.odc.service.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.db.DBRecyclebinSettingsService.RecyclebinSettings;
import com.oceanbase.odc.service.session.ConnectSessionStateManager;
import com.oceanbase.odc.service.state.model.RouteInfo;
import com.oceanbase.odc.service.state.model.SingleNodeStateResponse;
import com.oceanbase.odc.service.state.model.StateManager;

@Component
public class DbRecyclebinUpdateStateManager implements StateManager {

    @Autowired
    private ConnectSessionStateManager connectSessionStateManager;

    @Override
    public RouteInfo getRouteInfo(Object stateId) {
        return null;
    }

    @Override
    public boolean supportMultiRoute() {
        return true;
    }

    @Override
    public Set<RouteInfo> getAllRoutes(Object stateId) {
        Preconditions.checkArgument(stateId instanceof List);
        List<String> sessionIds = (List<String>) stateId;
        return sessionIds.stream().map(connectSessionStateManager::getRouteInfo).collect(
                Collectors.toSet());
    }

    @Override
    public SuccessResponse<RecyclebinSettings> handleMultiResponse(List<SingleNodeStateResponse> dispatchResponses,
            Object currentNodeRes)
            throws Throwable {
        List<RecyclebinSettings> allRes = new ArrayList<>();
        if (currentNodeRes != null) {
            Preconditions.checkArgument(currentNodeRes instanceof SuccessResponse);
            allRes.add(((SuccessResponse<RecyclebinSettings>) currentNodeRes).getData());
        }
        for (SingleNodeStateResponse response : dispatchResponses) {
            Throwable thrown = response.getThrown();
            if (thrown != null) {
                throw thrown;
            }
            RecyclebinSettings data = response.getDispatchResponse().getContentByType(
                    new TypeReference<SuccessResponse<RecyclebinSettings>>() {}).getData();
            allRes.add(data);
        }
        RecyclebinSettings recyclebinSettings = allRes.stream().filter(Objects::nonNull).findFirst().orElseThrow(
                () -> new IllegalStateException("all recyclebinSettings is null"));
        return Responses.ok(recyclebinSettings);
    }
}
