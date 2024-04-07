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
package com.oceanbase.odc.service.state.model;

import com.oceanbase.odc.service.dispatch.DispatchResponse;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SingleNodeStateResponse {
    private Object stateId;
    private DispatchResponse dispatchResponse;
    private RouteInfo routeInfo;
    private Throwable thrown;

    public SingleNodeStateResponse(Object stateId, RouteInfo routeInfo, DispatchResponse dispatchResponse) {
        this.stateId = stateId;
        this.dispatchResponse = dispatchResponse;
        this.routeInfo = routeInfo;
    }

    public static SingleNodeStateResponse error(Object stateId, RouteInfo routeInfo, Throwable thrown) {
        SingleNodeStateResponse response = new SingleNodeStateResponse();
        response.thrown = thrown;
        response.stateId = stateId;
        response.routeInfo = routeInfo;
        return response;
    }
}
