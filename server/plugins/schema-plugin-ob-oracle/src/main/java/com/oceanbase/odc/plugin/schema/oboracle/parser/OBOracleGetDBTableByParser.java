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
package com.oceanbase.odc.plugin.schema.oboracle.parser;

import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.schema.obmysql.parser.GetDBTableByParser;
import com.oceanbase.tools.dbbrowser.model.DBConstraintDeferability;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBForeignKeyModifyRule;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.SQLParser;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/6/30
 * @since 4.2.0
 */
@Slf4j
public class OBOracleGetDBTableByParser implements GetDBTableByParser {
    @Getter
    private final CreateTable createTableStmt;
    private final Connection connection;
    private final String schemaName;
    private final String tableName;
    private final char ORACLE_IDENTIFIER_WRAP_CHAR = '"';
    private List<DBTableConstraint> constraints = new ArrayList<>();
    private List<DBTableIndex> indexes = new ArrayList<>();
    @Getter
    private Map<String, String> indexName2Ddl = new HashMap<>();

    public OBOracleGetDBTableByParser(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull String tableName) {
        this.connection = connection;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.createTableStmt = parseTableDDL(schemaName, tableName);
    }

    private CreateTable parseTableDDL(@NonNull String schemaName, @NonNull String tableName) {
        CreateTable statement = null;
        OracleSqlBuilder getTableDDLSql = new OracleSqlBuilder();
        getTableDDLSql.append("SELECT dbms_metadata.get_ddl('TABLE', ");
        getTableDDLSql.value(tableName);
        getTableDDLSql.append(", ");
        getTableDDLSql.value(schemaName);
        getTableDDLSql.append(") as DDL from dual");
        String ddl = JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(getTableDDLSql.toString(),
                String.class);
        try {
            SQLParser sqlParser = new OBOracleSQLParser();
            statement = (CreateTable) sqlParser.parse(new StringReader(ddl));
        } catch (Exception e) {
            log.warn("Failed to parse table ddl, error message={}", e.getMessage());
        }
        return statement;
    }

