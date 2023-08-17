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

/**
 * INITIALLY AND DEFERRABLE options, optional values: <br>
 * - DEFERRABLE INITIALLY IMMEDIATE <br>
 * - DEFERRABLE INITIALLY DEFERRED <br>
 * - NOT DEFERRABLE <br>
 *
 * The DEFERRABLE and NOT DEFERRABLE parameters indicate whether in subsequent transactions,
 * constraint checking can be deferred until the end of the transaction using the SET CONSTRAINT(S)
 * statement. If you omit this clause, then the default is NOT DEFERRABLE.
 *
 */
public enum DBConstraintDeferability {
    INITIALLY_DEFERRED("DEFERRED", "INITIALLY_DEFERRED"),
    INITIALLY_IMMEDIATE("IMMEDIATE", "INITIALLY_IMMEDIATE"),
    NOT_DEFERRABLE("NOT DEFERRABLE", "NOT_DEFERRABLE"),
    UNKNOWN("UNKNOWN");

    private String[] values;

    DBConstraintDeferability(String... values) {
        this.values = values;
    }

    @JsonValue
    public String getName() {
        return this.name();
    }

    public String getValue() {
        return this.values[0];
    }

    @JsonCreator
    public static DBConstraintDeferability fromString(String value) {
        for (DBConstraintDeferability deferability : DBConstraintDeferability.values()) {
            for (String singleValue : deferability.values)
                if (singleValue.equalsIgnoreCase(value)) {
                    return deferability;
                }
        }
        return DBConstraintDeferability.UNKNOWN;
    }
}
