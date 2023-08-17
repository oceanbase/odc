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

public enum OBDatabaseUserType {
    ADMIN("Admin"),
    NORMAL("Normal"),
    ;

    private String value;

    OBDatabaseUserType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static OBDatabaseUserType fromValue(String value) {
        for (OBDatabaseUserType userType : OBDatabaseUserType.values()) {
            if (StringUtils.equalsIgnoreCase(userType.getValue(), value)) {
                return userType;
            }
        }
        throw new IllegalArgumentException("DatabaseUserType value not supported, given value '" + value + "'");
    }
}
