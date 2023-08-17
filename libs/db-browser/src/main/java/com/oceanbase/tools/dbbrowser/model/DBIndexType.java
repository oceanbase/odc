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
package com.oceanbase.tools.dbbrowser.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DBIndexType {
    STATISTIC("STATISTIC"),
    FULLTEXT("FULLTEXT"),
    NORMAL("NORMAL"),
    UNIQUE("UNIQUE"),
    BITMAP("BITMAP"),
    FUNCTION_BASED_NORMAL("FUNCTION-BASED NORMAL"),
    FUNCTION_BASED_BITMAP("FUNCTION-BASED BITMAP"),
    DOMAIN("DOMAIN"),
    SPATIAL("SPATIAL"),
    UNKNOWN("UNKNOWN");

    private String value;

    DBIndexType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getName() {
        return this.name();
    }

    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static DBIndexType fromString(String value) {
        for (DBIndexType type : DBIndexType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return DBIndexType.UNKNOWN;
    }

}