    @Override
    public List<DBTableColumn> listColumns() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBTableConstraint> listConstraints() {
        if (this.constraints.size() > 0 || this.createTableStmt == null) {
            return constraints;
        }
        AtomicInteger i = new AtomicInteger();
        for (TableElement element : createTableStmt.getTableElements()) {
            if (element instanceof ColumnDefinition) {
                ColumnDefinition columnDefinition = (ColumnDefinition) element;
                if (columnDefinition.getColumnAttributes() != null
                        && columnDefinition.getColumnAttributes().getConstraints() != null) {
                    List<InLineConstraint> cons = columnDefinition.getColumnAttributes().getConstraints();
                    cons.forEach(item -> {
                        DBTableConstraint constraint = new DBTableConstraint();
                        if (Objects.nonNull(item.getConstraintName())) {
                            constraint.setName(removeIdentifiers(item.getConstraintName()));
                        }
                        constraint.setOrdinalPosition(i.get());
                        constraint.setTableName(removeIdentifiers(createTableStmt.getTableName()));
                        constraint.setColumnNames(
                                Collections.singletonList(
                                        removeIdentifiers(columnDefinition.getColumnReference().getColumn())));
                        /**
                         * Ob oracle do not support change constraint's DEFERRABLE and DEFERRED attribute, so
                         * DBConstraintDeferability will always be "NOT DEFERRABLE"
                         */
                        constraint.setDeferability(DBConstraintDeferability.NOT_DEFERRABLE);
                        constraint.setEnabled(
                                item.getState() == null || item.getState().getEnable() == null
                                        || item.getState().getEnable());
                        constraint.setValidate(
                                item.getState() == null || item.getState().getValidate() == null
                                        || item.getState().getValidate());
                        if (item instanceof InLineCheckConstraint) {
                            constraint.setType(DBConstraintType.CHECK);
                            constraint.setCheckClause(((InLineCheckConstraint) item).getCheckExpr().getText());
                        } else if (item instanceof InLineForeignConstraint) {
                            InLineForeignConstraint foreignConstraint = (InLineForeignConstraint) item;
                            constraint.setType(DBConstraintType.FOREIGN_KEY);
                            constraint.setReferenceSchemaName(
                                    removeIdentifiers(foreignConstraint.getReference().getSchema()));
                            constraint.setReferenceTableName(
                                    removeIdentifiers(foreignConstraint.getReference().getRelation()));
                            constraint
                                    .setReferenceColumnNames(foreignConstraint.getReference().getColumns().stream()
                                            .map(col -> removeIdentifiers(col.getColumn()))
                                            .collect(Collectors.toList()));
                            constraint.setOnDeleteRule(foreignConstraint.getReference().getDeleteOption() != null
                                    ? DBForeignKeyModifyRule
                                            .fromValue(foreignConstraint.getReference().getDeleteOption().toString())
                                    : DBForeignKeyModifyRule.CASCADE);
                        } else {
                            if (item.isPrimaryKey()) {
                                constraint.setType(DBConstraintType.PRIMARY_KEY);
                            } else if (item.isUniqueKey()) {
                                constraint.setType(DBConstraintType.UNIQUE_KEY);
                            } else if (item.getNullable() != null && !item.getNullable()) {
                                constraint.setType(DBConstraintType.CHECK);
                                constraint.setCheckClause(
                                        "\"" + removeIdentifiers(columnDefinition.getColumnReference().getColumn())
                                                + "\" IS NOT NULL");
                            }
                        }
                        constraints.add(constraint);
                        i.getAndIncrement();
                    });
                }
            } else if (element instanceof OutOfLineConstraint) {
                DBTableConstraint constraint = new DBTableConstraint();
                constraint.setOrdinalPosition(i.get());
                constraint.setTableName(removeIdentifiers(createTableStmt.getTableName()));
                constraint.setDeferability(DBConstraintDeferability.NOT_DEFERRABLE);
                OutOfLineConstraint outOfLineConstraint = (OutOfLineConstraint) element;
                if (Objects.nonNull(outOfLineConstraint.getConstraintName())) {
                    constraint.setName(removeIdentifiers(outOfLineConstraint.getConstraintName()));
                }
                constraint.setEnabled(
                        outOfLineConstraint.getState() == null || outOfLineConstraint.getState().getEnable() == null
                                || outOfLineConstraint.getState().getEnable());
                constraint.setValidate(
                        outOfLineConstraint.getState() == null || outOfLineConstraint.getState().getValidate() == null
                                || outOfLineConstraint.getState().getValidate());
                if (outOfLineConstraint instanceof OutOfLineCheckConstraint) {
                    constraint.setType(DBConstraintType.CHECK);
                    constraint
                            .setCheckClause(((OutOfLineCheckConstraint) outOfLineConstraint).getCheckExpr().getText());
                } else if (outOfLineConstraint instanceof OutOfLineForeignConstraint) {
                    OutOfLineForeignConstraint foreignConstraint = (OutOfLineForeignConstraint) outOfLineConstraint;
                    constraint.setType(DBConstraintType.FOREIGN_KEY);
                    if (foreignConstraint.getReference().getSchema() != null) {
                        constraint.setReferenceSchemaName(
                                removeIdentifiers(foreignConstraint.getReference().getSchema()));
                    }
                    constraint.setColumnNames(
                            foreignConstraint.getColumns().stream()
                                    .map(item -> removeIdentifiers(item.getColumn().getText())).collect(
                                            Collectors.toList()));
                    constraint.setReferenceTableName(removeIdentifiers(foreignConstraint.getReference().getRelation()));
                    constraint.setReferenceColumnNames(foreignConstraint.getReference().getColumns().stream()
                            .map(col -> removeIdentifiers(col.getColumn())).collect(Collectors.toList()));
                    constraint.setOnDeleteRule(foreignConstraint.getReference().getDeleteOption() != null
                            ? DBForeignKeyModifyRule
                                    .fromValue(foreignConstraint.getReference().getDeleteOption().toString())
                            : DBForeignKeyModifyRule.CASCADE);
                } else {
                    if (outOfLineConstraint.isPrimaryKey()) {
                        constraint.setType(DBConstraintType.PRIMARY_KEY);
                    } else if (outOfLineConstraint.isUniqueKey()) {
                        constraint.setType(DBConstraintType.UNIQUE_KEY);
                    }
                    constraint.setColumnNames(
                            outOfLineConstraint.getColumns().stream()
                                    .map(item -> removeIdentifiers(item.getColumn().getText())).collect(
                                            Collectors.toList()));
                }
                constraints.add(constraint);
                i.getAndIncrement();
            }
        }
        return constraints;
    }

    private String removeIdentifiers(String str) {
        if (Objects.isNull(str)) {
            return "";
        }
        if (str.charAt(0) == ORACLE_IDENTIFIER_WRAP_CHAR
                && str.charAt(str.length() - 1) == ORACLE_IDENTIFIER_WRAP_CHAR) {
            return StringUtils.unwrap(str, ORACLE_IDENTIFIER_WRAP_CHAR);
        }
        return str.toUpperCase();
    }

