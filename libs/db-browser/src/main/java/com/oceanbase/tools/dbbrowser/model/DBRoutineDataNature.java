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
 * {@link DBRoutineDataNature}
 *
 * @author jingtian
 * @date 2023/4/10
 * @since db-browser_1.0.0-SNAPSHOT
 */
public enum DBRoutineDataNature {
    /**
     * 表示例程不包含读写数据的语句
     **/
    CONTAINS_SQL("CONTAINS SQL"),
    /**
     * 表示例程不包含 SQL 语句
     **/
    NO_SQL("NO SQL"),
    /**
     * 表示例程包含读取数据的语句（例如 SELECT），但不包含写入数据的语句
     **/
    READS_SQL("READS SQL"),
    /**
     * 表示例程包含可能写入数据的语句（例如 INSERT 或 DELETE）
     **/
    MODIFIES_SQL("MODIFIES SQL");


    private String value;

    DBRoutineDataNature(String value) {
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
    public static DBRoutineDataNature fromString(String value) {
        for (DBRoutineDataNature type : DBRoutineDataNature.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Illegal parameter value, " + value);
    }
}
