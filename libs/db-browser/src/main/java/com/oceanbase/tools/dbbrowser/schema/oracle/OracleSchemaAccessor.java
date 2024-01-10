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
package com.oceanbase.tools.dbbrowser.schema.oracle;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;

import com.oceanbase.tools.dbbrowser.model.DBBasicPLObject;
import com.oceanbase.tools.dbbrowser.model.DBColumnTypeDisplay;
import com.oceanbase.tools.dbbrowser.model.DBConstraintDeferability;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBDatabase;
import com.oceanbase.tools.dbbrowser.model.DBForeignKeyModifyRule;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBPackageBasicInfo;
import com.oceanbase.tools.dbbrowser.model.DBPackageDetail;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBSequence;
import com.oceanbase.tools.dbbrowser.model.DBSynonym;
import com.oceanbase.tools.dbbrowser.model.DBSynonymType;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn.CharUnit;
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
import com.oceanbase.tools.dbbrowser.model.OracleConstants;
import com.oceanbase.tools.dbbrowser.model.PLConstants;
import com.oceanbase.tools.dbbrowser.parser.PLParser;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMapper;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.Statements;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.OracleDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.PLObjectErrMsgUtils;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 */
@Slf4j
public class OracleSchemaAccessor implements DBSchemaAccessor {
    private static final String ORACLE_TABLE_COMMENT_DDL_TEMPLATE =
            "COMMENT ON TABLE ${schemaName}.${tableName} IS ${comment}";
    private static final String ORACLE_COLUMN_COMMENT_DDL_TEMPLATE =
            "COMMENT ON COLUMN ${schemaName}.${tableName}.${columnName} IS ${comment}";
    protected OracleDataDictTableNames dataDictTableNames;
    protected JdbcOperations jdbcOperations;
    protected DBSchemaAccessorSqlMapper sqlMapper;

