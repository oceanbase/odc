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
package com.oceanbase.tools.dbbrowser.schema.postgre;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;

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
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostgresSchemaAccessor implements DBSchemaAccessor {

    protected JdbcOperations jdbcOperations;

    public PostgresSchemaAccessor(@NonNull JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public List<String> showDatabases() {
        String sql = "SELECT datname FROM pg_database;";
        return jdbcOperations.queryForList(sql, String.class);
    }

    @Override
    public DBDatabase getDatabase(String schemaName) {
        return null;
    }

    @Override
    public List<DBDatabase> listDatabases() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT datname AS database_name,pg_encoding_to_char(encoding) AS character_set,"
                + "datcollate AS collation FROM pg_database;");
        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBDatabase database = new DBDatabase();
            database.setId(rs.getString("database_name"));
            database.setName(rs.getString("database_name"));
            database.setCharset(rs.getString("character_set"));
            database.setCollation(rs.getString("collation"));
            return database;
        });

    }

    @Override
    public void switchDatabase(String schemaName) {

    }

    @Override
    public List<DBObjectIdentity> listUsers() {
        return null;
    }

    @Override
    public List<String> showTables(String schemaName) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'");
        sb.append(" AND table_name NOT IN ("
                  + "        SELECT child.relname "
                  + "        FROM pg_inherits "
                  + "        JOIN pg_class child ON pg_inherits.inhrelid = child.oid "
                  + "        JOIN pg_namespace nmsp_child ON nmsp_child.oid = child.relnamespace "
                  + "        WHERE nmsp_child.nspname = 'public') ");
        String sql = sb.toString();
        if (StringUtils.isNotBlank(schemaName)) {
            sb.append("AND table_catalog").append(" = '%s'");
            sql = String.format(sb.toString(), schemaName);
        }
        List<String> tableNames;
        try {
            tableNames = jdbcOperations.query(sql, (rs, rowNum) -> rs.getString(1));
        } catch (BadSqlGrammarException e) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "Unknown database")) {
                return Collections.emptyList();
            }
            throw e;
        }
        return tableNames;
    }

    @Override
    public List<String> showTablesLike(String schemaName, String tableNameLike) {
        return null;
    }

    @Override
    public List<DBObjectIdentity> listTables(String schemaName, String tableNameLike) {
        return null;
    }

    @Override
    public List<DBObjectIdentity> listViews(String schemaName) {
        return null;
    }

    @Override
    public List<DBObjectIdentity> listAllViews(String viewNameLike) {
        return null;
    }

    @Override
    public List<DBObjectIdentity> listAllUserViews() {
        return null;
    }

    @Override
    public List<DBObjectIdentity> listAllSystemViews() {
        return null;
    }

    @Override
    public List<String> showSystemViews(String schemaName) {
        return null;
    }

    @Override
    public List<DBVariable> showVariables() {
        return null;
    }

    @Override
    public List<DBVariable> showSessionVariables() {
        return null;
    }

    @Override
    public List<DBVariable> showGlobalVariables() {
        return null;
    }

    @Override
    public List<String> showCharset() {
        return null;
    }

    @Override
    public List<String> showCollation() {
        return null;
    }

    @Override
    public List<DBPLObjectIdentity> listFunctions(String schemaName) {
        return null;
    }

    @Override
    public List<DBPLObjectIdentity> listProcedures(String schemaName) {
        return null;
    }

    @Override
    public List<DBPLObjectIdentity> listPackages(String schemaName) {
        return null;
    }

    @Override
    public List<DBPLObjectIdentity> listPackageBodies(String schemaName) {
        return null;
    }

    @Override
    public List<DBPLObjectIdentity> listTriggers(String schemaName) {
        return null;
    }

    @Override
    public List<DBPLObjectIdentity> listTypes(String schemaName) {
        return null;
    }

    @Override
    public List<DBObjectIdentity> listSequences(String schemaName) {
        return null;
    }

    @Override
    public List<DBObjectIdentity> listSynonyms(String schemaName,
            DBSynonymType synonymType) {
        return null;
    }

    @Override
    public Map<String, List<DBTableColumn>> listTableColumns(
            String schemaName, List<String> tableNames) {
        return null;
    }

    @Override
    public List<DBTableColumn> listTableColumns(String schemeName, String tableName) {
        return null;
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicTableColumns(String schemaName) {
        return null;
    }

    @Override
    public List<DBTableColumn> listBasicTableColumns(String schemaName, String tableName) {
        return null;
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicViewColumns(String schemaName) {
        return null;
    }

    @Override
    public List<DBTableColumn> listBasicViewColumns(String schemaName, String viewName) {
        return null;
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicColumnsInfo(String schemaName) {
        return null;
    }

    @Override
    public Map<String, List<DBTableIndex>> listTableIndexes(String schemaName) {
        return null;
    }

    @Override
    public Map<String, List<DBTableConstraint>> listTableConstraints(
            String schemaName) {
        return null;
    }

    @Override
    public Map<String, DBTableOptions> listTableOptions(
            String schemaName) {
        return null;
    }

    @Override
    public Map<String, DBTablePartition> listTablePartitions(
            @NonNull String schemaName, List<String> tableNames) {
        return null;
    }

    @Override
    public List<DBTablePartition> listTableRangePartitionInfo(String tenantName) {
        return null;
    }

    @Override
    public List<DBTableSubpartitionDefinition> listSubpartitions(
            String schemaName, String tableName) {
        return null;
    }

    @Override
    public Boolean isLowerCaseTableName() {
        return null;
    }

    @Override
    public List<DBObjectIdentity> listPartitionTables(String partitionMethod) {
        return null;
    }

    @Override
    public List<DBTableConstraint> listTableConstraints(String schemaName, String tableName) {
        return null;
    }

    @Override
    public DBTablePartition getPartition(String schemaName, String tableName) {
        return null;
    }

    @Override
    public List<DBTableIndex> listTableIndexes(String schemaName, String tableName) {
        return null;
    }

    @Override
    public String getTableDDL(String schemaName, String tableName) {
        return null;
    }

    @Override
    public DBTableOptions getTableOptions(String schemaName, String tableName) {
        return null;
    }

    @Override
    public DBTableOptions getTableOptions(String schemaName, String tableName, String ddl) {
        return null;
    }

    @Override
    public List<DBColumnGroupElement> listTableColumnGroups(String schemaName,
            String tableName) {
        return null;
    }

    @Override
    public DBView getView(String schemaName, String viewName) {
        return null;
    }

    @Override
    public DBFunction getFunction(String schemaName, String functionName) {
        return null;
    }

    @Override
    public DBProcedure getProcedure(String schemaName, String procedureName) {
        return null;
    }

    @Override
    public DBPackage getPackage(String schemaName, String packageName) {
        return null;
    }

    @Override
    public DBTrigger getTrigger(String schemaName, String packageName) {
        return null;
    }

    @Override
    public DBType getType(String schemaName, String typeName) {
        return null;
    }

    @Override
    public DBSequence getSequence(String schemaName, String sequenceName) {
        return null;
    }

    @Override
    public DBSynonym getSynonym(String schemaName, String synonymName,
            DBSynonymType synonymType) {
        return null;
    }

    @Override
    public Map<String, DBTable> getTables(String schemaName,
            List<String> tableNames) {
        return null;
    }
}
