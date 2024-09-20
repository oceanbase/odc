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
package com.oceanbase.odc.service.connection.model;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * copy from obcloud-datasource, and package is
 * com.oceanbase.obcloud.datasource.integration.ocp.model.OBTenantStatusResp <br>
 * OB 租户的状态，来源于 OCP 的TenantStatus 枚举值<br>
 * 这里将OCP的所有状态分为两类：ONLINE 和 NOT_CONNECTABLE
 */
public enum OBTenantStatus {
    ONLINE("ONLINE"),
    NOT_CONNECTABLE("NOT_CONNECTABLE");

    private final String name;
    private static final List<String> NOT_CONNECTABLE_VALUES =
            Arrays.asList("PENDING_OFFLINE", "DELETED", "STOPPED", "END", "PENDING_CREATE");

    OBTenantStatus(String name) {
        this.name = name;
    }

    @JsonValue
    public String getValue() {
        return this.name;
    }

    @JsonCreator
    public static OBTenantStatus fromValue(String name) {
        if (NOT_CONNECTABLE_VALUES.contains(name)) {
            return NOT_CONNECTABLE;
        } else {
            return ONLINE;
        }
    }
}