    @Override
    public List<DBTableIndex> listIndexes() {
        if (this.indexes.size() > 0) {
            return this.indexes;
        }
        OracleSqlBuilder sb = new OracleSqlBuilder();
        sb.append("select INDEX_NAME, VISIBILITY, STATUS from ALL_INDEXES where OWNER = ")
                .value(schemaName)
                .append(" AND TABLE_NAME = ")
                .value(tableName);
        // Query all index names belonging to this table.
        this.indexes = JdbcOperationsUtil.getJdbcOperations(connection).query(sb.toString(),
                (rs, num) -> {
                    DBTableIndex idx = new DBTableIndex();
                    idx.setOrdinalPosition(num);
                    idx.setSchemaName(schemaName);
                    idx.setTableName(tableName);
                    idx.setName(rs.getString("INDEX_NAME"));
                    idx.setVisible("VISIBLE".equals(rs.getString("VISIBILITY")));
                    idx.setAvailable("VALID".equals(rs.getString("STATUS")));
                    return idx;
                });
        /**
         * The ddl of the primary key can not be obtained through dbms_metadata.get_ddl(), we get columns
         * list of primary key by parse table ddl.
         */
        List<DBTableConstraint> priConstraint = listConstraints().stream().filter(
                constraint -> constraint.getType().equals(DBConstraintType.PRIMARY_KEY)).collect(Collectors.toList());
        String primaryKeyName = null;
        if (!priConstraint.isEmpty()) {
            if (Objects.nonNull(priConstraint.get(0).getName())) {
                primaryKeyName = priConstraint.get(0).getName();
            } else {
                /**
                 * The name of the primary key may not be obtained from the ddl of the table, in this case, we get
                 * primary key name by querying ALL_CONSTRAINTS
                 */
                OracleSqlBuilder getPrimaryKeyName = new OracleSqlBuilder();
                getPrimaryKeyName.append("SELECT CONSTRAINT_NAME FROM ALL_CONSTRAINTS WHERE OWNER=")
                        .value(schemaName)
                        .append(" AND TABLE_NAME=")
                        .value(tableName)
                        .append(" AND CONSTRAINT_TYPE='P'");
                primaryKeyName = JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(
                        getPrimaryKeyName.toString(),
                        String.class);
            }
        }

        for (DBTableIndex idx : this.indexes) {
            // ob oracle only support btree algorithm.
            idx.setAlgorithm(DBIndexAlgorithm.BTREE);
            if (Objects.nonNull(primaryKeyName) && primaryKeyName.equals(idx.getName())) {
                idx.setType(DBIndexType.UNIQUE);
                idx.setPrimary(true);
                idx.setUnique(true);
                idx.setGlobal(true);
                idx.setColumnNames(
                        priConstraint.get(0).getColumnNames().stream().map(item -> removeIdentifiers(item)).collect(
                                Collectors.toList()));
            } else {
                // Get index type、global/local attribute and column names by parse index ddl.
                String getIndexDDLSql = "SELECT dbms_metadata.get_ddl('INDEX', '" + idx.getName() + "', '" + schemaName
                        + "') as DDL from dual";
                JdbcOperationsUtil.getJdbcOperations(connection).query(getIndexDDLSql, (rs) -> {
                    String idxDDL = rs.getString("DDL");
                    this.indexName2Ddl.put(idx.getName(), idxDDL);
                    CreateIndex createIndexStmt = parseIndexDDL(idxDDL);
                    idx.setGlobal(
                            Objects.nonNull(createIndexStmt.getIndexOptions()) ? createIndexStmt.getIndexOptions()
                                    .getGlobal()
                                    : null);
                    if (createIndexStmt.getColumns().stream().allMatch(
                            item -> item.getColumn() instanceof RelationReference)) {
                        if (createIndexStmt.isUnique()) {
                            idx.setType(DBIndexType.UNIQUE);
                            idx.setPrimary(false);
                            idx.setUnique(true);
                        } else {
                            idx.setType(DBIndexType.NORMAL);
                            idx.setPrimary(false);
                            idx.setUnique(false);
                        }
                        idx.setColumnNames(createIndexStmt.getColumns().stream()
                                .map(item -> removeIdentifiers(item.getColumn().getText())).collect(
                                        Collectors.toList()));
                    } else {
                        /**
                         * The enumeration values of the INDEX_TYPE field of the internal table ALL_INDEXES are: NORMAL,
                         * FUNCTION-BASED NORMAL
                         */
                        idx.setType(DBIndexType.FUNCTION_BASED_NORMAL);
                        idx.setPrimary(false);
                        idx.setUnique(false);
                        idx.setColumnNames(createIndexStmt.getColumns().stream()
                                .map(item -> item.getColumn().getText()).collect(Collectors.toList()));
                    }
                });
            }
        }
        return this.indexes;
    }

    private CreateIndex parseIndexDDL(String ddl) {
        CreateIndex statement = null;
        try {
            SQLParser sqlParser = new OBOracleSQLParser();
            statement = (CreateIndex) sqlParser.parse(new StringReader(ddl));
        } catch (Exception e) {
            log.warn("Failed to parse ob oracle index ddl, error message={}", e.getMessage());
        }
        return statement;
    }

