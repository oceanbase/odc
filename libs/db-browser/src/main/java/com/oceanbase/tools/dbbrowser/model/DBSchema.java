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

import org.apache.commons.lang3.Validate;

import lombok.Data;

/**
 * Represents a logical namespace with access control layer for database objects <br>
 * - for MySQL: schema <br>
 * - for Oracle: user <br>
 */
@Data
public class DBSchema implements DBObject, DBObjectWarningDescriptor {

    private String name;
    private String charsetName;
    private String collationName;
    private String comment;

    /**
     * below OceanBase special
     */
    private String primaryZone;
    private String locality;
    private Integer replicaNum;
    private String defaultTablegroupName;
    private Boolean writeable;

    private String warning;

    public static DBSchema of(String schemaName) {
        Validate.notBlank(schemaName, "schemaName");
        DBSchema schema = new DBSchema();
        schema.setName(schemaName);
        return schema;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.SCHEMA;
    }
}

