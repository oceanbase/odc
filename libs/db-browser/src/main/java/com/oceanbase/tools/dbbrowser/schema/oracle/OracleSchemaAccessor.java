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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;

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
import com.oceanbase.tools.dbbrowser.model.DBPackage;
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
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMapper;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorSqlMappers;
import com.oceanbase.tools.dbbrowser.schema.constant.Statements;
import com.oceanbase.tools.dbbrowser.schema.constant.StatementsFiles;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.OracleDataDictTableNames;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
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
    private static final Set<String> ESCAPE_USER_SET = new HashSet<>(3);

    static {
        ESCAPE_USER_SET.add("PUBLIC");
        ESCAPE_USER_SET.add("LBACSYS");
        ESCAPE_USER_SET.add("ORAAUDITOR");
    }
    protected OracleDataDictTableNames dataDictTableNames;
    protected JdbcOperations jdbcOperations;
    protected DBSchemaAccessorSqlMapper sqlMapper;

    public OracleSchemaAccessor(@NonNull JdbcOperations jdbcOperations,
            OracleDataDictTableNames dataDictTableNames) {
        this.dataDictTableNames = dataDictTableNames;
        this.jdbcOperations = jdbcOperations;
        this.sqlMapper = DBSchemaAccessorSqlMappers.get(StatementsFiles.OBORACLE_4_0_x);
    }

    @Override
    public List<String> showDatabases() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select USERNAME from ");
        sb.identifier(dataDictTableNames.USERS());

        List<String> users = jdbcOperations.queryForList(sb.toString(), String.class);
        return users.stream().filter(user -> !ESCAPE_USER_SET.contains(user)).collect(Collectors.toList());
    }

    @Override
    public DBDatabase getDatabase(String schemaName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBDatabase> listDatabases() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void switchDatabase(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("alter session set current_schema=");
        sb.value(schemaName);
        jdbcOperations.execute(sb.toString());
    }

    @Override
    public List<DBObjectIdentity> listUsers() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SELECT USERNAME FROM ");
        sb.identifier(dataDictTableNames.USERS());
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
        sb.identifier(dataDictTableNames.VIEWS());
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
                .append("  order by name asc;");
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
        String sql = "select VIEW_NAME from SYS.ALL_VIEWS where OWNER='SYS' ORDER BY VIEW_NAME";
        return jdbcOperations.queryForList(sql, String.class);
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
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("show character set");

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString(1));
    }

    @Override
    public List<String> showCollation() {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("show collation");

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> rs.getString(1));
    }

    @Override
    public List<DBPLObjectIdentity> listFunctions(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select OWNER as schema_name, object_type as type, OBJECT_NAME as name, STATUS from ")
                .append(dataDictTableNames.OBJECTS())
                .append(" where object_type = 'FUNCTION' and owner=")
                .value(schemaName)
                .append(" order by object_name asc");

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBPLObjectIdentity.class));
    }

    @Override
    public List<DBPLObjectIdentity> listProcedures(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select object_name as name, object_type as type, owner as schema_name, status from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where object_type = 'PROCEDURE' and owner=");
        sb.value(schemaName);
        sb.append(" order by object_name asc");

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBPLObjectIdentity.class));
    }

    @Override
    public List<DBPLObjectIdentity> listPackages(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select object_name as name, object_type as type, owner, status from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where (object_type = 'PACKAGE' or object_type = 'PACKAGE BODY') and owner=");
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

        return jdbcOperations.query(sb.toString(), (rs, rowNum) -> {
            DBPLObjectIdentity trigger = new DBPLObjectIdentity();
            trigger.setName(rs.getString("TRIGGER_NAME"));
            trigger.setSchemaName(rs.getString("OWNER"));
            trigger.setStatus(rs.getString("STATUS"));
            trigger.setEnable("ENABLED".equals(rs.getString("ENABLE_STATUS")));
            trigger.setType(DBObjectType.TRIGGER);
            return trigger;
        });
    }

    @Override
    public List<DBPLObjectIdentity> listTypes(String schemaName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();

        sb.append("select OBJECT_NAME as name, STATUS, OBJECT_TYPE as type, OWNER as schema_name from ");
        sb.append(dataDictTableNames.OBJECTS());
        sb.append(" where OBJECT_TYPE='TYPE' and OWNER=");
        sb.value(schemaName);
        sb.append(" order by OBJECT_NAME asc");

        return jdbcOperations.query(sb.toString(), new BeanPropertyRowMapper<>(DBPLObjectIdentity.class));
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
        throw new UnsupportedOperationException("Not supported yet");
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
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_SCHEMA_COLUMNS);
        List<DBTableColumn> tableColumns =
                jdbcOperations.query(sql, new Object[] {schemaName}, listBasicColumnsRowMapper());
        return tableColumns.stream().collect(Collectors.groupingBy(DBTableColumn::getTableName));
    }

    @Override
    public List<DBTableColumn> listBasicTableColumns(String schemaName, String tableName) {
        String sql = sqlMapper.getSql(Statements.LIST_BASIC_TABLE_COLUMNS);
        return jdbcOperations.query(sql, new Object[] {schemaName, tableName}, listBasicColumnsRowMapper());
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
            tableColumn.setDefaultValue(rs.getString(OracleConstants.COL_DATA_DEFAULT));
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
            indexName2Index.putIfAbsent(index.getName(), index);
            return index;
        });
        return new ArrayList<>(indexName2Index.values());
    }

    @Override
    public String getTableDDL(String schemaName, String tableName) {
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("SHOW CREATE TABLE ");
        sb.identifier(schemaName);
        sb.append(".");
        sb.identifier(tableName);

        AtomicReference<String> ddlRef = new AtomicReference<>();
        jdbcOperations.query(sb.toString(), t -> {
            // Create table ddl like this: CREATE [GLOBAL TEMPORARY|SHARDED|DUPLICATED] TABLE T...
            String ddl = t.getString(2);
            if (Objects.nonNull(ddl)) {
                // fix: Replace " TABLE " to " TABLE schemaName."
                ddlRef.set(StringUtils.replace(ddl, " TABLE ",
                        " TABLE " + StringUtils.quoteOracleIdentifier(schemaName) + ".", 1));
            }
        });
        StringBuilder ddl = new StringBuilder(ddlRef.get());
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
        String sql = "SHOW VARIABLES LIKE 'nls_characterset'";
        jdbcOperations.query(sql, t -> {
            tableOptions.setCharsetName(t.getString("VALUE"));
        });
    }

    protected void obtainTableCollation(DBTableOptions tableOptions) {
        String sql = "SHOW VARIABLES LIKE 'nls_sort'";
        jdbcOperations.query(sql, t -> {
            tableOptions.setCollationName(t.getString("VALUE"));
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
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBFunction getFunction(String schemaName, String functionName) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBProcedure getProcedure(String schemaName, String procedureName) {
        throw new UnsupportedOperationException("Not supported yet");
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

}
