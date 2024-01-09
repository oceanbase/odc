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

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(
        exclude = {"name", "warning", "schemaName", "owner", "tableName", "createTime", "updateTime", "ordinalPosition",
                "enabled"})
public class DBTableConstraint implements DBObject, DBObjectWarningDescriptor {
    /**
     * 所属 schemaName
     */
    private String schemaName;

    /**
     * 所属 tableName
     */
    private String tableName;

    private String name;
    private DBConstraintType type;
    private List<String> columnNames;
    private String referenceSchemaName;
    private String referenceTableName;
    private List<String> referenceColumnNames;
    private String comment;
    private String checkClause;
    private DBTableIndex usingIndex;
    private Boolean validate;
    private DBConstraintDeferability deferability;
    private Boolean enabled;

    private Integer ordinalPosition;

    /**
     * 外键匹配方式，当 type=FOREIGN_KEY 时有效
     */
    private DBForeignKeyMatchType matchType;
    /**
     * ON UPDATE Action，当 type=FOREIGN_KEY 时有效
     */
    private DBForeignKeyModifyRule onUpdateRule;
    /**
     * ON DELETE Action，当 type=FOREIGN_KEY 时有效
     */
    private DBForeignKeyModifyRule onDeleteRule;

    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp updateTime;
    private String owner;

    private String warning;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.CONSTRAINT;
    }
}

