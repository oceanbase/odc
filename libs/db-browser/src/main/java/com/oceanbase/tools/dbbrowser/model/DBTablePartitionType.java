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

public enum DBTablePartitionType {
    /**
     * 表达式必须返回 INT 类型。
     */
    HASH("HASH"),

    /**
     * KEY 分区和 HASH 分区有点类似，区别是不支持表达式、列类型可以不是 INT <br>
     * 可以指定或不指定列，也可以指定多个列作为分区键。 <br>
     * 如果表上有主键，那么这些列必须是表的主键的一部分，或者全部。<br>
     * 如果 Key 分区不指定分区键，那么分区键就是主键列。<br>
     * 如果没有主键，有 UNIQUE 键，那么分区键就是 UNIQUE 键。
     */
    KEY("KEY"),

    /**
     * RANGE 分区可使用整型列 或者 表达式。
     */
    RANGE("RANGE"),

    /**
     * RANGE COLUMNS 可不使用整形列，但不支持表达式。
     */
    RANGE_COLUMNS("RANGE COLUMNS", "RANGE_COLUMNS"),

    /**
     * List 分区仅支持单分区键，分区键可以是一列，也可以是一个表达式。<br>
     * 分区键的数据类型仅支持 INT 类型。
     */
    LIST("LIST"),

    /**
     * List Columns 分区是 List 分区的一个扩展。<br>
     * 支持多个分区键，并且支持 INT 数据、DATE 类型和 DATETIME 类型。
     */
    LIST_COLUMNS("LIST COLUMNS", "LIST_COLUMNS"),

    /**
     * 非分区表。
     */
    NOT_PARTITIONED("NOT PARTITIONED", "NOT_PARTITIONED"),
    UNKNOWN;

    /**
     * 是否支持作为二级分区。
     */
    public boolean supportsAsSubpartition() {
        return this == RANGE || this == LIST || this == HASH;
    }

    /**
     * 是否支持表达式
     */
    public boolean supportExpression() {
        return this == RANGE || this == LIST || this == HASH;
    }

    private String[] values;

    DBTablePartitionType(String... values) {
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
    public static DBTablePartitionType fromValue(String value) {
        for (DBTablePartitionType type : DBTablePartitionType.values()) {
            for (String singleValue : type.values)
                if (singleValue.equalsIgnoreCase(value)) {
                    return type;
                }
        }
        return DBTablePartitionType.NOT_PARTITIONED;
    }
}