    public OracleSchemaAccessor(@NonNull JdbcOperations jdbcOperations,
            OracleDataDictTableNames dataDictTableNames) {
        this.dataDictTableNames = dataDictTableNames;
        this.jdbcOperations = jdbcOperations;
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.ORACLE_11_g);
    }

    @Override
    public List<String> showDatabases() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select USERNAME from ");
        sb.append(dataDictTableNames.USERS());

        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public DBDatabase getDatabase(String schemaName) {
        DBDatabase database = new DBDatabase();
        String sql = this.sqlMapper.getSql(Statements.GET_DATABASE);
        jdbcOperations.query(sql, new Object[] {schemaName}, rs -> {
            database.setId(rs.getString(2));
            database.setName(rs.getString(1));
        });
        sql = "select value from v$nls_parameters where PARAMETER = 'NLS_CHARACTERSET'";
        jdbcOperations.query(sql, rs -> {
            database.setCharset(rs.getString(1));
        });
        sql = "SELECT value from v$nls_parameters where parameter = 'NLS_SORT'";
        jdbcOperations.query(sql, rs -> {
            database.setCollation(rs.getString(1));
        });
        return database;
    }

    @Override
    public List<DBDatabase> listDatabases() {
        List<DBDatabase> databases = new ArrayList();
        String sql = this.sqlMapper.getSql(Statements.LIST_DATABASE);
        this.jdbcOperations.query(sql, (rs) -> {
            DBDatabase database = new DBDatabase();
            database.setId(rs.getString(2));
            database.setName(rs.getString(1));
            databases.add(database);
        });
        sql = "select value from v$nls_parameters where PARAMETER = 'NLS_CHARACTERSET'";
        AtomicReference<String> charset = new AtomicReference();
        this.jdbcOperations.query(sql, (rs) -> {
            charset.set(rs.getString(1));
        });
        sql = "SELECT value from v$nls_parameters where parameter = 'NLS_SORT'";
        AtomicReference<String> collation = new AtomicReference();
        this.jdbcOperations.query(sql, (rs) -> {
            collation.set(rs.getString(1));
        });
        databases.forEach((item) -> {
            item.setCharset(charset.get());
            item.setCollation(collation.get());
        });
        return databases;
    }

    @Override
    public void switchDatabase(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("alter session set current_schema=");
        sb.identifier(schemaName);
        jdbcOperations.execute(sb.toString());
    }

    @Override
    public List<DBObjectIdentity> listUsers() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SELECT USERNAME FROM ");
        sb.append(dataDictTableNames.USERS());
        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBObjectIdentity dbUser = new DBObjectIdentity();
            dbUser.setName(rs.getString(1));
            dbUser.setType(DBObjectType.USER);
            return dbUser;
        });
    }

    @Override
    public List<String> showTablesLike(String schemaName, String tableNameLike) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SELECT TABLE_NAME FROM ");
        sb.append(dataDictTableNames.TABLES());
        sb.append(" WHERE OWNER=");
        sb.value(schemaName);
        if (StringUtils.isNotBlank(tableNameLike)) {
            sb.append(" AND TABLE_NAME LIKE ");
            sb.value(tableNameLike);
        }
        sb.append(" ORDER BY TABLE_NAME ASC");
        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public List<DBObjectIdentity> listTables(String schemaName, String tableNameLike) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select OWNER as schema_name, 'TABLE' as type,TABLE_NAME as name");
        sb.append(" from ");
        sb.append(dataDictTableNames.TABLES());
        sb.append(" where 1=1 ");

        if (StringUtils.isNotBlank(schemaName)) {
            sb.append(" AND OWNER=");
            sb.value(schemaName);
        }
        if (StringUtils.isNotBlank(tableNameLike)) {
            sb.append(" AND TABLE_NAME LIKE ");
            sb.value(tableNameLike);
        }
        sb.append(" ORDER BY schema_name, type, name");
        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public List<DBObjectIdentity> listViews(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select OWNER as schema_name, 'VIEW' as type, view_name as name from ");
        sb.append(dataDictTableNames.VIEWS());
        sb.append(" where owner=");
        sb.value(schemaName);
        sb.append(" order by view_name asc");
        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public List<DBObjectIdentity> listAllViews(String viewNameLike) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select OWNER as schema_name, VIEW_NAME as name, 'VIEW' as type from ")
                .append(dataDictTableNames.VIEWS())
                .append(" where VIEW_NAME LIKE ")
                .value('%' + viewNameLike + '%')
                .append("  order by name asc");
        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public List<DBObjectIdentity> listAllUserViews() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select OWNER as schema_name, 'VIEW' as type, VIEW_NAME as name");
        sb.append(" from ");
        sb.append(dataDictTableNames.VIEWS());
        sb.append(" ORDER BY schema_name, type, name");
        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public List<DBObjectIdentity> listAllSystemViews() {
        return Collections.emptyList();
    }

    @Override
    public List<String> showSystemViews(String schemaName) {
        if (!StringUtils.equalsIgnoreCase("SYS", schemaName)) {
            return Collections.emptyList();
        }
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select VIEW_NAME from ")
                .append(dataDictTableNames.VIEWS())
                .append(" where OWNER='SYS' ORDER BY VIEW_NAME");
        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public List<DBVariable> showVariables() {
        String sql = "SELECT name, value FROM V$PARAMETER";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<DBVariable> showSessionVariables() {
        String sql = "SELECT name, value FROM V$PARAMETER";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<DBVariable> showGlobalVariables() {
        String sql = "SELECT name, value FROM V$SYSTEM_PARAMETER";

        return jdbcOperations.query(sql, (rs, rowNum) -> {
            DBVariable variable = new DBVariable();
            variable.setName(rs.getString(1));
            variable.setValue(rs.getString(2));
            return variable;
        });
    }

    @Override
    public List<String> showCharset() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SELECT DISTINCT VALUE FROM V$NLS_VALID_VALUES WHERE PARAMETER = 'CHARACTERSET' ORDER BY VALUE");

        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public List<String> showCollation() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SELECT DISTINCT VALUE FROM V$NLS_VALID_VALUES WHERE PARAMETER = 'SORT' ORDER BY VALUE");

        return jdbcOperations.queryForList(sb.toString(), String.class);
    }

    @Override
    public List<DBPLObjectIdentity> listFunctions(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select OWNER as schema_name, object_type as type, OBJECT_NAME as name, STATUS from ")
                .append(dataDictTableNames.OBJECTS())
                .append(" where object_type = 'FUNCTION' and owner=")
                .value(schemaName)
                .append(" order by object_name asc");

        List<DBPLObjectIdentity> functions =
                jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBPLObjectIdentity.class));

        Map<String, String> errorText = PLObjectErrMsgUtils.acquireErrorMessage(jdbcOperations,
                schemaName, DBObjectType.FUNCTION.name(), null);
        for (DBPLObjectIdentity function : functions) {
            if (StringUtils.containsIgnoreCase(function.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
                function.setErrorMessage(errorText.get(function.getName()));
            }
        }

        return functions;
    }

    @Override
    public List<DBPLObjectIdentity> listProcedures(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select object_name as name, object_type as type, owner as schema_name, status from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where object_type = 'PROCEDURE' and owner=");
        sb.value(schemaName);
        sb.append(" order by object_name asc");

        List<DBPLObjectIdentity> procedures =
                jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBPLObjectIdentity.class));

        Map<String, String> errorText = PLObjectErrMsgUtils.acquireErrorMessage(jdbcOperations,
                schemaName, DBObjectType.PROCEDURE.name(), null);
        for (DBPLObjectIdentity procedure : procedures) {
            if (StringUtils.containsIgnoreCase(procedure.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
                procedure.setErrorMessage(errorText.get(procedure.getName()));
            }
        }

        return procedures;
    }

    @Override
    public List<DBPLObjectIdentity> listPackages(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select object_name as name, object_type as type, owner, status from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where (object_type = 'PACKAGE' or object_type = 'PACKAGE BODY') and owner=");
        sb.value(schemaName);
        sb.append(" order by name asc");

        List<DBPLObjectIdentity> packages = jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBPLObjectIdentity dbPackage = new DBPLObjectIdentity();
            dbPackage.setName(rs.getString("name"));
            dbPackage.setStatus(rs.getString("status"));
            dbPackage.setSchemaName(rs.getString("owner"));
            dbPackage.setType(DBObjectType.getEnumByName(rs.getString("type")));
            return dbPackage;
        });

        List<DBPLObjectIdentity> filtered = new ArrayList<>();
        Map<String, String> name2Status = new HashMap<>();
        for (DBPLObjectIdentity dbPackage : packages) {
            String pkgName = dbPackage.getName();
            String status = dbPackage.getStatus();
            // merge status of 'package' and 'package body'
            if (name2Status.containsKey(pkgName)) {
                if (PLConstants.PL_OBJECT_STATUS_INVALID.equalsIgnoreCase(status)) {
                    name2Status.put(pkgName, status);
                }
            } else {
                name2Status.put(pkgName, status);
            }
        }
        Map<String, String> errorText = PLObjectErrMsgUtils.acquireErrorMessage(jdbcOperations,
                schemaName, DBObjectType.PACKAGE.name(), null);
        String pkgName = null;
        for (DBPLObjectIdentity pkg : packages) {
            if (Objects.isNull(pkgName) || !StringUtils.equals(pkgName, pkg.getName())) {
                pkgName = pkg.getName();
                DBPLObjectIdentity dbPackage = new DBPLObjectIdentity();
                dbPackage.setName(pkg.getName());
                dbPackage.setStatus(name2Status.get(pkg.getName()));
                dbPackage.setSchemaName(pkg.getSchemaName());
                dbPackage.setType(pkg.getType());
                if (StringUtils.containsIgnoreCase(dbPackage.getStatus(),
                        PLConstants.PL_OBJECT_STATUS_INVALID)) {
                    dbPackage.setErrorMessage(errorText.get(dbPackage.getName()));
                }
                filtered.add(dbPackage);
            }
        }
        return filtered;
    }

    @Override
    public List<DBPLObjectIdentity> listPackageBodies(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select object_name as name, object_type as type, owner, status from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where object_type = 'PACKAGE BODY' and owner=");
        sb.value(schemaName);
        sb.append(" order by name asc");

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBPLObjectIdentity dbPackage = new DBPLObjectIdentity();
            dbPackage.setName(rs.getString("name"));
            dbPackage.setStatus(rs.getString("status"));
            dbPackage.setSchemaName(rs.getString("owner"));
            dbPackage.setType(DBObjectType.getEnumByName(rs.getString("type")));
            return dbPackage;
        });
    }

    @Override
    public List<DBPLObjectIdentity> listTriggers(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select o.OWNER,s.STATUS,o.STATUS as ENABLE_STATUS,TRIGGER_NAME from "
                + "(select * from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where OBJECT_TYPE='TRIGGER') s right join ");
        sb.append(dataDictTableNames.TRIGGERS());
        sb.append(" o on s.OBJECT_NAME=o.TRIGGER_NAME and s.OWNER=o.OWNER where o.OWNER=");
        sb.value(schemaName);
        sb.append(" order by TRIGGER_NAME asc");

        List<DBPLObjectIdentity> triggers = jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBPLObjectIdentity trigger = new DBPLObjectIdentity();
            trigger.setName(rs.getString("TRIGGER_NAME"));
            trigger.setSchemaName(rs.getString("OWNER"));
            trigger.setStatus(rs.getString("STATUS"));
            trigger.setEnable("ENABLED".equals(rs.getString("ENABLE_STATUS")));
            trigger.setType(DBObjectType.TRIGGER);
            return trigger;
        });

        Map<String, String> errorText = PLObjectErrMsgUtils.acquireErrorMessage(jdbcOperations,
                schemaName, DBObjectType.TRIGGER.name(), null);
        for (DBPLObjectIdentity trigger : triggers) {
            if (StringUtils.containsIgnoreCase(trigger.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
                trigger.setErrorMessage(errorText.get(trigger.getName()));
            }
        }

        return triggers;
    }

    @Override
    public List<DBPLObjectIdentity> listTypes(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select OBJECT_NAME as name, STATUS, OBJECT_TYPE as type, OWNER as schema_name from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where OBJECT_TYPE='TYPE' and OWNER=");
        sb.value(schemaName);
        sb.append(" order by OBJECT_NAME asc");

        List<DBPLObjectIdentity> types =
                jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBPLObjectIdentity.class));

        return fillTypeErrorMessage(types, schemaName);
    }

    protected List<DBPLObjectIdentity> fillTypeErrorMessage(List<DBPLObjectIdentity> types, String schemaName) {
        Map<String, String> errorText = PLObjectErrMsgUtils.acquireErrorMessage(jdbcOperations,
                schemaName, DBObjectType.TYPE.name(), null);
        for (DBPLObjectIdentity type : types) {
            if (StringUtils.containsIgnoreCase(type.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
                type.setErrorMessage(errorText.get(type.getName()));
            }
        }

        return types;
    }

    @Override
    public List<DBObjectIdentity> listSequences(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select SEQUENCE_NAME as name, SEQUENCE_OWNER as schema_name from ");
        sb.append(dataDictTableNames.SEQUENCES());
        sb.append(" where SEQUENCE_OWNER=");
        sb.value(schemaName);
        sb.append(" order by name ASC");

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBObjectIdentity sequence = new DBObjectIdentity();
            sequence.setName(rs.getString("name"));
            sequence.setSchemaName(rs.getString("schema_name"));
            sequence.setType(DBObjectType.SEQUENCE);
            return sequence;
        });
    }

    @Override
    public List<DBObjectIdentity> listSynonyms(String schemaName, DBSynonymType synonymType) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        if (DBSynonymType.PUBLIC.equals(synonymType)) {
            sb.append(
                    "SELECT OWNER as schema_name, SYNONYM_NAME as name, 'PUBLIC_SYNONYM' as type FROM ALL_SYNONYMS where owner='PUBLIC'");
        } else if (DBSynonymType.COMMON.equals(synonymType)) {
            sb.append(
                    "SELECT OWNER as schema_name, SYNONYM_NAME as name, 'SYNONYM' as type FROM ALL_SYNONYMS where owner=")
                    .value(schemaName);
        } else {
            throw new UnsupportedOperationException("Not supported Synonym type");
        }

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    @Override
    public Map<String, List<DBTableColumn>> listTableColumns(String schemaName) {
        String sql = this.sqlMapper.getSql(Statements.LIST_SCHEMA_COLUMNS);
        List<DBTableColumn> tableColumns =
                this.jdbcOperations.query(sql.toString(), new Object[] {schemaName},
                        listColumnsRowMapper());
        Map<String, List<DBTableColumn>> tableName2Columns =
                tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
        tableName2Columns.forEach((table, cols) -> {
            Map<String, String> name2Comments = mapColumnName2ColumnComments(schemaName, table);
            cols.stream().forEach(col -> {
                if (name2Comments.containsKey(col.getName())) {
                    col.setComment(name2Comments.get(col.getName()));
                }
            });
        });
        return tableName2Columns;
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicTableColumns(String schemaName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_SCHEMA_TABLE_COLUMNS);
        List<DBTableColumn> tableColumns =
                jdbcOperations.query(sql, new Object[] {schemaName, schemaName}, listBasicColumnsRowMapper());
        return tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public List<DBTableColumn> listBasicTableColumns(String schemaName, String tableName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_TABLE_COLUMNS);
        return jdbcOperations.query(sql, new Object[] {schemaName, tableName}, listBasicColumnsRowMapper());
    }

    @Override
    public Map<String, List<DBTableColumn>> listBasicViewColumns(String schemaName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_SCHEMA_VIEW_COLUMNS);
        List<DBTableColumn> tableColumns =
                jdbcOperations.query(sql, new Object[] {schemaName, schemaName}, listBasicColumnsRowMapper());
        return tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public List<DBTableColumn> listBasicViewColumns(String schemaName, String viewName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_VIEW_COLUMNS);
        return jdbcOperations.query(sql, new Object[] {schemaName, viewName}, listBasicColumnsRowMapper());
    }

    @Override
    public Map<String, List<DBTableIndex>> listTableIndexes(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Map<String, List<DBTableConstraint>> listTableConstraints(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Map<String, DBTableOptions> listTableOptions(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBTablePartition> listTablePartitions(String tenantName, String schemaName, String tableName) {
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
        return false;
    }

    @Override
    public List<DBTableColumn> listTableColumns(String schemaName, String tableName) {
        String sql = this.sqlMapper.getSql(Statements.LIST_TABLE_COLUMNS);
        List<DBTableColumn> tableColumns =
                this.jdbcOperations.query(sql.toString(), new Object[] {schemaName, tableName},
                        listColumnsRowMapper());
        Map<String, String> name2Comments = mapColumnName2ColumnComments(schemaName, tableName);
        tableColumns.stream().forEach(dbTableColumn -> {
            if (name2Comments.containsKey(dbTableColumn.getName())) {
                dbTableColumn.setComment(name2Comments.get(dbTableColumn.getName()));
            }
        });
        return tableColumns;
    }

    @Override
    public List<DBObjectIdentity> listPartitionTables(String partitionMethod) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select DISTINCT OWNER as schema_name,TABLE_NAME as name,'TABLE' as type from ");
        sb.append(dataDictTableNames.PART_TABLES());
        sb.append(" where PARTITIONING_TYPE = ");
        sb.value(partitionMethod);

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBObjectIdentity.class));
    }

    protected Map<String, String> mapColumnName2ColumnComments(String schemaName, String tableName) {
        Map<String, String> commentsMap = new HashMap<>();
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select COLUMN_NAME, COMMENTS from ");
        sb.append(dataDictTableNames.COL_COMMENTS());
        sb.append(" where OWNER = ");
        sb.value(schemaName);
        sb.append(" and TABLE_NAME = ");
        sb.value(tableName);
        jdbcOperations.query(sb.toString(), resultSet -> {
            commentsMap.put(resultSet.getString(OracleConstants.COL_COLUMN_NAME),
                    resultSet.getString(OracleConstants.COL_COMMENTS));
        });
        return commentsMap;
    }

    protected RowMapper listColumnsRowMapper() {
        final int[] hiddenColumnOrdinaryPosition = {-1};
        return (rs, rowNum) -> {
            DBTableColumn tableColumn = new DBTableColumn();
            tableColumn.setSchemaName(rs.getString(OracleConstants.CONS_OWNER));
            tableColumn.setTableName(rs.getString(OracleConstants.COL_TABLE_NAME));
            tableColumn.setName(rs.getString(OracleConstants.COL_COLUMN_NAME));
            tableColumn.setTypeName(
                    DBSchemaAccessorUtil.normalizeTypeName(rs.getString(OracleConstants.COL_DATA_TYPE)));
            tableColumn.setFullTypeName(rs.getString(OracleConstants.COL_DATA_TYPE));
            tableColumn.setCharUsed(CharUnit.fromString(rs.getString(OracleConstants.COL_CHAR_USED)));
            tableColumn.setOrdinalPosition(rs.getInt(OracleConstants.COL_COLUMN_ID));
            tableColumn.setTypeModifiers(Arrays.asList(rs.getString(OracleConstants.COL_DATA_TYPE_MOD)));
            tableColumn.setMaxLength(
                    rs.getLong(tableColumn.getCharUsed() == CharUnit.CHAR ? OracleConstants.COL_CHAR_LENGTH
                            : OracleConstants.COL_DATA_LENGTH));
            tableColumn.setNullable("Y".equalsIgnoreCase(rs.getString(OracleConstants.COL_NULLABLE)));
            DBColumnTypeDisplay columnTypeDisplay = DBColumnTypeDisplay.fromName(tableColumn.getTypeName());
            if (columnTypeDisplay.displayScale()) {
                tableColumn.setScale(rs.getInt(OracleConstants.COL_DATA_SCALE));
            }
            if (columnTypeDisplay.displayPrecision()) {
                if (Objects.nonNull(rs.getObject(OracleConstants.COL_DATA_PRECISION))) {
                    tableColumn.setPrecision(rs.getLong(OracleConstants.COL_DATA_PRECISION));
                } else {
                    tableColumn.setPrecision(tableColumn.getMaxLength());
                }
            }
            tableColumn.setHidden("YES".equalsIgnoreCase(rs.getString(OracleConstants.COL_HIDDEN_COLUMN)));
            /**
             * hidden column does not have ordinary position, we assign an negative position<br>
             * for front-end as a key to identify a column
             *
             */
            if (tableColumn.getHidden()) {
                tableColumn.setOrdinalPosition(hiddenColumnOrdinaryPosition[0]);
                hiddenColumnOrdinaryPosition[0]--;
            }
            tableColumn.setVirtual("YES".equalsIgnoreCase(rs.getString(OracleConstants.COL_VIRTUAL_COLUMN)));
            tableColumn.setDefaultValue("NULL".equals(rs.getString(OracleConstants.COL_DATA_DEFAULT)) ? null
                    : rs.getString(OracleConstants.COL_DATA_DEFAULT));
            if (tableColumn.getVirtual()) {
                tableColumn.setGenExpression(rs.getString(OracleConstants.COL_DATA_DEFAULT));
            }
            return tableColumn;
        };

    }

    protected RowMapper listBasicColumnsRowMapper() {
        return (rs, rowNum) -> {
            DBTableColumn tableColumn = new DBTableColumn();
            tableColumn.setSchemaName(rs.getString(OracleConstants.CONS_OWNER));
            tableColumn.setTableName(rs.getString(OracleConstants.COL_TABLE_NAME));
            tableColumn.setName(rs.getString(OracleConstants.COL_COLUMN_NAME));
            tableColumn.setComment(rs.getString(OracleConstants.COL_COMMENTS));
            tableColumn
                    .setTypeName(DBSchemaAccessorUtil.normalizeTypeName(rs.getString(OracleConstants.COL_DATA_TYPE)));
            return tableColumn;
        };

    }

    @Override
    public List<DBTableConstraint> listTableConstraints(String schemaName, String tableName) {
        String sql = this.sqlMapper.getSql(Statements.LIST_TABLE_CONSTRAINTS);
        Map<String, DBTableConstraint> name2Constraint = new LinkedHashMap<>();
        jdbcOperations.query(sql, new Object[] {schemaName, tableName}, (rs, num) -> {
            String constraintName = rs.getString(OracleConstants.CONS_NAME);
            if (!name2Constraint.containsKey(constraintName)) {
                DBTableConstraint constraint = new DBTableConstraint();
                constraint.setName(constraintName);
                constraint.setOrdinalPosition(num);
                constraint.setSchemaName(rs.getString(OracleConstants.CONS_OWNER));
                constraint.setTableName(rs.getString(OracleConstants.COL_TABLE_NAME));
                constraint.setOwner(rs.getString(OracleConstants.CONS_OWNER));
                constraint.setValidate("VALIDATED".equalsIgnoreCase(rs.getString(OracleConstants.CONS_VALIDATED)));
                constraint.setType(DBConstraintType.fromValue(rs.getString(OracleConstants.CONS_TYPE)));
                constraint.setCheckClause(rs.getString("SEARCH_CONDITION"));
                constraint.setEnabled("ENABLED".equalsIgnoreCase(rs.getString("STATUS")));
                String deferrable = rs.getString(OracleConstants.CONS_DEFERRABLE);
                if (StringUtils.equalsIgnoreCase(deferrable, "DEFERRABLE")) {
                    constraint.setDeferability(DBConstraintDeferability.fromString(rs.getString("DEFERRED")));
                } else {
                    constraint.setDeferability(DBConstraintDeferability.NOT_DEFERRABLE);
                }
                List<String> columnNames = new ArrayList<>();
                columnNames.add(rs.getString("COLUMN_NAME"));
                constraint.setColumnNames(columnNames);
                List<String> refColumnNames = new ArrayList<>();
                constraint.setReferenceColumnNames(refColumnNames);
                if (DBConstraintType.FOREIGN_KEY == constraint.getType()) {
                    constraint.setReferenceTableName(rs.getString(OracleConstants.CONS_R_TABLE_NAME));
                    constraint.setReferenceSchemaName(rs.getString(OracleConstants.CONS_R_OWNER));
                    refColumnNames.add(rs.getString(OracleConstants.CONS_R_COLUMN_NAME));
                    constraint.setOnDeleteRule(
                            DBForeignKeyModifyRule.fromValue(rs.getString(OracleConstants.CONS_DELETE_RULE)));
                }
                name2Constraint.put(constraintName, constraint);
            } else {
                name2Constraint.get(constraintName).getColumnNames()
                        .add(rs.getString("COLUMN_NAME"));
                name2Constraint.get(constraintName).getReferenceColumnNames()
                        .add(rs.getString(OracleConstants.CONS_R_COLUMN_NAME));
            }
            return constraintName;
        });
        /**
         * 三表联查的结果 ColumnNames 和 RefColumnNames 可能存在 NULL 或者重复值，这里做一个过滤
         */
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
        partition.setPartitionOption(obtainPartitionOption(schemaName, tableName));
        partition.setPartitionDefinitions(
                obtainPartitionDefinition(schemaName, tableName, partition.getPartitionOption()));
        if (CollectionUtils.isNotEmpty(partition.getPartitionDefinitions())) {
            partition.getPartitionOption()
                    .setPartitionsNum(partition.getPartitionDefinitions().size());
        }
        return partition;
    }

    private DBTablePartitionOption obtainPartitionOption(String schemaName, String tableName) {
        DBTablePartitionOption option = new DBTablePartitionOption();
        String queryPartitionTypeSql = this.sqlMapper.getSql(Statements.GET_PARTITION_OPTION);
        jdbcOperations.query(queryPartitionTypeSql, new Object[] {schemaName, tableName}, (rs, num) -> {
            option.setType(DBTablePartitionType.fromValue(rs.getString("PARTITIONING_TYPE")));
            return option;
        });
        if (Objects.isNull(option.getType()) || option.getType() == DBTablePartitionType.NOT_PARTITIONED) {
            return option;
        }
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SELECT COLUMN_NAME FROM ")
                .append(dataDictTableNames.PART_KEY_COLUMNS())
                .append(" WHERE OWNER = ")
                .value(schemaName)
                .append(" AND NAME = ")
                .value(tableName);
        String queryColumnNameSql = sb.toString();
        List<String> columnNames = jdbcOperations.query(queryColumnNameSql, (rs, num) -> rs.getString("COLUMN_NAME"));
        if (option.getType().supportExpression()) {
            option.setExpression(String.join(",", columnNames));
        } else {
            option.setColumnNames(columnNames);
        }
        return option;
    }

    private List<DBTablePartitionDefinition> obtainPartitionDefinition(String schemaName, String tableName,
            DBTablePartitionOption option) {
        String sql = this.sqlMapper.getSql(Statements.LIST_PARTITION_DEFINITIONS);

        List<DBTablePartitionDefinition> partitionDefinitions =
                jdbcOperations.query(sql, new Object[] {schemaName, tableName}, (rs, num) -> {
                    DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                    partitionDefinition.setName(rs.getString("PARTITION_NAME"));
                    partitionDefinition.setOrdinalPosition(num);
                    partitionDefinition.setType(option.getType());
                    String description = rs.getString("HIGH_VALUE");
                    partitionDefinition.fillValues(description);
                    return partitionDefinition;
                });
        return partitionDefinitions;
    }

    @Override
    public List<DBTableIndex> listTableIndexes(String schemaName, String tableName) {
        List<DBTableIndex> indexList = obtainBasicIndexInfo(schemaName, tableName);
        fillIndexColumn(indexList, schemaName, tableName);
        return indexList;
    }

    protected void fillIndexColumn(List<DBTableIndex> indexList, String schemaName, String tableName) {
        String sql = this.sqlMapper.getSql(Statements.LIST_INDEX_COLUMNS);
        jdbcOperations.query(sql, new Object[] {schemaName, tableName}, (rs, num) -> {
            String indexName = rs.getString(OracleConstants.INDEX_NAME);
            String columnName = rs.getString(OracleConstants.COL_COLUMN_NAME);
            for (DBTableIndex index : indexList) {
                if (StringUtils.equals(index.getName(), indexName)) {
                    index.getColumnNames().add(columnName);
                }
            }
            return columnName;
        });

        for (DBTableIndex index : indexList) {
            if (index.getType() == DBIndexType.FUNCTION_BASED_NORMAL
                    || index.getType() == DBIndexType.FUNCTION_BASED_BITMAP) {
                List<String> columnNames = index.getColumnNames();
                if (CollectionUtils.isEmpty(columnNames)) {
                    return;
                }
                OracleSqlBuilder sqlBuilder = new OracleSqlBuilder();
                sqlBuilder.append("SELECT COLUMN_POSITION, COLUMN_EXPRESSION FROM ")
                        .append(dataDictTableNames.IND_EXPRESSIONS())
                        .append(" WHERE TABLE_OWNER=").value(schemaName)
                        .append(" AND TABLE_NAME=").value(tableName)
                        .append(" AND INDEX_NAME=").value(index.getName())
                        .append(" ORDER BY COLUMN_POSITION ASC");
                jdbcOperations.query(sqlBuilder.toString(), (rs, num) -> {
                    columnNames.set(rs.getInt("COLUMN_POSITION") - 1, rs.getString("COLUMN_EXPRESSION"));
                    return columnNames;
                });
            }
        }
    }

    protected List<DBTableIndex> obtainBasicIndexInfo(String schemaName, String tableName) {
        String sql = this.sqlMapper.getSql(Statements.LIST_TABLE_INDEXES);
        Map<String, DBTableIndex> indexName2Index = new LinkedHashMap<>();
        jdbcOperations.query(sql, new Object[] {schemaName, tableName}, (rs, num) -> {
            DBTableIndex index = new DBTableIndex();
            index.setName(rs.getString(OracleConstants.INDEX_NAME));
            index.setOrdinalPosition(num);
            index.setOwner(rs.getString("OWNER"));
            index.setNonUnique(!"UNIQUE".equalsIgnoreCase(rs.getString(OracleConstants.INDEX_UNIQUENESS)));
            index.setType(DBIndexType.fromString(rs.getString(OracleConstants.INDEX_TYPE)));
            index.setVisible("VISIBLE".equalsIgnoreCase(rs.getString("VISIBILITY")));
            if (index.isNonUnique()) {
                index.setType(DBIndexType.fromString(rs.getString(OracleConstants.INDEX_TYPE)));
            } else {
                index.setType(DBIndexType.UNIQUE);
            }
            index.setAlgorithm(DBIndexAlgorithm.fromString(rs.getString(OracleConstants.INDEX_TYPE)));
            index.setCompressInfo(rs.getString(OracleConstants.INDEX_COMPRESSION));
            index.setColumnNames(new ArrayList<>());
            index.setAvailable(isTableIndexAvailable(rs.getString(OracleConstants.INDEX_STATUS)));
            if (judgeIndexGlobalOrLocalFromDataDict()) {
                index.setGlobal("NO".equalsIgnoreCase(rs.getString("PARTITIONED")));
            }
            indexName2Index.putIfAbsent(index.getName(), index);
            return index;
        });
        return new ArrayList<>(indexName2Index.values());
    }

    protected boolean isTableIndexAvailable(String status) {
        return !"UNUSABLE".equals(status);
    }

    protected boolean judgeIndexGlobalOrLocalFromDataDict() {
        return true;
    }

    @Override
    public String getTableDDL(String schemaName, String tableName) {
        StringBuilder ddl = new StringBuilder(getTableDDLOnly(schemaName, tableName));
        ddl.append(";\n");
        Map<String, String> variables = new HashMap<>();
        DBTableOptions tableOptions = getTableOptions(schemaName, tableName);
        variables.put("schemaName", StringUtils.quoteOracleIdentifier(schemaName));
        variables.put("tableName",
                StringUtils.quoteOracleIdentifier(tableName));
        if (StringUtils.isNotEmpty(tableOptions.getComment())) {
            variables.put("comment", StringUtils.quoteOracleValue(tableOptions.getComment()));
            String tableCommentDdl = StringUtils.replaceVariables(ORACLE_TABLE_COMMENT_DDL_TEMPLATE, variables);
            ddl.append(tableCommentDdl).append(";\n");
        }
        List<DBTableColumn> columns = listTableColumns(schemaName, tableName);
        for (DBTableColumn column : columns) {
            if (StringUtils.isNotEmpty(column.getComment())) {
                variables.put("columnName", StringUtils.quoteOracleIdentifier(column.getName()));
                variables.put("comment", StringUtils.quoteOracleValue(column.getComment()));
                String columnCommentDdl = StringUtils.replaceVariables(ORACLE_COLUMN_COMMENT_DDL_TEMPLATE, variables);
                ddl.append(columnCommentDdl).append(";\n");
            }
        }
        List<DBTableIndex> indexes = listTableIndexes(schemaName, tableName);
        for (DBTableIndex index : indexes) {
            /**
             * 如果有唯一索引，则在表的 DDL 里已经包含了对应的唯一约束 这里就不需要再去获取索引的 DDL 了，否则会重复
             */
            if (index.getType() == DBIndexType.UNIQUE) {
                continue;
            }
            OracleSqlBuilder getIndexDDLSql = new OracleSqlBuilder();
            getIndexDDLSql.append("SELECT dbms_metadata.get_ddl('INDEX', ");
            getIndexDDLSql.value(index.getName());
            getIndexDDLSql.append(", ");
            getIndexDDLSql.value(schemaName);
            getIndexDDLSql.append(") as DDL from dual");
            try {
                jdbcOperations.query(getIndexDDLSql.toString(), (rs, num) -> {
                    String indexDdl = rs.getString("DDL");
                    /**
                     * OB 4.0，dbms_metadata.get_ddl 可能会返回表的 DDL，属于 内核的 bug <br>
                     * 这里判断下如果是 CREATE TABLE 开头，则跳过
                     */
                    if (StringUtils.isNotBlank(indexDdl) && !indexDdl.startsWith("CREATE TABLE")) {
                        ddl.append("\n").append(rs.getString("DDL"));
                    }
                    return ddl;
                });
            } catch (Exception ex) {
                log.warn("get index ddl failed, ex=", ex);
                index.setWarning("get index ddl failed");
            }
        }
        return ddl.toString();

    }

    protected String getTableDDLOnly(String schemaName, String tableName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SELECT dbms_metadata.get_ddl('TABLE', ")
                .value(tableName)
                .append(", ")
                .value(schemaName)
                .append(") as DDL from dual");

        return jdbcOperations.queryForObject(sb.toString(), String.class);
    }

    @Override
    public DBTableOptions getTableOptions(String schemaName, String tableName) {
        DBTableOptions tableOptions = new DBTableOptions();
        obtainTableCharset(tableOptions);
        obtainTableCollation(tableOptions);
        obtainTableComment(schemaName, tableName, tableOptions);
        obtainTableCreateAndUpdateTime(schemaName, tableName, tableOptions);
        return tableOptions;
    }

    @Override
    public DBTableOptions getTableOptions(String schemaName, String tableName, String ddl) {
        return getTableOptions(schemaName, tableName);
    }

    protected void obtainTableCharset(DBTableOptions tableOptions) {
        String sql = "select value from v$nls_parameters where PARAMETER = 'NLS_CHARACTERSET'";
        jdbcOperations.query(sql, t -> {
            tableOptions.setCharsetName(t.getString(1));
        });
    }

    protected void obtainTableCollation(DBTableOptions tableOptions) {
        String sql = "SELECT value from v$nls_parameters where parameter = 'NLS_SORT'";
        jdbcOperations.query(sql, t -> {
            tableOptions.setCollationName(t.getString(1));
        });
    }

    private void obtainTableComment(String schemaName, String tableName, DBTableOptions tableOptions) {
        OracleSqlBuilder sql = new OracleSqlBuilder();
        sql.append("select comments from ").append(dataDictTableNames.TAB_COMMENTS())
                .append(" where owner=").value(schemaName).append(" and table_name=").value(tableName)
                .append(" and comments is not null").toString();
        jdbcOperations.query(sql.toString(), t -> {
            tableOptions.setComment(t.getString("COMMENTS"));
        });
    }

    private void obtainTableCreateAndUpdateTime(String schemaName, String tableName, DBTableOptions tableOptions) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select CREATED, LAST_DDL_TIME from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" WHERE OBJECT_TYPE = ");
        sb.value("TABLE");
        sb.append(" and OWNER = ");
        sb.value(schemaName);
        sb.append(" and OBJECT_NAME = ");
        sb.value(tableName);
        jdbcOperations.query(sb.toString(), rs -> {
            tableOptions.setCreateTime(rs.getTimestamp("CREATED"));
            tableOptions.setUpdateTime(rs.getTimestamp("LAST_DDL_TIME"));
        });

    }

    @Override
    public DBView getView(String schemaName, String viewName) {
        DBView view = new DBView();
        view.setViewName(viewName);
        view.setSchemaName(schemaName);
        view.setDefiner(schemaName);
        OracleSqlBuilder getDDL = new OracleSqlBuilder();
        getDDL.append("SELECT dbms_metadata.get_ddl('VIEW', ")
                .value(viewName)
                .append(", ")
                .value(schemaName)
                .append(") as DDL FROM dual");
        jdbcOperations.query(getDDL.toString(), rs -> {
            view.setDdl(rs.getString(1));
        });

        OracleSqlBuilder getColumns = new OracleSqlBuilder();
        getColumns.append(
                "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE, DATA_DEFAULT, COMMENTS FROM SYS.ALL_TAB_COLS NATURAL JOIN SYS.ALL_COL_COMMENTS WHERE OWNER = ")
                .value(schemaName).append(" AND TABLE_NAME=").value(viewName).append(" ORDER BY COLUMN_ID ASC");
        List<DBTableColumn> columns = jdbcOperations.query(getColumns.toString(), (rs, rowNum) -> {
            DBTableColumn column = new DBTableColumn();
            column.setName(rs.getString("COLUMN_NAME"));
            column.setTypeName(rs.getString("DATA_TYPE"));
            column.setNullable("Y".equalsIgnoreCase(rs.getString("NULLABLE")));
            column.setDefaultValue(rs.getString("DATA_DEFAULT"));
            column.setOrdinalPosition(rowNum);
            column.setTableName(view.getViewName());
            return column;
        });
        view.setColumns(columns);
        return view;
    }

    @Override
    public DBFunction getFunction(String schemaName, String functionName) {
        OracleSqlBuilder info = new OracleSqlBuilder();
        info.append("select OWNER, STATUS, CREATED, LAST_DDL_TIME from ")
                .append(dataDictTableNames.OBJECTS())
                .append(" where object_type='FUNCTION' and owner=")
                .value(schemaName)
                .append(" and OBJECT_NAME=")
                .value(functionName);
        DBFunction function = new DBFunction();
        function.setFunName(functionName);
        function.setDefiner(schemaName);

        jdbcOperations.query(info.toString(), (rs) -> {
            function.setDefiner(rs.getString("OWNER"));
            function.setStatus(rs.getString("STATUS"));
            function.setCreateTime(Timestamp.valueOf(rs.getString("CREATED")));
            function.setModifyTime(Timestamp.valueOf(rs.getString("LAST_DDL_TIME")));
        });

        OracleSqlBuilder ddl = new OracleSqlBuilder();
        ddl.append("SELECT dbms_metadata.get_ddl('FUNCTION', ")
                .value(functionName)
                .append(", ")
                .value(schemaName)
                .append(") as DDL from dual");
        jdbcOperations.query(ddl.toString(), rs -> {
            function.setDdl(rs.getString(1));
        });

        OracleSqlBuilder getParams = new OracleSqlBuilder();
        getParams.append("SELECT OWNER, OBJECT_NAME, ARGUMENT_NAME, DATA_TYPE, IN_OUT, PLS_TYPE, POSITION FROM ")
                .append(dataDictTableNames.ARGUMENTS())
                .append(" WHERE OWNER=")
                .value(schemaName)
                .append(" AND OBJECT_NAME=")
                .value(functionName)
                .append("AND PACKAGE_NAME IS NULL ORDER BY POSITION");
        List<DBPLParam> params = new ArrayList<>();
        jdbcOperations.query(getParams.toString(), rs -> {
            if (Objects.isNull(rs.getString("ARGUMENT_NAME"))) {
                function.setReturnType(rs.getString("DATA_TYPE"));
            } else {
                DBPLParam param = new DBPLParam();
                param.setParamName(rs.getString("ARGUMENT_NAME"));
                param.setDataType(rs.getString("DATA_TYPE"));
                param.setParamMode(DBPLParamMode.getEnum(rs.getString("IN_OUT")));
                param.setSeqNum(rs.getInt("POSITION"));
                params.add(param);
            }
        });
        function.setParams(params);
        if (StringUtils.containsIgnoreCase(function.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
            function.setErrorMessage(PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                    function.getDefiner(), DBObjectType.FUNCTION.name(), function.getFunName()));
        }

        return parseFunctionDDL(function);
    }

    protected DBFunction parseFunctionDDL(DBFunction function) {
        // get variables defined in function by parse function ddl
        try {
            ParseOraclePLResult result = PLParser.parseOracle(function.getDdl());
            function.setVariables(result.getVaribaleList());
            function.setTypes(result.getTypeList());
        } catch (Exception e) {
            log.warn("Failed to parse function ddl={}, errorMessage={}", function.getDdl(), e.getMessage());
            function.setParseErrorMessage(e.getMessage());
        }
        return function;
    }

    @Override
    public DBProcedure getProcedure(String schemaName, String procedureName) {
        OracleSqlBuilder info = new OracleSqlBuilder();
        info.append("select OWNER, STATUS, CREATED, LAST_DDL_TIME from ")
                .append(dataDictTableNames.OBJECTS())
                .append(" where object_type='PROCEDURE' and owner=")
                .value(schemaName)
                .append(" and OBJECT_NAME=")
                .value(procedureName);
        DBProcedure procedure = new DBProcedure();
        procedure.setProName(procedureName);

        jdbcOperations.query(info.toString(), (rs) -> {
            procedure.setDefiner(rs.getString("OWNER"));
            procedure.setStatus(rs.getString("STATUS"));
            procedure.setCreateTime(Timestamp.valueOf(rs.getString("CREATED")));
            procedure.setModifyTime(Timestamp.valueOf(rs.getString("LAST_DDL_TIME")));
        });

        OracleSqlBuilder ddl = new OracleSqlBuilder();
        ddl.append("SELECT dbms_metadata.get_ddl('PROCEDURE', ")
                .value(procedureName)
                .append(", ")
                .value(schemaName)
                .append(") as DDL from dual");
        jdbcOperations.query(ddl.toString(), rs -> {
            procedure.setDdl(rs.getString(1));
        });

        OracleSqlBuilder getParams = new OracleSqlBuilder();
        getParams.append(
                "SELECT OWNER, OBJECT_NAME, ARGUMENT_NAME, DATA_TYPE, IN_OUT, PLS_TYPE, DEFAULT_VALUE, POSITION FROM ")
                .append(dataDictTableNames.ARGUMENTS())
                .append(" WHERE OWNER=")
                .value(schemaName)
                .append(" AND OBJECT_NAME=")
                .value(procedureName)
                .append(" AND PACKAGE_NAME IS NULL ORDER BY POSITION");
        List<DBPLParam> params = new ArrayList<>();
        jdbcOperations.query(getParams.toString(), rs -> {
            DBPLParam param = new DBPLParam();
            param.setParamName(rs.getString("ARGUMENT_NAME"));
            param.setDataType(rs.getString("DATA_TYPE"));
            param.setSeqNum(rs.getInt("POSITION"));
            param.setParamMode(DBPLParamMode.getEnum(rs.getString("IN_OUT")));
            params.add(param);
        });
        procedure.setParams(params);
        if (StringUtils.containsIgnoreCase(procedure.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
            procedure.setErrorMessage(PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                    procedure.getDefiner(), DBObjectType.PROCEDURE.name(), procedure.getProName()));
        }
        return parseProcedureDDL(procedure);
    }

    protected DBProcedure parseProcedureDDL(DBProcedure procedure) {
        Validate.notBlank(procedure.getDdl(), "procedure.ddl");
        String ddl = procedure.getDdl();
        ParseOraclePLResult result;
        try {
            result = PLParser.parseOracle(ddl);
        } catch (Exception e) {
            log.warn("Failed to parse oracle procedure ddl, ddl={}, errorMessage={}", ddl, e.getMessage());
            procedure.setParseErrorMessage(e.getMessage());
            return procedure;
        }
        procedure.setVariables(result.getVaribaleList());
        procedure.setTypes((result.getTypeList()));
        return procedure;
    }

    @Override
    public DBPackage getPackage(String schemaName, String packageName) {
        OracleSqlBuilder info = new OracleSqlBuilder();
        info.append(
                "select OWNER, OBJECT_TYPE, STATUS, CREATED, LAST_DDL_TIME from ")
                .append(dataDictTableNames.OBJECTS())
                .append(" where object_type in ('PACKAGE', 'PACKAGE BODY') and owner=")
                .value(schemaName)
                .append(" and OBJECT_NAME=")
                .value(packageName)
                .append(" order by OBJECT_TYPE");

        DBPackage dbPackage = new DBPackage();
        dbPackage.setPackageName(packageName);

        DBPackageDetail packageHead = new DBPackageDetail();
        DBPackageDetail packageBody = new DBPackageDetail();
        DBPackageBasicInfo packageHeadBasicInfo = new DBPackageBasicInfo();
        DBPackageBasicInfo packageBodyBasicInfo = new DBPackageBasicInfo();
        packageHead.setBasicInfo(packageHeadBasicInfo);
        packageBody.setBasicInfo(packageBodyBasicInfo);
        dbPackage.setPackageHead(packageHead);
        dbPackage.setPackageBody(packageBody);

        jdbcOperations.query(info.toString(), (rs) -> {
            dbPackage.setStatus(rs.getString("STATUS"));
            if (DBObjectType.PACKAGE.name().equalsIgnoreCase(rs.getString("OBJECT_TYPE"))) {
                packageHeadBasicInfo.setDefiner(rs.getString("OWNER"));
                packageHeadBasicInfo.setCreateTime(rs.getTimestamp("CREATED"));
                packageHeadBasicInfo.setModifyTime(rs.getTimestamp("LAST_DDL_TIME"));
            } else {
                packageBodyBasicInfo.setDefiner(rs.getString("OWNER"));
                packageBodyBasicInfo.setCreateTime(rs.getTimestamp("CREATED"));
                packageBodyBasicInfo.setModifyTime(rs.getTimestamp("LAST_DDL_TIME"));
            }
        });

        OracleSqlBuilder packageHeadDDL = new OracleSqlBuilder();
        packageHeadDDL.append("SELECT dbms_metadata.get_ddl('PACKAGE_SPEC', ")
                .value(packageName)
                .append(", ")
                .value(schemaName)
                .append(") as DDL from dual");
        jdbcOperations.query(packageHeadDDL.toString(), rs -> {
            packageHeadBasicInfo.setDdl(rs.getString(1));
        });
        parsePackageDDL(packageHead);

        OracleSqlBuilder packageBodyDDL = new OracleSqlBuilder();
        packageBodyDDL.append("SELECT dbms_metadata.get_ddl('PACKAGE_BODY', ")
                .value(packageName)
                .append(", ")
                .value(schemaName)
                .append(") as DDL from dual");
        jdbcOperations.query(packageBodyDDL.toString(), rs -> {
            packageBodyBasicInfo.setDdl(rs.getString(1));
        });
        parsePackageDDL(packageBody);

        if (StringUtils.containsIgnoreCase(dbPackage.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
            dbPackage.setErrorMessage(PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                    schemaName, DBObjectType.PACKAGE.name(), dbPackage.getPackageName()));
        }
        return dbPackage;
    }

    private void parsePackageDDL(DBPackageDetail packageDetail) {
        if (Objects.isNull(packageDetail.getBasicInfo().getDdl())) {
            return;
        }
        try {
            ParseOraclePLResult oraclePLResult = PLParser.parseOracle(packageDetail.getBasicInfo().getDdl());
            packageDetail.setVariables(oraclePLResult.getVaribaleList());
            packageDetail.setTypes(oraclePLResult.getTypeList());
            packageDetail.setFunctions(oraclePLResult.getFunctionList());
            packageDetail.setProcedures(oraclePLResult.getProcedureList());
        } catch (Exception e) {
            log.warn("Failed to parse package ddl={}, errorMessage={}", packageDetail.getBasicInfo().getDdl(),
                    e.getMessage());
            packageDetail.setParseErrorMessage(e.getMessage());
        }
    }

    @Override
    public DBTrigger getTrigger(String schemaName, String triggerName) {
        DBTrigger trigger = new DBTrigger();
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append(
                "select s.OWNER, s.TRIGGER_NAME, s.BASE_OBJECT_TYPE, s.TABLE_OWNER, s.TABLE_NAME, s.STATUS as ENABLE_STATUS, o.STATUS")
                .append(" FROM (SELECT * FROM ")
                .append(dataDictTableNames.OBJECTS())
                .append(" WHERE OBJECT_TYPE='TRIGGER') o RIGHT JOIN ")
                .append(dataDictTableNames.TRIGGERS())
                .append(" s ON o.OBJECT_NAME=s.TRIGGER_NAME AND o.OWNER=s.OWNER")
                .append(" WHERE s.OWNER=").value(schemaName)
                .append(" AND s.TRIGGER_NAME=").value(triggerName);
        jdbcOperations.query(sb.toString(), (rs) -> {
            trigger.setBaseObjectType(rs.getString("BASE_OBJECT_TYPE"));
            trigger.setTriggerName(rs.getString("TRIGGER_NAME"));
            trigger.setOwner(rs.getString("OWNER"));
            trigger.setSchemaMode(rs.getString("TABLE_OWNER"));
            trigger.setSchemaName(rs.getString("TABLE_NAME"));
            trigger.setEnable("ENABLED".equalsIgnoreCase(rs.getString("ENABLE_STATUS")));
            trigger.setStatus(rs.getString("STATUS"));
        });

        OracleSqlBuilder ddl = new OracleSqlBuilder();
        ddl.append("SELECT dbms_metadata.get_ddl('TRIGGER',")
                .value(triggerName)
                .append(", ")
                .value(schemaName)
                .append(") as DDL FROM dual");
        jdbcOperations.query(ddl.toString(), rs -> {
            trigger.setDdl(rs.getString(1));
        });
        if (StringUtils.containsIgnoreCase(trigger.getStatus(), PLConstants.PL_OBJECT_STATUS_INVALID)) {
            trigger.setErrorMessage(PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                    trigger.getOwner(), DBObjectType.TRIGGER.name(), trigger.getTriggerName()));
        }
        return trigger;
    }

    @Override
    public DBType getType(String schemaName, String typeName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select a.OWNER,a.OBJECT_NAME,u.TYPE_NAME,a.CREATED,a.LAST_DDL_TIME,u.TYPECODE,u.TYPEID from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" a right join ");
        sb.append(dataDictTableNames.TYPES());
        sb.append(" u on a.OBJECT_NAME=u.TYPE_NAME where a.OWNER=");
        sb.value(schemaName);
        sb.append(" and u.TYPE_NAME=");
        sb.value(typeName);

        DBType type = new DBType();
        jdbcOperations.query(sb.toString(), (rs) -> {
            type.setOwner(rs.getString("OWNER"));
            type.setTypeName(rs.getString("TYPE_NAME"));
            type.setCreateTime(rs.getTimestamp("CREATED"));
            type.setLastDdlTime(rs.getTimestamp("LAST_DDL_TIME"));
            type.setTypeId(rs.getString("TYPEID"));
            type.setType(rs.getString("TYPECODE"));
        });
        return parseTypeDDL(type);
    }

    protected DBType parseTypeDDL(DBType type) {
        OracleSqlBuilder typeDDL = new OracleSqlBuilder();
        typeDDL.append("select dbms_metadata.get_ddl('TYPE', ")
                .value(type.getTypeName())
                .append(", ")
                .value(type.getOwner())
                .append(") as DDL from dual");

        String typeDdl = queryTypeDdl(typeDDL.toString());

        OracleSqlBuilder typeSpecDDL = new OracleSqlBuilder();
        typeSpecDDL.append("select dbms_metadata.get_ddl('TYPE_SPEC', ")
                .value(type.getTypeName())
                .append(", ")
                .value(type.getOwner())
                .append(") as DDL from dual");

        String typeHeadDdl = queryTypeSpecDdl(typeSpecDDL.toString());
        Validate.notBlank(typeDdl, "typeDdl");
        Validate.notBlank(typeHeadDdl, "typeHeadDdl");
        type.setDdl(typeDdl);

        return parseTypeDDL(type, typeHeadDdl);
    }

    protected String queryTypeDdl(String querySql) {
        return jdbcOperations.query(querySql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getString(1);
        });
    }

    protected String queryTypeSpecDdl(String querySql) {
        return jdbcOperations.query(querySql, rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getString(1);
        });
    }

    protected DBType parseTypeDDL(DBType type, String typeHeadDdl) {
        DBBasicPLObject typeDetail = new DBBasicPLObject();
        try {
            ParseOraclePLResult oraclePLResult = PLParser.parseObOracle(typeHeadDdl);
            typeDetail.setVariables(oraclePLResult.getVaribaleList());
            typeDetail.setTypes(oraclePLResult.getTypeList());
            typeDetail.setProcedures(oraclePLResult.getProcedureList());
            typeDetail.setFunctions(oraclePLResult.getFunctionList());
        } catch (Exception e) {
            log.warn("Parse type ddl failed, ddl={}, errorMessage={}", typeHeadDdl, e.getMessage());
            typeDetail.setParseErrorMessage(e.getMessage());
        }

        type.setTypeDetail(typeDetail);
        String errorText = PLObjectErrMsgUtils.getOraclePLObjErrMsg(jdbcOperations,
                type.getOwner(), DBObjectType.TYPE.name(), type.getTypeName());
        if (StringUtils.isNotBlank(errorText)) {
            type.setStatus(PLConstants.PL_OBJECT_STATUS_INVALID);
            type.setErrorMessage(errorText);
        }
        return type;
    }

    @Override
    public DBSequence getSequence(String schemaName, String sequenceName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select * from ")
                .append(dataDictTableNames.SEQUENCES())
                .append(" where sequence_owner=")
                .value(schemaName)
                .append(" and sequence_name=")
                .value(sequenceName);

        DBSequence sequence = new DBSequence();
        sequence.setName(sequenceName);
        jdbcOperations.query(sb.toString(), rs -> {
            sequence.setUser(rs.getString("SEQUENCE_OWNER"));
            sequence.setMinValue(rs.getBigDecimal("MIN_VALUE").toString());
            sequence.setMaxValue(rs.getBigDecimal("MAX_VALUE").toString());
            sequence.setIncreament(rs.getBigDecimal("INCREMENT_BY").longValue());
            sequence.setCycled("Y".equalsIgnoreCase(rs.getString("CYCLE_FLAG")));
            sequence.setOrderd("Y".equalsIgnoreCase(rs.getString("ORDER_FLAG")));
            long cacheSize = rs.getBigDecimal("CACHE_SIZE").longValue();
            if (cacheSize > 1) {
                sequence.setCacheSize(cacheSize);
                sequence.setCached(true);
            } else {
                sequence.setCached(false);
            }
            sequence.setNextCacheValue(rs.getBigDecimal("LAST_NUMBER").toString());

        });

        String ddl = getSequenceDDL(sequence);
        sequence.setDdl(ddl);
        return sequence;
    }

    protected String getSequenceDDL(DBSequence sequence) {
        Validate.notNull(sequence, "sequence");
        Validate.notBlank(sequence.getName(), "sequence.name");

        OracleSqlBuilder ddl = new OracleSqlBuilder();
        ddl.append("SELECT dbms_metadata.get_ddl('SEQUENCE', ")
                .value(sequence.getName())
                .append(", ")
                .value(sequence.getUser())
                .append(") as DDL from dual");
        return jdbcOperations.query(ddl.toString(), rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getString(1);
        });
    }

    @Override
    public DBSynonym getSynonym(String schemaName, String synonymName, DBSynonymType synonymType) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append(
                "select s.OWNER,s.SYNONYM_NAME,s.TABLE_OWNER,s.TABLE_NAME,s.DB_LINK,o.CREATED,o.LAST_DDL_TIME,o.STATUS from ");
        sb.append(dataDictTableNames.SYNONYMS());
        sb.append(" s left join (select * from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where OBJECT_TYPE='SYNONYM') o on s.SYNONYM_NAME=o.OBJECT_NAME and s.OWNER=o.OWNER where s.OWNER=");
        sb.value(getSynonymOwnerSymbol(synonymType, schemaName));
        sb.append(" and s.SYNONYM_NAME=");
        sb.value(synonymName);

        DBSynonym synonym = new DBSynonym();
        synonym.setSynonymType(synonymType);
        jdbcOperations.query(sb.toString(), rs -> {
            synonym.setOwner(rs.getString("OWNER"));
            synonym.setSynonymName(rs.getString("SYNONYM_NAME"));
            synonym.setTableOwner(rs.getString("TABLE_OWNER"));
            synonym.setTableName(rs.getString("TABLE_NAME"));
            synonym.setDbLink(rs.getString("DB_LINK"));
            synonym.setCreated(rs.getTimestamp("CREATED"));
            synonym.setLastDdlTime(rs.getTimestamp("LAST_DDL_TIME"));
            synonym.setStatus(rs.getString("STATUS"));
        });
        synonym.setDdl(getSynonymDDL(synonym));

        return synonym;
    }

    protected String getSynonymDDL(DBSynonym synonym) {
        OracleSqlBuilder ddl = new OracleSqlBuilder();
        ddl.append("SELECT dbms_metadata.get_ddl('SYNONYM', ")
                .value(synonym.getSynonymName())
                .append(", ")
                .value(synonym.getOwner())
                .append(") as DDL from dual");

        return jdbcOperations.query(ddl.toString(), rs -> {
            if (!rs.next()) {
                return null;
            }
            return rs.getString(1);
        });
    }

    protected String getSynonymOwnerSymbol(DBSynonymType synonymType, String schemaName) {
        if (synonymType.equals(DBSynonymType.PUBLIC)) {
            return "PUBLIC";
        } else if (synonymType.equals(DBSynonymType.COMMON)) {
            return schemaName;
        } else {
            throw new UnsupportedOperationException("Not supported Synonym type");
        }
    }

}
