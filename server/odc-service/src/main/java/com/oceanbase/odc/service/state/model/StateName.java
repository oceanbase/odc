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

import com.oceanbase.odc.service.session.ConnectSessionStateManager;
import com.oceanbase.odc.service.state.StatefulUuidStateIdManager;

import lombok.Getter;

@Getter
public enum StateName {
    NONE(null),
    UUID_STATEFUL_ID(StatefulUuidStateIdManager.class),
    DB_SESSION(ConnectSessionStateManager.class);

    private final Class<? extends StateManager> stateManagerClass;

    StateName(Class<? extends StateManager> stateManagerClass) {
        this.stateManagerClass = stateManagerClass;
    }
}
