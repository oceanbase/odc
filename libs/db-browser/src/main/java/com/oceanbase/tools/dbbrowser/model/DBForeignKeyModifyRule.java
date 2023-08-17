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
 * Reference action for foreign key modify <br>
 *
 * Oracle supported actions:
 *
 * <pre>
 * - ON UPDATE: not supported, there is no ON UPDATE option for foreign key references for Oracle
 * - ON DELETE: CASCADE / SET NULL
 * </pre>
 *
 * MySQL supported actions
 *
 * <pre>
 * - ON UPDATE: CASCADE / SET NULL /  RESTRICT / NO ACTION
 * - ON DELETE: CASCADE / SET NULL /  RESTRICT / NO ACTION
 * </pre>
 *
 * MySQL options explanation <br>
 * - CASCADE: Delete or update the row from the parent table and automatically delete or update the
 * matching rows in the child table. Both ON DELETE CASCADE and ON UPDATE CASCADE are supported.
 * Between two tables, do not define several ON UPDATE CASCADE clauses that act on the same column
 * in the parent table or in the child table. <br>
 * - RESTRICT: Rejects the delete or update operation for the parent table. Specifying RESTRICT (or
 * NO ACTION) is the same as omitting the ON DELETE or ON UPDATE clause. <br>
 * - NO ACTION: A keyword from standard SQL. In MySQL, equivalent to RESTRICT. The MySQL Server
 * rejects the delete or update operation for the parent table if there is a related foreign key
 * value in the referenced table. Some database systems have deferred checks, and NO ACTION is a
 * deferred check. In MySQL, foreign key constraints are checked immediately, so NO ACTION is the
 * same as RESTRICT. <br>
 * - SET DEFAULT action is recognized by the MySQL parser, but both InnoDB and NDB reject table
 * definitions containing ON DELETE SET DEFAULT or ON UPDATE SET DEFAULT clauses.
 */
public enum DBForeignKeyModifyRule {
    RESTRICT("RESTRICT"),
    CASCADE("CASCADE"),
    SET_NULL("SET NULL", "SET_NULL"),
    NO_ACTION("NO ACTION", "NO_ACTION"),
    SET_DEFAULT("SET DEFAULT", "SET_DEFAULT"),
    UNKNOWN("UNKNOWN");

    private String[] values;

    DBForeignKeyModifyRule(String... values) {
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
    public static DBForeignKeyModifyRule fromValue(String value) {
        for (DBForeignKeyModifyRule rule : DBForeignKeyModifyRule.values()) {
            for (String singleValue : rule.values)
                if (singleValue.equalsIgnoreCase(value)) {
                    return rule;
                }
        }
        return DBForeignKeyModifyRule.UNKNOWN;
    }
}
