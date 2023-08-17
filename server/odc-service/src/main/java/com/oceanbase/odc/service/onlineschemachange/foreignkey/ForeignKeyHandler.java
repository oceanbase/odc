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
package com.oceanbase.odc.service.onlineschemachange.foreignkey;

/**
 * foreign key handler
 *
 * @author yaobin
 * @date 2023-06-21
 * @since 4.2.0
 */
public interface ForeignKeyHandler {

    /**
     * disable all table foreign key check in session
     */
    void disableForeignKeyCheck(String schemaName, String tableName);

    /**
     * enable sing table foreign key check in session
     */
    void enableForeignKeyCheck(String schemaName, String tableName);

    /**
     * drop all foreign keys on table name
     *
     * @param schemaName schema name of table
     * @param tableName table name
     */
    void dropAllForeignKeysOnTable(String schemaName, String tableName);

    /**
     * alter table foreign keys reference from old table name to new table name
     *
     * @param schemaName schema name of table
     * @param oldTableName old table name
     * @param newTableName new table name
     */
    void alterTableForeignKeyReference(String schemaName, String oldTableName, String newTableName);

}
