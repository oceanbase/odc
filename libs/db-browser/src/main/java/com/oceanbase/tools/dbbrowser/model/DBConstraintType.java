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

public enum DBConstraintType {
    FOREIGN_KEY("FOREIGN KEY", "FOREIGN_KEY", "R"),
    PRIMARY_KEY("PRIMARY KEY", "PRIMARY_KEY", "P"),
    UNIQUE_KEY("UNIQUE", "UNIQUE_KEY", "U"),
    INDEX("INDEX"),
    CHECK("CHECK", "C"),
    NOT_NULL("NOT NULL", "NOT_NULL"),
    UNKNOWN("UNKNOWN"),
    ;

    private String[] values;

    DBConstraintType(String... values) {
        this.values = values;
    }

    public String[] getValues() {
        return this.values;
    }

    @JsonValue
    public String getName() {
        return this.name();
    }

    public String getValue() {
        return this.values[0];
    }

    @JsonCreator
    public static DBConstraintType fromValue(String value) {
        for (DBConstraintType type : DBConstraintType.values()) {
            for (String singleValue : type.values)
                if (singleValue.equalsIgnoreCase(value)) {
                    return type;
                }
        }
        return DBConstraintType.UNKNOWN;
    }
}

