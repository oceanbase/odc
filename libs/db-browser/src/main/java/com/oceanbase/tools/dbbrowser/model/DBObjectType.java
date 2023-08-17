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

import lombok.Getter;

@Getter
public enum DBObjectType {
    /**
     * table and view
     */
    SCHEMA("SCHEMA"),
    TABLE("TABLE"),
    COLUMN("COLUMN"),
    INDEX("INDEX"),
    CONSTRAINT("CONSTRAINT"),
    PARTITION("PARTITION"),
    SUBPARTITION("SUBPARTITION"),
    VIEW("VIEW"),
    /**
     * other
     */
    TRIGGER("TRIGGER"),
    SEQUENCE("SEQUENCE"),
    PROCEDURE("PROCEDURE"),
    FUNCTION("FUNCTION"),
    PACKAGE("PACKAGE"),
    PACKAGE_BODY("PACKAGE BODY"),
    SYNONYM("SYNONYM"),
    PUBLIC_SYNONYM("PUBLIC SYNONYM"),
    TYPE("TYPE"),
    DATABASE("DATABASE"),
    ANONYMOUS_BLOCK("ANONYMOUSBLOCK"),
    USER("USER"),
    GLOBAL_VARIABLE("GLOBAL VARIABLE"),
    SESSION_VARIABLE("SESSION VARIABLE"),
    USER_VARIABLE("USER VARIABLE"),
    SYSTEM_VARIABLE("SYSTEM VARIABLE"),
    OTHERS("OTHERS");

    private final String name;

    DBObjectType(String name) {
        this.name = name;
    }

    public static DBObjectType getEnumByName(String name) {
        DBObjectType result = DBObjectType.OTHERS;
        DBObjectType[] types = DBObjectType.values();
        for (DBObjectType type : types) {
            if (type.getName().equalsIgnoreCase(name)) {
                result = type;
                break;
            }
        }
        return result;
    }

}
