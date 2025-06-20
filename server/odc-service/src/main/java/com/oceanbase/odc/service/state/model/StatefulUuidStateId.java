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

import javax.annotation.Nullable;

import lombok.Data;

@Data
public class StatefulUuidStateId {
    @Nullable
    private String type;
    @Nullable
    private String originId;
    private String uuid;
    private String from;


    public static StatefulUuidStateId createUuidStateId(String uuid, String from) {
        StatefulUuidStateId statefulUuidStateId = new StatefulUuidStateId();
        statefulUuidStateId.uuid = uuid;
        statefulUuidStateId.from = from;
        return statefulUuidStateId;
    }

    public static StatefulUuidStateId createUuidStateId(String type, String originId, String uuid, String from) {
        StatefulUuidStateId typeUuidStateId = createTypeUuidStateId(type, uuid, from);
        typeUuidStateId.setOriginId(originId);
        return typeUuidStateId;
    }

    public static StatefulUuidStateId createTypeUuidStateId(String type, String uuid, String from) {
        StatefulUuidStateId uuidStateId = createUuidStateId(uuid, from);
        uuidStateId.type = type;
        return uuidStateId;
    }
}
