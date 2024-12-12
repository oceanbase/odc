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
package com.oceanbase.odc.service.pldebug.model;

import lombok.Getter;
import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/12/2 15:20
 * @since: 4.3.3
 */
@Getter
public class PLDebugODPSpecifiedRoute {

    private final String observerHost;

    private final Integer observerPort;

    public PLDebugODPSpecifiedRoute(@NonNull String observerHost, @NonNull Integer observerPort) {
        this.observerHost = observerHost;
        this.observerPort = observerPort;
    }
}
