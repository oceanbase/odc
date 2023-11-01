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
package com.oceanbase.odc.service.integration;

import org.springframework.context.ApplicationEvent;

import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.IntegrationType;

import lombok.Getter;

public class IntegrationEvent extends ApplicationEvent {

    private static final String PRE_CREATE = "PRE_CREATE";
    private static final String PRE_DELETE = "PRE_DELETE";
    private static final String PRE_UPDATE = "PRE_UPDATE";

    private final String type;

    @Getter
    private final IntegrationConfig preConfig;

    @Getter
    private final IntegrationConfig currentConfig;

    @Getter
    private String salt;

    private IntegrationEvent(String type, IntegrationConfig currentConfig,
            IntegrationConfig preConfig, String salt) {
        super(currentConfig);
        this.type = type;
        this.currentConfig = currentConfig;
        this.preConfig = preConfig;
        this.salt = salt;
    }

    public static IntegrationEvent createPreCreate(IntegrationConfig currentConfig) {
        return new IntegrationEvent(PRE_CREATE, currentConfig, null, null);
    }

    public static IntegrationEvent createPreDelete(IntegrationConfig currentConfig) {
        return new IntegrationEvent(PRE_DELETE, currentConfig, null, null);
    }

    public static IntegrationEvent createPreUpdate(IntegrationConfig currentConfig, IntegrationConfig oldConfig,
            String salt) {
        return new IntegrationEvent(PRE_UPDATE, currentConfig, oldConfig, salt);
    }

    public IntegrationType getCurrentIntegrationType() {
        return currentConfig.getType();
    }

    public boolean isPreCreate() {
        return PRE_CREATE.equals(type);
    }

    public boolean isPreDelete() {
        return PRE_DELETE.equals(type);

    }

    public boolean isPreUpdate() {
        return PRE_UPDATE.equals(type);

    }
}