    @Override
    public DBTablePartition getPartition() {
        DBTablePartition partition = new DBTablePartition();
        partition.setPartitionOption(new DBTablePartitionOption());
        partition.setPartitionDefinitions(new ArrayList<>());

        if (Objects.isNull(createTableStmt)) {
            partition.setWarning("Failed to parse ob oracle table ddl");
            return partition;
        }
        Partition partitionStmt = createTableStmt.getPartition();
        if (Objects.isNull(partitionStmt)) {
            return partition;
        }
        if (partitionStmt instanceof HashPartition) {
            parseHashPartitionStmt((HashPartition) partitionStmt, partition);
        } else if (partitionStmt instanceof RangePartition) {
            parseRangePartitionStmt((RangePartition) partitionStmt, partition);
        } else if (partitionStmt instanceof ListPartition) {
            parseListPartitionStmt((ListPartition) partitionStmt, partition);
        }

        /**
         * In order to adapt to the front-end only the expression field is used for Hash、List and Range
         * partition types
         */
        if (Objects.nonNull(partition.getPartitionOption().getType())
                && partition.getPartitionOption().getType().supportExpression()
                && StringUtils.isBlank(partition.getPartitionOption().getExpression())) {
            List<String> columnNames = partition.getPartitionOption().getColumnNames();
            if (!columnNames.isEmpty()) {
                partition.getPartitionOption().setExpression(String.join(", ", columnNames));
            }
        }
        return partition;
    }

    private void parseHashPartitionStmt(HashPartition statement, DBTablePartition partition) {
        DBTablePartitionOption option = partition.getPartitionOption();
        option.setType(DBTablePartitionType.HASH);
        option.setColumnNames(
                statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                        .collect(Collectors.toList()));
        if (statement.getPartitionElements() != null) {
            int num = statement.getPartitionElements().size();
            option.setPartitionsNum(num);
            for (int i = 0; i < num; i++) {
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setName(removeIdentifiers(statement.getPartitionElements().get(i).getRelation()));
                partitionDefinition.setOrdinalPosition(i);
                partitionDefinition.setType(DBTablePartitionType.HASH);
                partition.getPartitionDefinitions().add(partitionDefinition);
            }
        } else if (statement.getPartitionsNum() != null) {
            Integer num = statement.getPartitionsNum();
            option.setPartitionsNum(num);
            for (int i = 0; i < num; i++) {
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setName("p" + i);
                partitionDefinition.setOrdinalPosition(i);
                partitionDefinition.setType(DBTablePartitionType.HASH);
                partition.getPartitionDefinitions().add(partitionDefinition);
            }
        }
    }

    private void parseRangePartitionStmt(RangePartition statement, DBTablePartition partition) {
        DBTablePartitionOption option = partition.getPartitionOption();
        int num = statement.getPartitionElements().size();
        option.setPartitionsNum(num);
        option.setType(DBTablePartitionType.RANGE);
        option.setColumnNames(
                statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                        .collect(Collectors.toList()));
        for (int i = 0; i < num; i++) {
            RangePartitionElement element = (RangePartitionElement) statement.getPartitionElements().get(i);
            DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
            partitionDefinition.setOrdinalPosition(i);
            partitionDefinition.setMaxValues(
                    element.getRangeExprs().stream().map(Expression::getText).collect(Collectors.toList()));
            partitionDefinition.setName(removeIdentifiers(element.getRelation()));
            partitionDefinition.setType(DBTablePartitionType.RANGE);
            partition.getPartitionDefinitions().add(partitionDefinition);
        }
    }

    private void parseListPartitionStmt(ListPartition statement, DBTablePartition partition) {
        DBTablePartitionOption option = partition.getPartitionOption();
        int num = statement.getPartitionElements().size();
        option.setPartitionsNum(num);
        option.setType(DBTablePartitionType.LIST);
        option.setColumnNames(
                statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                        .collect(Collectors.toList()));
        for (int i = 0; i < num; i++) {
            ListPartitionElement element = (ListPartitionElement) statement.getPartitionElements().get(i);
            DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
            partitionDefinition.setOrdinalPosition(i);
            List<List<String>> valuesList = new ArrayList<>();
            element.getListExprs().forEach(item -> valuesList.add(Collections.singletonList(item.getText())));
            partitionDefinition.setValuesList(valuesList);
            partitionDefinition.setName(removeIdentifiers(element.getRelation()));
            partitionDefinition.setType(DBTablePartitionType.LIST);
            partition.getPartitionDefinitions().add(partitionDefinition);
        }
    }

}
