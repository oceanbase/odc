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

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/8/26 19:27
 * @since: 4.4.1
 */
public enum DBExternalFunctionDisplayType {

    JAVA_UDF("JAVA UDF"),
    JAVA_UDAF("JAVA UDAF"),
    JAVA_UDTF("JAVA UDTF"),
    PYTHON_UDF("PYTHON UDF"),
    PYTHON_UDAF("PYTHON UDAF"),
    PYTHON_UDTF("PYTHON UDTF"),
    NULL("NULL"),
    OTHERS("OTHERS");

    private final String name;

    DBExternalFunctionDisplayType(String name) {
        this.name = name;
    }

    @JsonCreator
    public static DBExternalFunctionDisplayType getEnumByName(String name) {
        DBExternalFunctionDisplayType result = DBExternalFunctionDisplayType.OTHERS;
        DBExternalFunctionDisplayType[] types = DBExternalFunctionDisplayType.values();
        for (DBExternalFunctionDisplayType type : types) {
            if (type.name.equalsIgnoreCase(name)) {
                result = type;
                break;
            }
        }
        return result;
    }

}
