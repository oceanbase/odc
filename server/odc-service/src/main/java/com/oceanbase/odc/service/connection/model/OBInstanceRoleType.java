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
import com.oceanbase.odc.common.i18n.Translatable;

import lombok.Getter;

public enum OBInstanceRoleType implements Translatable {
    // not a primary/standby instance
    NORMAL("NORMAL"),

    PRIMARY("PRIMARY"),

    PHYSICAL_STANDBY("PHYSICAL_STANDBY");

    @Getter
    private String value;

    OBInstanceRoleType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getName() {
        return this.name();
    }

    @JsonCreator
    public static OBInstanceRoleType fromValue(String value) {
        for (OBInstanceRoleType roleType : OBInstanceRoleType.values()) {
            if (StringUtils.equalsIgnoreCase(roleType.value, value)) {
                return roleType;
            }
        }
        return OBInstanceRoleType.NORMAL;
    }

    @Override
    public String code() {
        return name();
    }
}
