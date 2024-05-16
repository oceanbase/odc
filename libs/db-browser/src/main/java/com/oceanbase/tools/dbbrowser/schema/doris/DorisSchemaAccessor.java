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
package com.oceanbase.tools.dbbrowser.schema.doris;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;

import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBColumnTypeDisplay;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBSequence;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.DBTableSubpartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.model.DBVariable;
import com.oceanbase.tools.dbbrowser.model.DBView;
import com.oceanbase.tools.dbbrowser.model.MySQLConstants;
import com.oceanbase.tools.dbbrowser.parser.PLParser;
import com.oceanbase.tools.dbbrowser.parser.result.ParseMysqlPLResult;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMapper;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.Statements;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;

import lombok.extern.slf4j.Slf4j;

/**
 * ClassName: DorisSchemaAccessor Package: com.oceanbase.tools.dbbrowser.schema.doris Description:
 *
 * @Author: fenghao
 * @Create 2024/1/9 20:53
 * @Version 1.0
 */
@Slf4j
public class DorisSchemaAccessor implements DBSchemaAccessor {

    protected JdbcOperations jdbcOperations;
    protected DBSchemaAccessorSqlMapper sqlMapper;

    public DorisSchemaAccessor(@NonNull JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.MYSQL_5_7_x);
    }

    @Override
    public List<String> showDatabases() {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SHOW DATABASES");
        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public DBDatabase getDatabase(String schemaName) {
        DBDatabase database = new DBDatabase();
        /**
         * we cannot identify a database under this MySQL version, so we just consider schemaName as its
         * id<br>
         */
        database.setId(schemaName);
        database.setName(schemaName);
        String sql =
                "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.schemata WHERE SCHEMA_NAME ='"
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
        });
    }

    @Override
    public void switchDatabase(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("use ");
        sb.identifier(schemaName);
        jdbcOperations.execute(sb.toString());
    }

    @Override
    public List<DBObjectIdentity> listUsers() {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SELECT DISTINCT user FROM mysql.user");
        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBObjectIdentity dbUser = new DBObjectIdentity();
            dbUser.setName(rs.getString(1));
            dbUser.setType(DBObjectType.USER);
            return dbUser;
        });
    }

    @Override
    public List<String> showTables(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SHOW FULL TABLES ");

        if (StringUtils.isNotBlank(schemaName)) {
            sb.append("FROM ");
            sb.identifier(schemaName);
        }
        List<String> tableNames = new ArrayList<>();
        sb.append(" WHERE table_type='BASE TABLE'");
        try {
            tableNames = jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString(1));
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
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SELECT table_name FROM information_schema.tables WHERE table_type='BASE TABLE'");
        if (StringUtils.isNotBlank(schemaName)) {
            sb.append(" AND table_schema=");
            sb.value(schemaName);
        }
        if (StringUtils.isNotBlank(tableNameLike)) {
            sb.append(" AND table_name LIKE ");
            sb.value(tableNameLike);
        }
        sb.append(" ORDER BY table_name");
        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public List<DBObjectIdentity> listTables(String schemaName, String tableNameLike) {
        List<DBObjectIdentity> results = new ArrayList<>();
        try {
            results.addAll(listBaseTables(schemaName, tableNameLike));
        } catch (Exception e) {
            log.warn("List base tables failed, reason={}", e.getMessage());
        }

        return results;
    }

    protected List<DBObjectIdentity> listBaseTables(String schemaName, String tableNameLike)
            throws DataAccessException {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("select table_schema as schema_name, 'TABLE' as type, table_name as name ");
        sb.append("from information_schema.tables where table_type = 'BASE TABLE'");
        if (StringUtils.isNotBlank(schemaName)) {
            sb.append(" AND table_schema=");
            sb.value(schemaName);
        }
        if (StringUtils.isNotBlank(tableNameLike)) {
            sb.append(" AND table_name LIKE ");
            sb.value(tableNameLike);
        }
        sb.append(" ORDER BY schema_name, table_name");

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public List<DBObjectIdentity> listViews(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("show full tables from ");
        sb.identifier(schemaName);
        sb.append(" where Table_type like '%VIEW%'");
        return jdbcOperations.query(sb.toString(),
                (rs, rowNum) -> DBObjectIdentity.of(schemaName, DBObjectType.VIEW, rs.getString(1)));
    }

    @Override
    public List<DBObjectIdentity> listAllViews(String viewNameLike) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append(
                "select TABLE_SCHEMA as schema_name,TABLE_NAME as name, 'VIEW' as type from information_schema.views "
                        + "where TABLE_NAME LIKE ")
                .value('%' + viewNameLike + '%')
                .append(" order by name asc;");
        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public List<DBObjectIdentity> listAllUserViews() {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SELECT table_schema as schema_name, 'VIEW' as type, table_name as name ");
        sb.append(" FROM information_schema.tables where table_type = 'VIEW'");
        sb.append(" ORDER BY schema_name, name");
        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public List<DBObjectIdentity> listAllSystemViews() {
        List<DBObjectIdentity> results = new ArrayList<>();

        String sql = "show full tables from `information_schema` where Table_type='SYSTEM VIEW'";
        try {
            List<String> informationSchemaViews = jdbcOperations.query(sql, (rs, rowNum) -> rs.getString(1));
            informationSchemaViews
                    .forEach(name -> results.add(DBObjectIdentity.of("information_schema", DBObjectType.VIEW, name)));
        } catch (Exception ex) {
            log.info("List tables for 'information_schema' failed, reason={}", ex.getMessage());
        }

        return results;
    }

    @Override
    public List<String> showSystemViews(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SHOW FULL TABLES ");

        if (StringUtils.isNotBlank(schemaName)) {
            sb.append(" FROM ");
            sb.identifier(schemaName);
        }
        sb.append(" WHERE Table_type = 'SYSTEM VIEW'");

        try {
            return jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString(1));
        } catch (BadSqlGrammarException ex) {
            if (StringUtils.containsIgnoreCase(ex.getMessage(), "Unknown database")) {
                return Collections.emptyList();
            }
            throw ex;
        }
    }

    @Override
    public List<DBVariable> showVariables() {
        String sql = "show variables";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<DBVariable> showSessionVariables() {
        String sql = "show session variables";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<DBVariable> showGlobalVariables() {
        String sql = "show global variables";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<String> showCharset() {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("show collation");

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString(2));
    }

    @Override
    public List<String> showCollation() {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("show collation");

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString(1));
    }

    @Override
    public List<DBPLObjectIdentity> listFunctions(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append(
                "select ROUTINE_NAME as name, ROUTINE_SCHEMA as schema_name, ROUTINE_TYPE as type from `information_schema`.`routines` where ROUTINE_SCHEMA=");
        sb.value(schemaName);
        sb.append(" and ROUTINE_TYPE = 'FUNCTION';");

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBPLObjectIdentity.class));
    }

    @Override
    public List<DBPLObjectIdentity> listProcedures(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append(
                "select ROUTINE_NAME as name, ROUTINE_SCHEMA as schema_name, ROUTINE_TYPE as type from `information_schema`.`routines` where ROUTINE_SCHEMA=");
        sb.value(schemaName);
        sb.append(" and ROUTINE_TYPE = 'PROCEDURE';");

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBPLObjectIdentity.class));
    }

    @Override
    public List<DBPLObjectIdentity> listPackages(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBPLObjectIdentity> listPackageBodies(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBPLObjectIdentity> listTriggers(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBPLObjectIdentity> listTypes(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBObjectIdentity> listSequences(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBObjectIdentity> listSynonyms(String schemaName, DBSynonymType synonymType) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Map<String, List<DBTableColumn>> listTableColumns(String schemaName, List<String> tableNames) {
        List<DBTableColumn> tableColumns = DBSchemaAccessorUtil.partitionFind(tableNames,
                DBSchemaAccessorUtil.OB_MAX_IN_SIZE, names -> {
                    String querySql = filterByValues(getListTableColumnsSql(schemaName), "TABLE_NAME", names);
                    return jdbcOperations.query(querySql, listTableRowMapper());
                });
        return tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicTableColumns(String schemaName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_SCHEMA_TABLE_COLUMNS);
        List<DBTableColumn> tableColumns = jdbcOperations.query(sql, new Object[] {schemaName, schemaName},
                listBasicTableColumnRowMapper());
        return tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public List<DBTableColumn> listBasicTableColumns(String schemaName, String tableName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_TABLE_COLUMNS);
        return jdbcOperations.query(sql, new Object[] {schemaName, tableName}, listBasicTableColumnRowMapper());
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicViewColumns(String schemaName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_SCHEMA_VIEW_COLUMNS);
        List<DBTableColumn> tableColumns = jdbcOperations.query(sql, new Object[] {schemaName, schemaName},
                listBasicTableColumnRowMapper());
        return tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public List<DBTableColumn> listBasicViewColumns(String schemaName, String viewName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_VIEW_COLUMNS);
        return jdbcOperations.query(sql, new Object[] {schemaName, viewName}, listBasicTableColumnRowMapper());
    }

    protected String getListTableColumnsSql(String schemaName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append(
                "select TABLE_NAME, TABLE_SCHEMA, ORDINAL_POSITION, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, NUMERIC_SCALE, "
                        + "NUMERIC_PRECISION, "
                        + "DATETIME_PRECISION, CHARACTER_MAXIMUM_LENGTH, EXTRA, CHARACTER_SET_NAME, "
                        + "COLLATION_NAME, COLUMN_COMMENT, COLUMN_DEFAULT, IS_NULLABLE, GENERATION_EXPRESSION, "
                        + "COLUMN_KEY from information_schema.columns where TABLE_SCHEMA = ");
        sb.value(schemaName);
        sb.append(" ORDER BY TABLE_NAME, ORDINAL_POSITION");
        return sb.toString();
    }

    protected RowMapper<DBTableColumn> listTableRowMapper() {
        return (rs, romNum) -> {
            DBTableColumn tableColumn = new DBTableColumn();
            tableColumn.setSchemaName(rs.getString(MySQLConstants.COL_TABLE_SCHEMA));
            tableColumn.setTableName(rs.getString(MySQLConstants.COL_TABLE_NAME));
            tableColumn.setName(rs.getString(MySQLConstants.COL_COLUMN_NAME));
            tableColumn.setTypeName(rs.getString(MySQLConstants.COL_DATA_TYPE));
            String fullTypeName = rs.getString(MySQLConstants.COL_COLUMN_TYPE);
            tableColumn.setFullTypeName(fullTypeName);
            if (StringUtils.isNotBlank(tableColumn.getTypeName())
                    && (isTypeEnum(tableColumn.getTypeName()) || isTypeSet((tableColumn.getTypeName())))) {
                tableColumn.setEnumValues(DBSchemaAccessorUtil.parseEnumValues(fullTypeName));
            }
            DBColumnTypeDisplay columnTypeDisplay = DBColumnTypeDisplay.fromName(tableColumn.getTypeName());
            if (columnTypeDisplay.displayScale()) {
                tableColumn.setScale(rs.getInt(MySQLConstants.COL_NUMERIC_SCALE));
            }
            if (columnTypeDisplay.displayPrecision()) {
                if (Objects.nonNull(rs.getObject(MySQLConstants.COL_NUMERIC_SCALE))
                        || Objects.nonNull(rs.getObject(MySQLConstants.COL_NUMERIC_PRECISION))) {
                    tableColumn.setPrecision(rs.getLong(MySQLConstants.COL_NUMERIC_PRECISION));
                } else if (Objects.nonNull(rs.getObject(MySQLConstants.COL_DATETIME_SCALE))) {
                    tableColumn.setPrecision(rs.getLong(MySQLConstants.COL_DATETIME_SCALE));
                } else {
                    tableColumn.setPrecision(rs.getLong(MySQLConstants.COL_CHARACTER_MAXIMUM_LENGTH));
                }
            }
            tableColumn.setExtraInfo(rs.getString(MySQLConstants.COL_COLUMN_EXTRA));

            Long maxLength = rs.getLong(MySQLConstants.COL_CHARACTER_MAXIMUM_LENGTH);
            if (Objects.isNull(maxLength)) {
                tableColumn.setMaxLength(rs.getLong(MySQLConstants.COL_NUMERIC_PRECISION));
            } else {
                tableColumn.setMaxLength(maxLength);
            }
            tableColumn.setCharsetName(rs.getString(MySQLConstants.COL_CHARACTER_SET_NAME));
            tableColumn.setCollationName(rs.getString(MySQLConstants.COL_COLLATION_NAME));
            tableColumn.setComment(rs.getString(MySQLConstants.COL_COLUMN_COMMENT));
            tableColumn.fillDefaultValue(rs.getString(MySQLConstants.COL_COLUMN_DEFAULT));
            tableColumn.setNullable("YES".equalsIgnoreCase(rs.getString(MySQLConstants.COL_IS_NULLABLE)));
            tableColumn.setGenExpression(rs.getString(MySQLConstants.COL_COLUMN_GENERATION_EXPRESSION));
            tableColumn.setVirtual(StringUtils.isNotEmpty(tableColumn.getGenExpression()));
            tableColumn.setOrdinalPosition(rs.getInt(MySQLConstants.COL_ORDINAL_POSITION));
            String keyTypeName = rs.getString(MySQLConstants.COL_COLUMN_KEY);
            if (StringUtils.isNotBlank(keyTypeName)) {
                tableColumn.setKeyType(DBTableColumn.KeyType.valueOf(keyTypeName));
            }

            tableColumn.setTypeModifiers(new ArrayList<>());
            tableColumn.setZerofill(false);
            tableColumn.setUnsigned(false);
            for (String modifier : fullTypeName.toLowerCase().split("\\s+")) {
                switch (modifier) {
                    case "zerofill":
                        tableColumn.setZerofill(true);
                        tableColumn.getTypeModifiers().add("zerofill");
                        break;
                    case "unsigned":
                        tableColumn.setUnsigned(true);
                        tableColumn.getTypeModifiers().add("unsigned");
                        break;
                }
            }

            if (Objects.nonNull(tableColumn.getExtraInfo())) {
                tableColumn.setAutoIncrement(
                        tableColumn.getExtraInfo().equalsIgnoreCase(MySQLConstants.EXTRA_AUTO_INCREMENT));
                tableColumn.setOnUpdateCurrentTimestamp(
                        tableColumn.getExtraInfo()
                                .equalsIgnoreCase(MySQLConstants.EXTRA_ON_UPDATE_CURRENT_TIMESTAMP));
                tableColumn.setStored(tableColumn.getExtraInfo()
                        .equalsIgnoreCase(MySQLConstants.EXTRA_STORED_GENERATED));
            }
            return tableColumn;
        };
    }

    protected RowMapper<DBTableColumn> listBasicTableColumnRowMapper() {
        return (rs, romNum) -> {
            DBTableColumn tableColumn = new DBTableColumn();
            tableColumn.setSchemaName(rs.getString(MySQLConstants.COL_TABLE_SCHEMA));
            tableColumn.setTableName(rs.getString(MySQLConstants.COL_TABLE_NAME));
            tableColumn.setName(rs.getString(MySQLConstants.COL_COLUMN_NAME));
            tableColumn.setTypeName(rs.getString(MySQLConstants.COL_DATA_TYPE));
            tableColumn.setComment(rs.getString(MySQLConstants.COL_COLUMN_COMMENT));
            return tableColumn;
        };
    }

    protected boolean isTypeEnum(String typeName) {
        return typeName.equalsIgnoreCase(MySQLConstants.TYPE_NAME_ENUM);
    }

    protected boolean isTypeSet(String typeName) {
        return typeName.equalsIgnoreCase(MySQLConstants.TYPE_NAME_SET);
    }

    @Override
    public Map<String, List<DBTableIndex>> listTableIndexes(String schemaName) {
        String sql = sqlMapper.getSql(Statements.LIST_SCHEMA_INDEX);
        Map<String, DBTableIndex> fullIndexName2Index = new LinkedHashMap<>();
        jdbcOperations.query(sql, new Object[] {schemaName}, (rs, num) -> {
            String tableName = rs.getString("TABLE_NAME");
            String indexName = rs.getString("INDEX_NAME");

            if (!fullIndexName2Index.containsKey(tableName + indexName)) {
                DBTableIndex index = new DBTableIndex();
                index.setSchemaName(rs.getString("TABLE_SCHEMA"));
                index.setTableName(rs.getString("TABLE_NAME"));
                index.setName(indexName);
                index.setOrdinalPosition(rs.getInt("SEQ_IN_INDEX"));
                index.setPrimary(indexName.equalsIgnoreCase("PRIMARY"));
                index.setCardinality(rs.getLong("CARDINALITY"));
                index.setComment(rs.getString("INDEX_COMMENT"));
                index.setAdditionalInfo(rs.getString("COMMENT"));
                index.setNonUnique(rs.getInt("NON_UNIQUE") != 0);
                if (isIndexDistinguishesVisibility()) {
                    String visible = rs.getString("IS_VISIBLE");
                    if (Objects.nonNull(visible)) {
                        index.setVisible(visible.equalsIgnoreCase("YES"));
                    }
                } else {
                    index.setVisible(true);
                }
                index.setCollation(rs.getString("COLLATION"));
                index.setAlgorithm(DBIndexAlgorithm.fromString(rs.getString("INDEX_TYPE")));
                if (index.getAlgorithm() == DBIndexAlgorithm.FULLTEXT) {
                    index.setType(DBIndexType.FULLTEXT);
                } else if (index.getAlgorithm() == DBIndexAlgorithm.RTREE
                        || index.getAlgorithm() == DBIndexAlgorithm.SPATIAL) {
                    index.setType(DBIndexType.SPATIAL);
                } else {
                    if (index.isNonUnique()) {
                        index.setType(DBIndexType.NORMAL);
                    } else {
                        index.setType(DBIndexType.UNIQUE);
                    }
                }
                List<String> columnNames = new ArrayList<>();
                columnNames.add(rs.getString("COLUMN_NAME"));
                index.setColumnNames(columnNames);
                index.setGlobal(true);
                fullIndexName2Index.put(tableName + indexName, index);
            } else {
                fullIndexName2Index.get(tableName + indexName).getColumnNames()
                        .add(rs.getString(MySQLConstants.IDX_COLUMN_NAME));
            }
            return null;
        });

        Map<String, List<DBTableIndex>> tableName2Indexes =
                fullIndexName2Index.values().stream().collect(Collectors.groupingBy(DBTableIndex::getTableName));
        for (List<DBTableIndex> columns : tableName2Indexes.values()) {
            columns.stream().sorted(Comparator.comparing(DBTableIndex::getOrdinalPosition))
                    .collect(Collectors.toList());
        }
        return tableName2Indexes;
    }

    protected boolean isIndexDistinguishesVisibility() {
        return false;
    }

    @Override
    public Map<String, List<DBTableConstraint>> listTableConstraints(String schemaName) {
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        String sql =
                sqlBuilder
                        .append(
                                "select t1.CONSTRAINT_NAME, t1.CONSTRAINT_SCHEMA, t1.TABLE_NAME, t1.COLUMN_NAME, t1"
                                        + ".ORDINAL_POSITION, "
                                        + "t1.REFERENCED_TABLE_SCHEMA, t1.REFERENCED_TABLE_NAME, t1.REFERENCED_COLUMN_NAME, t2"
                                        + ".CONSTRAINT_TYPE from ")
                        .append(MySQLConstants.META_TABLE_KEY_COLUMN_USAGE)
                        .append(" t1 left join ")
                        .append(MySQLConstants.META_TABLE_TABLE_CONSTRAINTS)
                        .append(
                                " t2 on t1.table_name=t2.table_name and t1.table_schema=t2.table_schema and t1.constraint_name=t2"
                                        + ".constraint_name")
                        .append(" where t1.table_schema=")
                        .value(schemaName)
                        .append(" order by t1.CONSTRAINT_NAME, t1.ORDINAL_POSITION asc;")
                        .toString();
        Map<String, DBTableConstraint> fullConstraintName2Constraint = new LinkedHashMap<>();
        jdbcOperations.query(sql, (rs, num) -> {
            String constraintName = rs.getString(MySQLConstants.CONS_NAME);
            String tableName = rs.getString(MySQLConstants.COL_TABLE_NAME);
            if (!fullConstraintName2Constraint.containsKey(tableName + constraintName)) {
                DBTableConstraint constraint = new DBTableConstraint();
                constraint.setName(constraintName);
                List<String> columnNames = new ArrayList<>();
                columnNames.add(rs.getString(MySQLConstants.CONS_COL_NAME));
                constraint.setColumnNames(columnNames);
                constraint.setOrdinalPosition(rs.getInt(MySQLConstants.COL_ORDINAL_POSITION));
                constraint.setOwner(rs.getString(MySQLConstants.CONS_CONSTRAINT_SCHEMA));
                constraint.setSchemaName(schemaName);
                constraint.setTableName(tableName);
                constraint.setReferenceSchemaName(rs.getString(MySQLConstants.CONS_REFERENCED_TABLE_SCHEMA));
                constraint.setReferenceTableName(rs.getString(MySQLConstants.CONS_REFERENCED_TABLE_NAME));
                constraint.setType(DBConstraintType.fromValue(rs.getString(MySQLConstants.CONS_TYPE)));
                List<String> referencedColumnNames = new ArrayList<>();
                referencedColumnNames.add(rs.getString(MySQLConstants.CONS_REFERENCED_COLUMN_NAME));
                constraint.setReferenceColumnNames(referencedColumnNames);

                fullConstraintName2Constraint.put(tableName + constraintName, constraint);
            } else {
                fullConstraintName2Constraint.get(tableName + constraintName).getColumnNames()
                        .add(rs.getString(MySQLConstants.CONS_COL_NAME));
                fullConstraintName2Constraint.get(tableName + constraintName).getReferenceColumnNames()
                        .add(rs.getString(MySQLConstants.CONS_REFERENCED_COLUMN_NAME));
            }

            return constraintName;
        });
        for (DBTableConstraint constraint : fullConstraintName2Constraint.values()) {
            if (Objects.nonNull(constraint.getReferenceColumnNames())) {
                constraint.setReferenceColumnNames(constraint.getReferenceColumnNames().stream()
                        .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
            }
            if (Objects.nonNull(constraint.getColumnNames())) {
                constraint.setColumnNames(constraint.getColumnNames().stream().filter(Objects::nonNull).distinct()
                        .collect(Collectors.toList()));
            }
        }
        return fullConstraintName2Constraint.values().stream()
                .collect(Collectors.groupingBy(DBTableConstraint::getTableName));
    }

    @Override
    public Map<String, DBTable.DBTableOptions> listTableOptions(String schemaName) {
        // 查询 collation 和 charset 的映射关系，加载到内存
        String collationAndCharsetQuery =
                "select COLLATION_NAME, CHARACTER_SET_NAME from information_schema.collation_character_set_applicability";
        Map<String, String> collation2Charset = new HashMap<>();
        jdbcOperations.query(collationAndCharsetQuery, rs -> {
            String collationName = rs.getString("COLLATION_NAME");
            String charsetName = rs.getString("CHARACTER_SET_NAME");
            collation2Charset.putIfAbsent(collationName, charsetName);
        });

        Map<String, DBTable.DBTableOptions> tableName2TableOptions = new LinkedHashMap<>();
        String sql =
                new MySQLSqlBuilder().append(
                        "select `TABLE_NAME`, `CREATE_TIME`, `UPDATE_TIME`, `AUTO_INCREMENT`, `TABLE_COLLATION`, "
                                + "`TABLE_COMMENT` from "
                                + "`information_schema`"
                                + ".`tables`"
                                + " where table_schema=")
                        .value(schemaName)
                        .toString();
        jdbcOperations.query(sql, t -> {
            String tableName = t.getString("TABLE_NAME");
            DBTable.DBTableOptions options = new DBTable.DBTableOptions();
            tableName2TableOptions.putIfAbsent(tableName, options);

            options.setCreateTime(t.getTimestamp("CREATE_TIME"));
            options.setUpdateTime(t.getTimestamp("UPDATE_TIME"));
            options.setAutoIncrementInitialValue(t.getLong("AUTO_INCREMENT"));
            options.setComment(t.getString("TABLE_COMMENT"));
            options.setCollationName(t.getString("TABLE_COLLATION"));
            if (collation2Charset.containsKey(options.getCollationName())) {
                options.setCharsetName(collation2Charset.get(options.getCollationName()));
            }
        });
        return tableName2TableOptions;
    }

    @Override
    public Map<String, DBTablePartition> listTablePartitions(@NonNull String schemaName,
            List<String> tableNames) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBTablePartition> listTableRangePartitionInfo(String tenantName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBTableSubpartitionDefinition> listSubpartitions(String schemaName, String tableName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Boolean isLowerCaseTableName() {
        AtomicBoolean isLowerCaseTableName = new AtomicBoolean(true);
        /**
         * lower_case_table_names 用于设置表名是否对大小写敏感, 默认值为 1，即不区分大小写。 0：表名将按照指定的大小写形式进行存储，并以区分大小写形式进行比较
         * 1：表名将按照小写形式进行存储，并以不区分大小写形式进行比较 2：表名将按照指定的大小写形式进行存储，并以不区分大小写形式进行比较 REF:
         * https://www.oceanbase.com/docs/enterprise-oceanbase-database-cn-10000000000362989
         */
        try {
            jdbcOperations.query(
                    "show variables like 'lower_case_table_names'", t -> {
                        isLowerCaseTableName.set(t.getInt("Value") != 0);
                    });
        } catch (Exception ex) {
            log.warn("get variable lower_case_table_names failed, will use default value, reason=", ex);
        }
        return isLowerCaseTableName.get();
    }

    @Override
    public List<DBTableColumn> listTableColumns(String schemaName, String tableName) {
        String sql = this.sqlMapper.getSql(Statements.LIST_TABLE_COLUMNS);
        return jdbcOperations.query(sql, new Object[] {schemaName, tableName}, listTableRowMapper());
    }

    @Override
    public List<DBObjectIdentity> listPartitionTables(String partitionMethod) {
        String sql =
                String.format("select distinct t1.table_name as name,t1.table_schema as schema_name, 'TABLE' as type "
                        + "from information_schema.tables t1 join information_schema.partitions t2 on "
                        + "t1.table_name = t2.table_name and t1.table_schema = t2.table_schema "
                        + "where t2.partition_method = '%s'", partitionMethod);
        return jdbcOperations.query(sql, new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public List<DBTableConstraint> listTableConstraints(String schemaName, String tableName) {
        String sql = sqlMapper.getSql(Statements.LIST_TABLE_CONSTRAINTS);
        Map<String, DBTableConstraint> name2Constraint = new LinkedHashMap<>();
        jdbcOperations.query(sql, new Object[] {schemaName, tableName}, (rs, num) -> {
            String constraintName = rs.getString(MySQLConstants.CONS_NAME);
            if (!name2Constraint.containsKey(constraintName)) {
                DBTableConstraint constraint = new DBTableConstraint();
                constraint.setName(constraintName);
                List<String> columnNames = new ArrayList<>();
                columnNames.add(rs.getString(MySQLConstants.CONS_COL_NAME));
                constraint.setColumnNames(columnNames);
                constraint.setOrdinalPosition(num);
                constraint.setOwner(rs.getString(MySQLConstants.CONS_CONSTRAINT_SCHEMA));
                constraint.setSchemaName(schemaName);
                constraint.setTableName(tableName);
                constraint.setReferenceSchemaName(rs.getString(MySQLConstants.CONS_REFERENCED_TABLE_SCHEMA));
                constraint.setReferenceTableName(rs.getString(MySQLConstants.CONS_REFERENCED_TABLE_NAME));
                constraint.setType(DBConstraintType.fromValue(rs.getString(MySQLConstants.CONS_TYPE)));
                List<String> referencedColumnNames = new ArrayList<>();
                referencedColumnNames.add(rs.getString(MySQLConstants.CONS_REFERENCED_COLUMN_NAME));
                constraint.setReferenceColumnNames(referencedColumnNames);

                name2Constraint.put(constraintName, constraint);
            } else {
                name2Constraint.get(constraintName).getColumnNames()
                        .add(rs.getString(MySQLConstants.CONS_COL_NAME));
                name2Constraint.get(constraintName).getReferenceColumnNames()
                        .add(rs.getString(MySQLConstants.CONS_REFERENCED_COLUMN_NAME));
            }

            return constraintName;
        });
        for (DBTableConstraint constraint : name2Constraint.values()) {
            if (Objects.nonNull(constraint.getReferenceColumnNames())) {
                constraint.setReferenceColumnNames(constraint.getReferenceColumnNames().stream()
                        .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
            }
            if (Objects.nonNull(constraint.getColumnNames())) {
                constraint.setColumnNames(constraint.getColumnNames().stream().filter(Objects::nonNull).distinct()
                        .collect(Collectors.toList()));

            }
        }
        return new ArrayList<>(name2Constraint.values());
    }

    @Override
    public DBTablePartition getPartition(String schemaName, String tableName) {
        DBTablePartition partition = new DBTablePartition();
        DBTablePartition subPartition = new DBTablePartition();
        partition.setSubpartition(subPartition);

        DBTablePartitionOption partitionOption = new DBTablePartitionOption();
        partitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        partition.setPartitionOption(partitionOption);

        DBTablePartitionOption subPartitionOption = new DBTablePartitionOption();
        subPartitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        subPartition.setPartitionOption(subPartitionOption);

        List<DBTablePartitionDefinition> partitionDefinitions = new ArrayList<>();
        partition.setPartitionDefinitions(partitionDefinitions);
        List<DBTablePartitionDefinition> subPartitionDefinitions = new ArrayList<>();
        subPartition.setPartitionDefinitions(subPartitionDefinitions);

        Set<String> partitionNames = new HashSet<>();
        String sql = sqlMapper.getSql(Statements.GET_PARTITION);
        jdbcOperations.query(sql, new Object[] {schemaName, tableName}, rs -> {
            partitionOption.setType(DBTablePartitionType.fromValue(rs.getString("PARTITION_METHOD")));
            String expression = rs.getString("PARTITION_EXPRESSION");
            if (StringUtils.isNotEmpty(expression)) {
                if (partitionOption.getType().supportExpression()) {
                    partitionOption.setExpression(expression);
                } else {
                    partitionOption.setColumnNames(Arrays.asList(expression.split(",")));
                }
            }
            String partitionName = rs.getString("PARTITION_NAME");
            if (StringUtils.isNotEmpty(partitionName) && !partitionNames.contains(partitionName)) {
                partitionNames.add(partitionName);
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setName(partitionName);
                partitionDefinition.setOrdinalPosition(rs.getInt("PARTITION_ORDINAL_POSITION"));
                partitionDefinition.setType(DBTablePartitionType.fromValue(rs.getString("PARTITION_METHOD")));
                String description = rs.getString("PARTITION_DESCRIPTION");
                partitionDefinition.fillValues(description);
                partitionDefinitions.add(partitionDefinition);
            }
            String subPartitionName = rs.getString("SUBPARTITION_NAME");
            DBTablePartitionType subPartitionType = DBTablePartitionType.fromValue(rs.getString("SUBPARTITION_METHOD"));
            String subPartExpression = rs.getString("SUBPARTITION_EXPRESSION");

            // 二级分区
            if (StringUtils.isNotEmpty(subPartitionName)) {
                // TODO 目前只支持二级模板化 HASH/KEY 分区，后续需要全部支持
                if (subPartitionType == DBTablePartitionType.HASH || subPartitionType == DBTablePartitionType.KEY) {
                    partition.setSubpartitionTemplated(true);
                    subPartitionOption.setType(subPartitionType);
                    subPartitionOption.setPartitionsNum(rs.getInt("SUB_NUM"));
                    if (StringUtils.isNotEmpty(subPartExpression)) {
                        if (subPartitionType.supportExpression()) {
                            subPartitionOption.setExpression(subPartExpression);
                        } else {
                            subPartitionOption.setColumnNames(Arrays.asList(subPartExpression.split(",")));
                        }
                    }
                } else {
                    partition.setWarning("Only support HASH/KEY subpartition currently, please check comparing ddl");
                }
            }
        });

        partitionOption.setPartitionsNum(partitionNames.size());
        // OB 字典表不兼容的 bug，即使是非分区表，PARTITION_METHOD 也会是 HASH
        // 这里判断下如果是 HASH 分区，且分区数为 0 的话，认为是非分区表
        if (partitionOption.getType() == DBTablePartitionType.HASH && partitionOption.getPartitionsNum() == 0) {
            partitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
            partition.setPartitionDefinitions(Collections.emptyList());
        }
        return partition;
    }

    @Override
    public List<DBTableIndex> listTableIndexes(String schemaName, String tableName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("show index from ");
        sb.identifier(tableName);
        sb.append(" from ");
        sb.identifier(schemaName);

        Map<String, DBTableIndex> indexName2Index = new LinkedHashMap<>();
        jdbcOperations.query(sb.toString(), (rs, num) -> {
            String indexName = rs.getString(MySQLConstants.IDX_NAME);
            if (!indexName2Index.containsKey(indexName)) {
                DBTableIndex index = new DBTableIndex();
                index.setTableName(rs.getString(MySQLConstants.IDX_TABLE_NAME));
                index.setSchemaName(schemaName);
                index.setName(indexName);
                index.setOrdinalPosition(num);
                index.setPrimary(indexName.equalsIgnoreCase(MySQLConstants.IDX_PRIMARY_KEY));
                index.setCardinality(rs.getLong(MySQLConstants.IDX_CARDINALITY));
                index.setComment(rs.getString(MySQLConstants.IDX_COMMENT));
                index.setAdditionalInfo(rs.getString(MySQLConstants.IDX_COL_COMMENT));
                index.setNonUnique(rs.getInt(MySQLConstants.IDX_COL_NON_UNIQUE) != 0);
                if (isIndexDistinguishesVisibility()) {
                    String visible = rs.getString(MySQLConstants.IDX_VISIBLE);
                    if (Objects.nonNull(visible)) {
                        index.setVisible(visible.equalsIgnoreCase("YES"));
                    }
                } else {
                    index.setVisible(true);
                }
                index.setCollation(rs.getString(MySQLConstants.IDX_COLLATION));
                index.setAlgorithm(DBIndexAlgorithm.fromString(rs.getString(MySQLConstants.IDX_TYPE)));
                if (index.getAlgorithm() == DBIndexAlgorithm.FULLTEXT) {
                    index.setType(DBIndexType.FULLTEXT);
                } else if (index.getAlgorithm() == DBIndexAlgorithm.RTREE
                        || index.getAlgorithm() == DBIndexAlgorithm.SPATIAL) {
                    index.setType(DBIndexType.SPATIAL);
                } else {
                    if (index.isNonUnique()) {
                        index.setType(DBIndexType.NORMAL);
                    } else {
                        index.setType(DBIndexType.UNIQUE);
                    }
                }
                List<String> columnNames = new ArrayList<>();
                columnNames.add(rs.getString(MySQLConstants.IDX_COLUMN_NAME));
                index.setColumnNames(columnNames);
                index.setGlobal(true);
                handleIndexAvailability(index, rs.getString(MySQLConstants.IDX_COL_COMMENT));
                indexName2Index.put(indexName, index);
            } else {
                indexName2Index.get(indexName).getColumnNames().add(rs.getString(MySQLConstants.IDX_COLUMN_NAME));
            }
            return null;
        });
        return new ArrayList<>(indexName2Index.values());
    }

    protected void handleIndexAvailability(DBTableIndex index, String availability) {
        if (StringUtils.isBlank(availability)) {
            index.setAvailable(true);
        } else if ("disabled".equals(availability)) {
            index.setAvailable(false);
        }
    }

    @Override
    public String getTableDDL(String schemaName, String tableName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("SHOW CREATE TABLE ");
        sb.identifier(schemaName);
        sb.append(".");
        sb.identifier(tableName);

        AtomicReference<String> ddl = new AtomicReference<>();
        jdbcOperations.query(sb.toString(), rs -> {
            ddl.set(rs.getString(2));
        });
        return ddl.get();
    }

    @Override
    public DBTable.DBTableOptions getTableOptions(String schemaName, String tableName) {
        return getTableOptions(schemaName, tableName, getTableDDL(schemaName, tableName));
    }

    @Override
    public DBTable.DBTableOptions getTableOptions(String schemaName, String tableName, @lombok.NonNull String ddl) {
        DBTable.DBTableOptions dbTableOptions = new DBTable.DBTableOptions();
        obtainOptionsByQuery(schemaName, tableName, dbTableOptions);
        try {
            obtainOptionsByParser(dbTableOptions, ddl);
        } catch (Exception e) {
            log.warn("Failed to get table options by parse table ddl, message={}", e.getMessage());
        }
        return dbTableOptions;
    }

    @Override
    public List<DBColumnGroupElement> listTableColumnGroups(String schemaName, String tableName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    private void obtainOptionsByQuery(String schemaName, String tableName, DBTable.DBTableOptions dbTableOptions) {
        String sql = this.sqlMapper.getSql(Statements.GET_TABLE_OPTION);
        jdbcOperations.query(sql, new Object[] {schemaName, tableName}, t -> {
            dbTableOptions.setCreateTime(t.getTimestamp("CREATE_TIME"));
            dbTableOptions.setUpdateTime(t.getTimestamp("UPDATE_TIME"));
            dbTableOptions.setAutoIncrementInitialValue(t.getLong("AUTO_INCREMENT"));
            dbTableOptions.setCollationName(t.getString("TABLE_COLLATION"));
            dbTableOptions.setComment(t.getString("TABLE_COMMENT"));
        });
    }

    private void obtainOptionsByParser(DBTable.DBTableOptions dbTableOptions, String ddl) {
        SQLParser sqlParser = new OBMySQLParser();
        CreateTable stmt = (CreateTable) sqlParser.parse(new StringReader(ddl));
        TableOptions options = stmt.getTableOptions();
        if (Objects.nonNull(options)) {
            dbTableOptions.setCharsetName(options.getCharset());
            dbTableOptions.setRowFormat(options.getRowFormat());
            dbTableOptions.setCompressionOption(options.getCompression());
            dbTableOptions.setReplicaNum(options.getReplicaNum());
            dbTableOptions.setBlockSize(options.getBlockSize());
            dbTableOptions.setUseBloomFilter(options.getUseBloomFilter());
            dbTableOptions
                    .setTabletSize(
                            Objects.nonNull(options.getTabletSize()) ? options.getTabletSize().longValue() : null);
        }
    }

    @Override
    public DBView getView(String schemaName, String viewName) {
        MySQLSqlBuilder sb = new MySQLSqlBuilder();
        sb.append("select * from information_schema.views where table_schema=");
        sb.value(schemaName);
        sb.append(" and table_name=");
        sb.value(viewName);

        DBView view = new DBView();
        view.setViewName(viewName);
        view.setSchemaName(schemaName);
        jdbcOperations.query(sb.toString(), (rs) -> {
            view.setCheckOption(rs.getString(5));
            view.setUpdatable("YES".equalsIgnoreCase(rs.getString(6)));
            view.setDefiner(rs.getString(7));
        });
        MySQLSqlBuilder getDDL = new MySQLSqlBuilder();
        getDDL.append("show create table ");
        getDDL.identifier(schemaName);
        getDDL.append(".");
        getDDL.identifier(viewName);
        jdbcOperations.query(getDDL.toString(), (rs) -> {
            view.setDdl(rs.getString(2));
        });

        return fillColumnInfoByDesc(view);
    }

    protected DBView fillColumnInfoByDesc(DBView view) {
        try {
            MySQLSqlBuilder sb = new MySQLSqlBuilder();
            sb.append("desc ");
            if (StringUtils.isNotBlank(view.getSchemaName())) {
                sb.identifier(view.getSchemaName()).append(".");
            }
            sb.identifier(view.getViewName());

            List<DBTableColumn> columns = jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
                DBTableColumn column = new DBTableColumn();
                column.setName(rs.getString(1));
                column.setTypeName(rs.getString(2));
                column.setNullable("YES".equalsIgnoreCase(rs.getString(3)));
                column.setDefaultValue(rs.getString(5));
                column.setOrdinalPosition(rowNum);
                column.setTableName(view.getViewName());
                return column;
            });
            view.setColumns(columns);
        } catch (Exception e) {
            log.warn("fail to get view column info, message={}", e.getMessage());
        }
        return view;
    }

    @Override
    public DBFunction getFunction(String schemaName, String functionName) {
        MySQLSqlBuilder sql1 = new MySQLSqlBuilder();
        sql1.append(
                "select DEFINER, CREATED, LAST_ALTERED, ROUTINE_DEFINITION from `information_schema`.`routines` where ROUTINE_SCHEMA=")
                .value(schemaName)
                .append(" and ROUTINE_TYPE = 'FUNCTION' and ROUTINE_NAME=")
                .value(functionName);

        MySQLSqlBuilder queryForParameters = new MySQLSqlBuilder();
        queryForParameters.append(
                "select PARAMETER_MODE, PARAMETER_NAME, DTD_IDENTIFIER from `information_schema`.`parameters` where SPECIFIC_SCHEMA=")
                .value(schemaName)
                .append(" and SPECIFIC_NAME=")
                .value(functionName)
                .append(" and ROUTINE_TYPE='FUNCTION'");
        MySQLSqlBuilder parameters = new MySQLSqlBuilder();
        DBFunction function = new DBFunction();
        function.setFunName(functionName);
        jdbcOperations.query(queryForParameters.toString(), (rs) -> {
            if ("NULL".equals(rs.getString("PARAMETER_MODE")) || Objects.isNull(rs.getString("PARAMETER_MODE"))) {
                function.setReturnType(rs.getString("DTD_IDENTIFIER"));
            } else {
                parameters.identifier(rs.getString("PARAMETER_NAME")).space()
                        .append(rs.getString("DTD_IDENTIFIER")).append(",");
            }
        });
        jdbcOperations.query(sql1.toString(), (rs) -> {
            function.setDefiner(rs.getString("DEFINER"));
            function.setCreateTime(Timestamp.valueOf(rs.getString("CREATED")));
            function.setModifyTime(Timestamp.valueOf(rs.getString("LAST_ALTERED")));
            function.setDdl(String.format("create function %s (%s) returns %s %s;",
                    StringUtils.quoteMysqlIdentifier(function.getFunName()),
                    StringUtils.substring(parameters.toString(), 0, parameters.length() - 1),
                    function.getReturnType(),
                    rs.getString("ROUTINE_DEFINITION")));
        });
        return parseFunctionDDL(function);
    }

    protected String convertBlobToString(Blob data, String collation) {
        Charset charset = null;
        try {
            charset = Charset.forName(collation.split("_")[0]);
        } catch (UnsupportedCharsetException exception) {
            log.warn("Unsupported Charset, message={}", exception.getMessage());
        }
        final StringWriter writer = new StringWriter();
        try (Reader reader = new InputStreamReader(data.getBinaryStream(),
                (charset == null ? StandardCharsets.UTF_8 : charset));
                BufferedReader br = new BufferedReader(reader)) {
            String content;
            while ((content = br.readLine()) != null) {
                writer.write(content + "\n");
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Could not convert BLOB to string, message={}", e);
        }
        return writer.toString().substring(0, writer.toString().length() - 1);
    }

    protected DBFunction parseFunctionDDL(DBFunction function) {
        try {
            ParseMysqlPLResult result = PLParser.parseObMysql(function.getDdl());
            function.setVariables(result.getVaribaleList());
            function.setParams(result.getParamList());
        } catch (Exception e) {
            log.warn("Failed to parse function ddl={}, errorMessage={}", function.getDdl(), e.getMessage());
            function.setParseErrorMessage(e.getMessage());
        }

        return function;
    }

    @Override
    public DBProcedure getProcedure(String schemaName, String procedureName) {
        MySQLSqlBuilder sql1 = new MySQLSqlBuilder();
        sql1.append(
                "select DEFINER, CREATED, LAST_ALTERED, ROUTINE_DEFINITION from `information_schema`.`routines` where ROUTINE_SCHEMA=")
                .value(schemaName)
                .append(" and ROUTINE_TYPE = 'PROCEDURE' and ROUTINE_NAME=")
                .value(procedureName);

        MySQLSqlBuilder queryForParameters = new MySQLSqlBuilder();
        queryForParameters.append(
                "select PARAMETER_MODE, PARAMETER_NAME, DTD_IDENTIFIER from `information_schema`.`parameters` where SPECIFIC_SCHEMA=")
                .value(schemaName)
                .append(" and SPECIFIC_NAME=")
                .value(procedureName)
                .append(" and ROUTINE_TYPE='PROCEDURE'");
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(procedureName);
        MySQLSqlBuilder parameters = new MySQLSqlBuilder();

        jdbcOperations.query(queryForParameters.toString(), (rs) -> {
            parameters.append(rs.getString("PARAMETER_MODE")).space()
                    .identifier(rs.getString("PARAMETER_NAME")).space()
                    .append(rs.getString("DTD_IDENTIFIER")).append(",");
        });
        jdbcOperations.query(sql1.toString(), (rs) -> {
            procedure.setDefiner(rs.getString("DEFINER"));
            procedure.setCreateTime(Timestamp.valueOf(rs.getString("CREATED")));
            procedure.setModifyTime(Timestamp.valueOf(rs.getString("LAST_ALTERED")));
            procedure.setDdl(String.format("create procedure %s (%s) %s;",
                    StringUtils.quoteMysqlIdentifier(procedure.getProName()),
                    StringUtils.substring(parameters.toString(), 0, parameters.length() - 1),
                    rs.getString("ROUTINE_DEFINITION")));
        });
        return parseProcedureDDL(procedure);
    }

    protected DBProcedure parseProcedureDDL(DBProcedure procedure) {
        Validate.notBlank(procedure.getDdl(), "procedure.ddl");
        try {
            ParseMysqlPLResult result = PLParser.parseObMysql(procedure.getDdl());
            procedure.setParams(result.getParamList());
            procedure.setVariables(result.getVaribaleList());
            procedure.setTypes((result.getTypeList()));
        } catch (Exception e) {
            log.warn("Failed to parse, ddl={}, errorMessage={}", procedure.getDdl(), e.getMessage());
            procedure.setParseErrorMessage(e.getMessage());
            return procedure;
        }

        return procedure;
    }

    @Override
    public DBPackage getPackage(String schemaName, String packageName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBTrigger getTrigger(String schemaName, String packageName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBType getType(String schemaName, String typeName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBSequence getSequence(String schemaName, String sequenceName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBSynonym getSynonym(String schemaName, String synonymName, DBSynonymType synonymType) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    private String filterByValues(String target, String colName, List<String> candidates) {
        if (CollectionUtils.isEmpty(candidates)) {
            return target;
        }
        String tables = candidates.stream().map(s -> new MySQLSqlBuilder().value(s).toString())
                .collect(Collectors.joining(","));
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        return sqlBuilder.append("select * from (")
                .append(target).append(") dbbrowser").append(" WHERE dbbrowser.").identifier(colName)
                .append(" in (").append(tables).append(")").toString();
    }

    @Override
    public Map<String, DBTable> getTables(String schemaName, List<String> tableNames) {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
