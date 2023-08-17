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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DBColumnTypeDisplay {
    DISPLAY_PRECISION_AND_SCALE("decimal", "float", "double", "NUMBER"),
    DISPLAY_ONLY_PRECISION("bit", "int", "tinyint", "smallint", "mediumint", "bigint", "varchar", "char",
            "binary", "varbinary",
            "timestamp", "time", "datetime", "year", "INTEGER", "CHAR", "VARCHAR2", "RAW",
            "NCHAR", "VARCHAR",
            "NVARCHAR2", "FLOAT", "UROWID"),
    NOT_DISPLAY_PRECISION_AND_SCALE("boolean", "bool", "text", "tinytext", "mediumtext", "longtext", "tinyblob",
            "longblob",
            "mediumblob", "date", "BLOB", "CLOB", "DATE", "BINARY_FLOAT", "BINARY_DOUBLE", "ROWID",
            "INTERVAL DAY TO SECOND",
            "INTERVAL YEAR TO MONTH", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE"),


    ;

    DBColumnTypeDisplay(String... typeNames) {
        this.typeNames = typeNames;
    }

    private String[] typeNames;

    public boolean displayPrecision() {
        return this == DISPLAY_ONLY_PRECISION || this == DISPLAY_PRECISION_AND_SCALE;
    }

    public boolean displayScale() {
        return this == DISPLAY_PRECISION_AND_SCALE;
    }

    @JsonCreator
    public static DBColumnTypeDisplay fromName(String name) {
        for (DBColumnTypeDisplay type : DBColumnTypeDisplay.values()) {
            for (String singleValue : type.typeNames)
                if (StringUtils.equals(singleValue, name)) {
                    return type;
                }
        }
        return NOT_DISPLAY_PRECISION_AND_SCALE;
    }
}

