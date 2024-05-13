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
package com.oceanbase.tools.dbbrowser.schema.mysql;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBObjectWarningDescriptor;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.dbbrowser.parser.result.ParseSqlResult;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * 适用 OB 版本：[4.0.0, ~)
 *
 * @author jingtian
 */
@Slf4j
public class OBMySQLSchemaAccessor extends MySQLNoLessThan5700SchemaAccessor {

    protected static final Set<String> ESCAPE_SCHEMA_SET = new HashSet<>(3);

    static {
        ESCAPE_SCHEMA_SET.add("PUBLIC");
        ESCAPE_SCHEMA_SET.add("LBACSYS");
        ESCAPE_SCHEMA_SET.add("ORAAUDITOR");
        ESCAPE_SCHEMA_SET.add("__public");
    }

    @Override
    public List<String> showDatabases() {
        return super.showDatabases().stream().filter(database -> !ESCAPE_SCHEMA_SET.contains(database))
                .collect(Collectors.toList());
    }

    public OBMySQLSchemaAccessor(JdbcOperations jdbcOperations) {
        super(jdbcOperations);
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.OBMYSQL_40X);
    }

    @Override
    public DBDatabase getDatabase(String schemaName) {
        DBDatabase database = new DBDatabase();
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append(
                "select object_name, timestamp from oceanbase.DBA_OBJECTS where object_type = 'DATABASE' and object_name = ")
                .value(schemaName);
        jdbcOperations.query(sb.toString(), rs -> {
            String objectName = rs.getString("object_name");
            String timestamp = rs.getString("timestamp");
            database.setName(objectName);
            database.setId(objectName + "_" + timestamp);

        });
        String sql =
                "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.schemata where SCHEMA_NAME ='"
                        + schemaName + "'";
        jdbcOperations.query(sql, rs -> {
            database.setCharset(rs.getString("DEFAULT_CHARACTER_SET_NAME"));
            database.setCollation(rs.getString("DEFAULT_COLLATION_NAME"));
        });
        return database;
    }

    @Override
    public List<DBDatabase> listDatabases() {
        String sql =
                "SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.schemata;";
        return jdbcOperations.query(sql, (rs, num) -> {
            DBDatabase database = new DBDatabase();
            database.setId(rs.getString("SCHEMA_NAME"));
            database.setName(rs.getString("SCHEMA_NAME"));
            database.setCharset(rs.getString("DEFAULT_CHARACTER_SET_NAME"));
            database.setCollation(rs.getString("DEFAULT_COLLATION_NAME"));
            return database;
        }).stream().filter(database -> !ESCAPE_SCHEMA_SET.contains(database.getName())).collect(Collectors.toList());
    }

    @Override
    public List<DBObjectIdentity> listTables(String schemaName, String tableNameLike) {
        List<DBObjectIdentity> results = super.listTables(schemaName, tableNameLike);

        if (StringUtils.isBlank(schemaName) || "oceanbase".equals(schemaName)) {
            MySQLSqlBuilder querySystemTable = new MySQLSqlBuilder();
            querySystemTable.append("show full tables from oceanbase where Table_type='BASE TABLE'");
            if (StringUtils.isNotBlank(tableNameLike)) {
                querySystemTable.append(" and tables_in_oceanbase like ").value("%" + tableNameLike + "%");
            }
            try {
                List<String> tables =
                        jdbcOperations.query(querySystemTable.toString(), (rs, rowNum) -> rs.getString(1));
                tables.forEach(name -> results.add(DBObjectIdentity.of("oceanbase", DBObjectType.TABLE, name)));
            } catch (Exception e) {
                log.warn("List system tables from 'oceanbase' failed, reason={}", e.getMessage());
            }
        }

        if (StringUtils.isBlank(schemaName) || "mysql".equals(schemaName)) {
            MySQLSqlBuilder queryMysqlTable = new MySQLSqlBuilder();
            queryMysqlTable.append("show full tables from `mysql` where Table_type='BASE TABLE'");
            if (StringUtils.isNotBlank(tableNameLike)) {
                queryMysqlTable.append(" and tables_in_mysql like ").value("%" + tableNameLike + "%");
            }
            try {
                jdbcOperations.query(queryMysqlTable.toString(),
                        (rs, num) -> results.add(DBObjectIdentity.of("mysql", DBObjectType.TABLE, rs.getString(1))));
            } catch (Exception e) {
                log.warn("List base tables from 'mysql' failed, reason={}", e.getMessage());
            }
        }
        return results;
    }

    @Override
    public List<DBObjectIdentity> listAllSystemViews() {
        List<DBObjectIdentity> results = super.listAllSystemViews();
        String sql1 = "show full tables from `oceanbase` where Table_type='SYSTEM VIEW'";
        try {
            List<String> oceanbaseViews = jdbcOperations.query(sql1, (rs, rowNum) -> rs.getString(1));
            oceanbaseViews.forEach(name -> results.add(DBObjectIdentity.of("oceanbase", DBObjectType.VIEW, name)));
        } catch (Exception ex) {
            log.info("List tables for 'oceanbase' failed, reason={}", ex.getMessage());
        }

        return results;
    }

    @Override
    public List<DBTableColumn> listTableColumns(String schemaName, String tableName) {
        List<DBTableColumn> columns = super.listTableColumns(schemaName, tableName);
        setStoredColumnByDDL(schemaName, tableName, columns);
        return columns;
    }

    protected void setStoredColumnByDDL(String schemeName, String tableName, List<DBTableColumn> columns) {
        if (CollectionUtils.isEmpty(columns)) {
            return;
        }
        try {
            MySQLSqlBuilder sb = new MySQLSqlBuilder();
            sb.append("show create table ");
            sb.schemaPrefixIfNotBlank(schemeName);
            sb.identifier(tableName);
            List<String> ddl =
                    jdbcOperations.query(sb.toString(), (rs, num) -> rs.getString(2));
            if (CollectionUtils.isEmpty(ddl) || StringUtils.isBlank(ddl.get(0))) {
                fillWarning(columns, DBObjectType.COLUMN, "get table DDL failed");
            } else {
                ParseSqlResult result = SqlParser.parseMysql(ddl.get(0));
                if (CollectionUtils.isEmpty(result.getColumns())) {
                    fillWarning(columns, DBObjectType.COLUMN, "parse DDL failed, may view object");
                } else {
                    columns.forEach(column -> result.getColumns().forEach(columnDefinition -> {
                        if (StringUtils.equals(column.getName(), columnDefinition.getName())) {
                            column.setStored(columnDefinition.getIsStored());
                        }
                    }));
                }
            }
        } catch (Exception e) {
            fillWarning(columns, DBObjectType.COLUMN, "query ddl failed");
            log.warn("Fetch table ddl for parsing column failed", e);
        }
    }

    @Override
    public List<DBTableIndex> listTableIndexes(String schemaName, String tableName) {
        List<DBTableIndex> indexList = super.listTableIndexes(schemaName, tableName);
        fillIndexInfo(indexList, schemaName, tableName);
        for (DBTableIndex index : indexList) {
            if (index.getAlgorithm() == DBIndexAlgorithm.UNKNOWN) {
                index.setAlgorithm(DBIndexAlgorithm.BTREE);
            }
        }
        return indexList;
    }

    @Override
    protected void handleIndexAvailability(DBTableIndex index, String availability) {
        if ("available".equals(availability)) {
            index.setAvailable(true);
        } else if ("unavailable".equals(availability)) {
            index.setAvailable(false);
        }
    }

    @Override
    public Map<String, List<DBTableIndex>> listTableIndexes(String schemaName) {
        Map<String, List<DBTableIndex>> tableName2Indexes = super.listTableIndexes(schemaName);
        for (Map.Entry<String, List<DBTableIndex>> entry : tableName2Indexes.entrySet()) {
            fillIndexInfo(entry.getValue(), schemaName, entry.getKey());
            for (DBTableIndex index : entry.getValue()) {
                if (index.getAlgorithm() == DBIndexAlgorithm.UNKNOWN) {
                    index.setAlgorithm(DBIndexAlgorithm.BTREE);
                }
            }
        }
        return tableName2Indexes;
    }

    public Map<String, List<DBTableIndex>> listTableIndexes(String schemaName, Map<String, String> tableName2Ddl) {
        Map<String, List<DBTableIndex>> tableName2Indexes = super.listTableIndexes(schemaName);
        tableName2Indexes.keySet().forEach(tableName -> {
            if (tableName2Ddl.containsKey(tableName)) {
                parseDdlToSetIndexInfo(tableName2Ddl.get(tableName), tableName2Indexes.get(tableName));
            } else {
                fillIndexInfo(tableName2Indexes.get(tableName), schemaName, tableName);
            }
        });
        return tableName2Indexes;
    }

    @Override
    public List<DBColumnGroupElement> listTableColumnGroups(String schemaName, String tableName) {
        return listTableColumnGroups(getTableDDL(schemaName, tableName));
    }

    private List<DBColumnGroupElement> listTableColumnGroups(String ddl) {
        SQLParser sqlParser = new OBMySQLParser();
        CreateTable stmt = (CreateTable) sqlParser.parse(new StringReader(ddl));
        return stmt.getColumnGroupElements() == null ? Collections.emptyList()
                : stmt.getColumnGroupElements().stream()
                        .map(DBColumnGroupElement::ofColumnGroupElement).collect(Collectors.toList());
    }

    @Override
    protected boolean isIndexDistinguishesVisibility() {
        return true;
    }

    protected void fillIndexInfo(List<DBTableIndex> indexList, String schemaName,
            String tableName) {
        setIndexInfoByDDL(indexList, schemaName, tableName);
    }

    protected void setIndexInfoByDDL(List<DBTableIndex> indexList, String schemaName, String tableName) {
        try {
            MySQLSqlBuilder sb = new MySQLSqlBuilder();
            sb.append("show create table ");
            sb.identifier(schemaName, tableName);
            // Column label May 'Create Table' or 'Create View', use columnIndex here
            List<String> ddl =
                    jdbcOperations.query(sb.toString(), (rs, num) -> rs.getString(2));
            if (CollectionUtils.isEmpty(ddl) || StringUtils.isBlank(ddl.get(0))) {
                fillWarning(indexList, DBObjectType.INDEX, "get index DDL failed");
            } else {
                parseDdlToSetIndexInfo(ddl.get(0), indexList);
            }
        } catch (Exception e) {
            fillWarning(indexList, DBObjectType.INDEX, "query index ddl failed");
            log.warn("Fetch table index through ddl parsing failed", e);
        }
    }

    private void parseDdlToSetIndexInfo(String ddl, List<DBTableIndex> indexList) {
        if (StringUtils.isBlank(ddl)) {
            fillWarning(indexList, DBObjectType.INDEX, "table ddl is blank, can not set index range by parse ddl");
            return;
        }
        ParseSqlResult result = SqlParser.parseMysql(ddl);
        if (CollectionUtils.isEmpty(result.getIndexes())) {
            fillWarning(indexList, DBObjectType.INDEX, "parse index DDL failed");
        } else {
            indexList.forEach(index -> result.getIndexes().forEach(dbIndex -> {
                if (StringUtils.equals(index.getName(), dbIndex.getName())) {
                    index.setGlobal("GLOBAL".equalsIgnoreCase(dbIndex.getRange().name()));
                    index.setColumnGroups(dbIndex.getColumnGroups());
                }
            }));
        }
    }

    protected <T extends DBObjectWarningDescriptor> void fillWarning(List<T> warningDescriptor, DBObjectType type,
            String reason) {
        if (CollectionUtils.isEmpty(warningDescriptor)) {
            return;
        }
        warningDescriptor
                .forEach(descriptor -> {
                    if (StringUtils.isBlank(descriptor.getWarning())) {
                        DBSchemaAccessorUtil.fillWarning(descriptor, type, reason);
                    }
                });
    }

    @Override
    public List<DBObjectIdentity> listSequences(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SHOW SEQUENCES IN ").identifier(schemaName);
        List<String> sequenceNames = jdbcOperations.queryForList(sb.toString(), String.class);
        return sequenceNames.stream().map(name -> DBObjectIdentity.of(schemaName, DBObjectType.SEQUENCE, name)).collect(
                Collectors.toList());
    }

    @Override
    public Map<String, DBTable> getTables(@NonNull String schemaName, List<String> tableNames) {
        // TODO: Only query the table information of tableNames passed upstream
        Map<String, DBTable> returnVal = new HashMap<>();
        tableNames = showTables(schemaName);
        if (tableNames.isEmpty()) {
            return returnVal;
        }
        Map<String, String> tableName2Ddl = new HashMap<>();
        tableNames.stream()
                .forEach(tableName -> tableName2Ddl.put(tableName, getTableDDL(schemaName, tableName)));
        Map<String, List<DBTableColumn>> tableName2Columns = listTableColumns(schemaName, Collections.emptyList());
        Map<String, List<DBTableIndex>> tableName2Indexes = listTableIndexes(schemaName, tableName2Ddl);
        Map<String, List<DBTableConstraint>> tableName2Constraints = listTableConstraints(schemaName);
        Map<String, DBTableOptions> tableName2Options = listTableOptions(schemaName);
        for (String tableName : tableNames) {
            if (!tableName2Columns.containsKey(tableName)) {
                continue;
            }
            DBTable table = new DBTable();
            table.setSchemaName(schemaName);
            table.setOwner(schemaName);
            table.setName(tableName);
            table.setColumns(tableName2Columns.getOrDefault(tableName, new ArrayList<>()));
            table.setIndexes(tableName2Indexes.getOrDefault(tableName, new ArrayList<>()));
            table.setConstraints(tableName2Constraints.getOrDefault(tableName, new ArrayList<>()));
            table.setTableOptions(tableName2Options.getOrDefault(tableName, new DBTableOptions()));
            table.setColumnGroups(listTableColumnGroups(tableName2Ddl.get(tableName)));
            try {
                table.setPartition(getPartition(schemaName, tableName));
            } catch (Exception e) {
                log.warn("Failed to set table partition", e);
            }
            table.setDDL(tableName2Ddl.get(tableName));
            returnVal.put(tableName, table);
        }
        return returnVal;
    }
}
