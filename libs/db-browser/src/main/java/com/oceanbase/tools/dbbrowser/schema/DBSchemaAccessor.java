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
package com.oceanbase.tools.dbbrowser.schema;

import java.util.List;
import java.util.Map;

import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBSequence;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTableSubpartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.model.DBVariable;
import com.oceanbase.tools.dbbrowser.model.DBView;

import lombok.NonNull;

/**
 * @author jingtian
 */
public interface DBSchemaAccessor {
    /**
     * Show all databases
     */
    List<String> showDatabases();

    DBDatabase getDatabase(String schemaName);

    List<DBDatabase> listDatabases();

    /**
     * Switch Database
     */
    void switchDatabase(String schemaName);

    /**
     * list all users
     *
     * @return user list
     */
    List<DBObjectIdentity> listUsers();

    /**
     * Show all table names list in the specified schema
     */
    default List<String> showTables(String schemaName) {
        return showTablesLike(schemaName, null);
    }

    List<String> showTablesLike(String schemaName, String tableNameLike);

    /**
     * List all table as BObjectIdentity in the specified schema
     */
    List<DBObjectIdentity> listTables(String schemaName, String tableNameLike);

    /**
     * Show all view names list in the specified schema
     */
    List<DBObjectIdentity> listViews(String schemaName);

    List<DBObjectIdentity> listAllViews(String viewNameLike);

    /**
     * List all user view as DBObjectIdentity
     */
    List<DBObjectIdentity> listAllUserViews();

    /**
     * List all system view as DBObjectIdentity
     */
    List<DBObjectIdentity> listAllSystemViews();

    /**
     * Show all system view names list
     */
    List<String> showSystemViews(String schemaName);

    /**
     * List all variables
     */
    List<DBVariable> showVariables();

    /**
     * List session variables
     */
    List<DBVariable> showSessionVariables();

    /**
     * List global variables
     */
    List<DBVariable> showGlobalVariables();

    /**
     * Show the charset supported by the database
     */
    List<String> showCharset();

    /**
     * Show the collation supported by the database
     */
    List<String> showCollation();

    /**
     * Show all function list in the specified schema
     */
    List<DBPLObjectIdentity> listFunctions(String schemaName);

    /**
     * Show all procedure list in the specified schema
     */
    List<DBPLObjectIdentity> listProcedures(String schemaName);

    /**
     * Show all package list in the specified schema
     */
    List<DBPLObjectIdentity> listPackages(String schemaName);

    /**
     * Show all package body list in the specified schema
     */
    List<DBPLObjectIdentity> listPackageBodies(String schemaName);

    /**
     * Show all trigger list in the specified schema
     */
    List<DBPLObjectIdentity> listTriggers(String schemaName);

    /**
     * Show all type list in the specified schema
     */
    List<DBPLObjectIdentity> listTypes(String schemaName);

    /**
     * Show all sequence list in the specified schema
     */
    List<DBObjectIdentity> listSequences(String schemaName);

    /**
     * Show all synonym list in the specified schema
     */
    List<DBObjectIdentity> listSynonyms(String schemaName, DBSynonymType synonymType);

    /**
     * Get all table columns in the specified schema
     */
    Map<String, List<DBTableColumn>> listTableColumns(String schemaName, List<String> tableNames);

    /**
     * Get all table columns in the specified schema and table
     */
    List<DBTableColumn> listTableColumns(String schemeName, String tableName);

    /**
     * Get all table columns(hold only basic info) in the specified schema
     */
    Map<String, List<DBTableColumn>> listBasicTableColumns(String schemaName);

    /**
     * Get all table columns(hold only basic info) in the specified schema and table
     */
    List<DBTableColumn> listBasicTableColumns(String schemaName, String tableName);

    /**
     * Get all view columns(hold only basic info) in the specified schema
     */
    Map<String, List<DBTableColumn>> listBasicViewColumns(String schemaName);

    /**
     * Get all view columns(hold only basic info) in the specified schema and view
     */
    List<DBTableColumn> listBasicViewColumns(String schemaName, String viewName);

    /**
     * Get all table indexs in the specified schema
     */
    Map<String, List<DBTableIndex>> listTableIndexes(String schemaName);

    /**
     * Get all table constraints in the specified schema
     */
    Map<String, List<DBTableConstraint>> listTableConstraints(String schemaName);

    Map<String, DBTableOptions> listTableOptions(String schemaName);

    Map<String, DBTablePartition> listTablePartitions(@NonNull String schemaName, List<String> tableNames);

    /**
     * you can use {@link DBSchemaAccessor#listTablePartitions(String, List)} instead we will delete it
     * soon
     */
    @Deprecated
    List<DBTablePartition> listTableRangePartitionInfo(String tenantName);

    List<DBTableSubpartitionDefinition> listSubpartitions(String schemaName, String tableName);

    Boolean isLowerCaseTableName();

    List<DBObjectIdentity> listPartitionTables(String partitionMethod);

    List<DBTableConstraint> listTableConstraints(String schemaName, String tableName);

    DBTablePartition getPartition(String schemaName, String tableName);

    List<DBTableIndex> listTableIndexes(String schemaName, String tableName);

    String getTableDDL(String schemaName, String tableName);

    DBTableOptions getTableOptions(String schemaName, String tableName);

    DBTableOptions getTableOptions(String schemaName, String tableName, String ddl);

    List<DBColumnGroupElement> listTableColumnGroups(String schemaName, String tableName);

    DBView getView(String schemaName, String viewName);

    DBFunction getFunction(String schemaName, String functionName);

    DBProcedure getProcedure(String schemaName, String procedureName);

    DBPackage getPackage(String schemaName, String packageName);

    DBTrigger getTrigger(String schemaName, String packageName);

    DBType getType(String schemaName, String typeName);

    DBSequence getSequence(String schemaName, String sequenceName);

    DBSynonym getSynonym(String schemaName, String synonymName, DBSynonymType synonymType);

    Map<String, DBTable> getTables(String schemaName, List<String> tableNames);
}
