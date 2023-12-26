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
package com.oceanbase.tools.dbbrowser.util;

abstract class AbstractOracleDataDictTableNames implements OracleDataDictTableNames {
    protected final String SCHEMA_NAME = "SYS";

    @Override
    public String TABLES() {
        return prefix() + "TABLES";
    }

    @Override
    public String VIEWS() {
        return prefix() + "VIEWS";
    }

    @Override
    public String TAB_COLUMNS() {
        return prefix() + "TAB_COLUMNS";
    }

    @Override
    public String COL_COMMENTS() {
        return prefix() + "COL_COMMENTS";
    }

    @Override
    public String TAB_COLS() {
        return prefix() + "TAB_COLS";
    }

    @Override
    public String TAB_COMMENTS() {
        return prefix() + "TAB_COMMENTS";
    }

    @Override
    public String CONSTRAINTS() {
        return prefix() + "CONSTRAINTS";
    }

    @Override
    public String CONS_COLUMNS() {
        return prefix() + "CONS_COLUMNS";
    }

    @Override
    public String INDEXES() {
        return prefix() + "INDEXES";
    }

    @Override
    public String IND_COLUMNS() {
        return prefix() + "IND_COLUMNS";
    }

    @Override
    public String IND_EXPRESSIONS() {
        return prefix() + "IND_EXPRESSIONS";
    }

    @Override
    public String TAB_PARTITIONS() {
        return prefix() + "TAB_PARTITIONS";
    }

    @Override
    public String PART_KEY_COLUMNS() {
        return prefix() + "PART_KEY_COLUMNS";
    }

    @Override
    public String PART_TABLES() {
        return prefix() + "PART_TABLES";
    }

    @Override
    public String SOURCE() {
        return prefix() + "SOURCE";
    }

    @Override
    public String OBJECTS() {
        return prefix() + "OBJECTS";
    }

    @Override
    public String ARGUMENTS() {
        return prefix() + "ARGUMENTS";
    }

    @Override
    public String SEQUENCES() {
        return prefix() + "SEQUENCES";
    }

    @Override
    public String SYNONYMS() {
        return prefix() + "SYNONYMS";
    }

    @Override
    public String TRIGGERS() {
        return prefix() + "TRIGGERS";
    }

    @Override
    public String TYPES() {
        return prefix() + "TYPES";
    }

    @Override
    public String SEGMENTS() {
        return prefix() + "SEGMENTS";
    }

    @Override
    public String USERS() {
        return prefix() + "USERS";
    }
}
