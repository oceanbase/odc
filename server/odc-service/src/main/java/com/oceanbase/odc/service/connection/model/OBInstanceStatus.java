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

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OBInstanceStatus {
    ONLINE("ONLINE"),

    PENDING_CREATE("PENDING_CREATE"),

    ARREARS_CLOSED("ARREARS_CLOSED"),

    PREPAID_EXPIRE_CLOSED("PREPAID_EXPIRE_CLOSED"),

    UPGRADING("UPGRADING"),

    PENDING_DELETE("PENDING_DELETE"),

    DELETED("DELETED"),

    ABNORMAL("ABNORMAL"),

    OFFLINE("OFFLINE"),

    PENDING_STOP("PENDING_STOP"),

    STOPPED("STOPPED"),

    PENDING_START("PENDING_START");

    private String name;


    OBInstanceStatus(String name) {
        this.name = name;
    }

    @JsonValue
    public String getValue() {
        return this.name;
    }

    @JsonCreator
    public static OBInstanceStatus fromValue(String name) {
        for (OBInstanceStatus status : OBInstanceStatus.values()) {
            if (StringUtils.equalsIgnoreCase(status.name, name)) {
                return status;
            }
        }
        return ONLINE;
    }
}
