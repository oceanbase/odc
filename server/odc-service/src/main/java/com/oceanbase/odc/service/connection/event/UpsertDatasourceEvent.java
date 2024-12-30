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
package com.oceanbase.odc.service.connection.event;

import com.oceanbase.odc.common.event.AbstractEvent;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Getter;

/**
 * @Author：tianke
 * @Date: 2024/12/30 10:41
 * @Descripition:
 */
public class UpsertDatasourceEvent extends AbstractEvent {

    @Getter
    private ConnectionConfig connectionConfig;

    public UpsertDatasourceEvent(ConnectionConfig connectionConfig) {
        super(connectionConfig, "UpsertDatasourceEvent");
        this.connectionConfig = connectionConfig;

    }
}
