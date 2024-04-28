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
package com.oceanbase.odc.service.common.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceIdentifier {
    public static final String TYPE_KEY = ":ty:";
    public static final String SYNONYM_KEY = ":syn:";
    public static final String TRIGGER_KEY = ":tr:";
    public static final String SEQUENCE_KEY = ":s:";
    public static final String PACKAGE_KEY = ":pkg:";
    public static final String PARTITION_KEY = ":tp:";
    public static final String PROCEDURE_KEY = ":p:";
    public static final String FUNCTION_KEY = ":f:";
    public static final String INDEX_KEY = ":i:";
    public static final String COLUMN_KEY = ":c:";
    public static final String VIEW_KEY = ":v:";
    public static final String TABLE_KEY = ":t:";
    public static final String VARIABLE_SCOPE_KEY = ":var:";
    public static final String DATABASE_KEY = ":d:";
    public static final String DATABASE_ID_KEY = ":did:";
    public static final String SID_KEY = "sid:";

    private String sid;
    private String database;
    private String variableScope;
    private String table;
    private String view;
    private String column;
    private String index;
    private String function;
    private String procedure;
    private String partition;
    private String sequence;
    private String pkg;
    private String trigger;
    private String synonym;
    private String type;
    private Long databaseId;

    public void setValue(String key, String value) {
        switch (key) {
            case SID_KEY:
                this.sid = value;
                break;
            case DATABASE_KEY:
                this.database = value;
                break;
            case VARIABLE_SCOPE_KEY:
                this.variableScope = value;
                break;
            case TABLE_KEY:
                this.table = value;
                break;
            case VIEW_KEY:
                this.view = value;
                break;
            case COLUMN_KEY:
                this.column = value;
                break;
            case INDEX_KEY:
                this.index = value;
                break;
            case FUNCTION_KEY:
                this.function = value;
                break;
            case PROCEDURE_KEY:
                this.procedure = value;
                break;
            case PARTITION_KEY:
                this.partition = value;
                break;
            case SEQUENCE_KEY:
                this.sequence = value;
                break;
            case PACKAGE_KEY:
                this.pkg = value;
                break;
            case TRIGGER_KEY:
                this.trigger = value;
                break;
            case SYNONYM_KEY:
                this.synonym = value;
                break;
            case TYPE_KEY:
                this.type = value;
                break;
            case DATABASE_ID_KEY:
                this.databaseId = Long.parseLong(value);
                break;
        }
    }
}
