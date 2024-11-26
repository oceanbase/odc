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
package com.oceanbase.odc.plugin.schema.obmysql.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBForeignKeyModifyRule;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.HashPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.KeyPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartition;
import com.oceanbase.tools.sqlparser.statement.createtable.ListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineCheckConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineForeignConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionOption;
import com.oceanbase.tools.sqlparser.statement.createtable.SubRangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/6/30
 * @since 4.2.0
 */
@Slf4j
public class OBMySQLGetDBTableByParser implements GetDBTableByParser {

    private final CreateTable createTableStmt;
    private static final char MYSQL_IDENTIFIER_WRAP_CHAR = '`';

    public OBMySQLGetDBTableByParser(@NonNull String tableDDL) {
        this.createTableStmt = parseTableDDL(tableDDL);
    }

    private CreateTable parseTableDDL(String ddl) {
        CreateTable statement = null;
        try {
            Statement value = SqlParser.parseMysqlStatement(ddl);
            if (value instanceof CreateTable) {
                statement = (CreateTable) value;
            }
        } catch (Exception e) {
            log.warn("Failed to parse table ddl, error message={}", e.getMessage());
        }
        return statement;
    }

    @Override
    public List<DBTableColumn> listColumns() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    /**
     * The original intention of this method is to solve the time-consuming problem of obtaining
     * constraint information by querying internal tables. But DBSchemaAccessor.listTableConstraints of
     * OB MySQL does not have performance issues, so this method is not currently called.
     */
    @Override
    public List<DBTableConstraint> listConstraints() {
        List<DBTableConstraint> constraints = new ArrayList<>();
        if (this.createTableStmt == null) {
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
                        constraint.setOrdinalPosition(i.get());
                        constraint.setTableName(removeIdentifiers(createTableStmt.getTableName()));
                        constraint.setColumnNames(
                                Collections.singletonList(
                                        removeIdentifiers(columnDefinition.getColumnReference().getColumn())));
                        constraint.setName(removeIdentifiers(item.getConstraintName()));
                        if (item.isPrimaryKey()) {
                            constraint.setType(DBConstraintType.PRIMARY_KEY);
                            constraint.setName("PRIMARY");
                        } else if (item.isUniqueKey()) {
                            constraint.setType(DBConstraintType.UNIQUE_KEY);
                        } else if (item.getNullable() != null && !item.getNullable()) {
                            constraint.setType(DBConstraintType.CHECK);
                            constraint
                                    .setCheckClause(columnDefinition.getColumnReference().getColumn() + " IS NOT NULL");
                        } else if (item instanceof InLineCheckConstraint) {
                            constraint.setType(DBConstraintType.CHECK);
                            constraint.setCheckClause(((InLineCheckConstraint) item).getCheckExpr().getText());
                        }
                        constraints.add(constraint);
                        i.getAndIncrement();
                    });
                }
            } else if (element instanceof OutOfLineConstraint) {
                DBTableConstraint constraint = new DBTableConstraint();
                constraint.setOrdinalPosition(i.get());
                constraint.setTableName(removeIdentifiers(createTableStmt.getTableName()));
                OutOfLineConstraint ofLineConstraint = (OutOfLineConstraint) element;
                constraint.setName(removeIdentifiers(ofLineConstraint.getConstraintName()));
                if (ofLineConstraint instanceof OutOfLineCheckConstraint) {
                    constraint.setType(DBConstraintType.CHECK);
                    constraint.setCheckClause(((OutOfLineCheckConstraint) ofLineConstraint).getCheckExpr().getText());
                } else if (ofLineConstraint instanceof OutOfLineForeignConstraint) {
                    OutOfLineForeignConstraint foreignConstraint = (OutOfLineForeignConstraint) ofLineConstraint;
                    constraint.setType(DBConstraintType.FOREIGN_KEY);
                    constraint.setReferenceSchemaName(removeIdentifiers(foreignConstraint.getReference().getSchema()));
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
                            : DBForeignKeyModifyRule.RESTRICT);
                    constraint.setOnUpdateRule(foreignConstraint.getReference().getUpdateOption() != null
                            ? DBForeignKeyModifyRule
                                    .fromValue(foreignConstraint.getReference().getUpdateOption().toString())
                            : DBForeignKeyModifyRule.RESTRICT);
                } else {
                    constraint.setColumnNames(
                            ofLineConstraint.getColumns().stream()
                                    .map(item -> removeIdentifiers(item.getColumn().getText())).collect(
                                            Collectors.toList()));
                    if (ofLineConstraint.isPrimaryKey()) {
                        constraint.setType(DBConstraintType.PRIMARY_KEY);
                        constraint.setName("PRIMARY");
                    } else if (ofLineConstraint.isUniqueKey()) {
                        constraint.setType(DBConstraintType.UNIQUE_KEY);
                        if (ofLineConstraint.getIndexName() != null) {
                            constraint.setName(removeIdentifiers(ofLineConstraint.getIndexName()));
                        }
                    }
                }
                constraints.add(constraint);
                i.getAndIncrement();
            }
        }
        return constraints;
    }

    private String removeIdentifiers(String str) {
        return StringUtils.unwrap(str, MYSQL_IDENTIFIER_WRAP_CHAR);
    }

    @Override
    public List<DBTableIndex> listIndexes() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public DBTablePartition getPartition() {
        DBTablePartition partition = new DBTablePartition();
        DBTablePartitionOption partitionOption = new DBTablePartitionOption();
        partitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        partition.setPartitionOption(partitionOption);
        List<DBTablePartitionDefinition> partitionDefinitions = new ArrayList<>();
        partition.setPartitionDefinitions(partitionDefinitions);

        if (Objects.isNull(createTableStmt)) {
            partition.setWarning("Failed to parse table ddl");
            return partition;
        }
        Partition partitionStmt = createTableStmt.getPartition();
        if (Objects.isNull(partitionStmt)) {
            return partition;
        }
        if (partitionStmt instanceof HashPartition) {
            parseHashPartitionStmt((HashPartition) partitionStmt, partition);
        } else if (partitionStmt instanceof KeyPartition) {
            parseKeyPartitionStmt((KeyPartition) partitionStmt, partition);
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
        fillSubPartitions(partition, partitionStmt);
        return partition;
    }

    private void fillSubPartitions(DBTablePartition partition, Partition partitionStmt) {
        if (partitionStmt.getSubPartitionOption() == null) {
            return;
        }
        DBTablePartition subPartition = new DBTablePartition();
        partition.setSubpartition(subPartition);
        DBTablePartitionOption subPartitionOption = new DBTablePartitionOption();
        subPartitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        subPartition.setPartitionOption(subPartitionOption);
        List<DBTablePartitionDefinition> subPartitionDefinitions = new ArrayList<>();
        subPartition.setPartitionDefinitions(subPartitionDefinitions);
        partition.setSubpartitionTemplated(partitionStmt.getSubPartitionOption().getTemplates() != null);
        String type = partitionStmt.getSubPartitionOption().getType();
        DBTablePartitionType subDBTablePartitionType = DBTablePartitionType.fromValue(type);
        if (DBTablePartitionType.NOT_PARTITIONED == subDBTablePartitionType) {
            partition.setWarning("not support this subpartition type, type: " + type);
            return;
        }
        subPartitionOption.setType(subDBTablePartitionType);
        SubPartitionOption parsedSubPartitionOption = partitionStmt.getSubPartitionOption();
        // When expressions are supported, only single partition keys are supported
        if (subDBTablePartitionType.supportExpression()) {
            Expression expression = parsedSubPartitionOption.getSubPartitionTargets().get(0);
            if (expression instanceof ColumnReference) {
                subPartitionOption.setColumnNames(Collections.singletonList(removeIdentifiers(expression.getText())));
            } else {
                subPartitionOption.setExpression(expression.getText());
            }
        } else {
            // When expressions are not supported, multiple columns are supported as partition keys
            subPartitionOption.setColumnNames(parsedSubPartitionOption.getSubPartitionTargets() == null ? null
                    : parsedSubPartitionOption.getSubPartitionTargets().stream()
                            .map(item -> removeIdentifiers(item.getText()))
                            .collect(Collectors.toList()));
        }
        /**
         * <pre>
         * subpartitionsNum indicates the number of subpartitions in each partition.
         * Therefore, subpartitionsNum should be configured only when the subpartitions are templated.
         * If subpartition is not templated, the subpartitionsNum is not fixed.
         * such as the following example of non-template subpartition table
         *
         * CREATE TABLE ranges_list (col1 INT,col2 INT)
         *        PARTITION BY RANGE COLUMNS(col1)
         *        SUBPARTITION BY LIST(col2)
         *        (PARTITION p0 VALUES LESS THAN(100)
         *          (SUBPARTITION sp0 VALUES IN(1,3),
         *           SUBPARTITION sp1 VALUES IN(4,6),
         *           SUBPARTITION sp2 VALUES IN(7,9)),
         *         PARTITION p1 VALUES LESS THAN(200)
         *          (SUBPARTITION sp3 VALUES IN(1,3))
         *        );
         *
         * In this case, the subpartitionsNum is not fixed.
         * </pre>
         */
        subPartitionOption
                .setPartitionsNum(
                        parsedSubPartitionOption.getTemplates() != null ? parsedSubPartitionOption.getTemplates().size()
                                : null);
        for (PartitionElement partitionElement : partitionStmt.getPartitionElements()) {
            if (partitionElement.getSubPartitionElements() != null) {
                // obtain DBTablePartitionDefinitions for non-templated subpartitions
                for (int i = 0; i < partitionElement.getSubPartitionElements().size(); i++) {
                    DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                    SubPartitionElement subPartitionElement = partitionElement.getSubPartitionElements().get(i);
                    fillSubPartitionValue(subDBTablePartitionType,subPartitionElement, partitionDefinition);
                    partitionDefinition.setName(
                            removeIdentifiers(subPartitionElement.getRelation()));
                    partitionDefinition.setOrdinalPosition(i);
                    partitionDefinition.setType(subDBTablePartitionType);
                    subPartitionDefinitions.add(partitionDefinition);
                }
            } else {
                // obtain DBTablePartitionDefinitions for templated subpartitions
                String parentPartitionName = removeIdentifiers(partitionElement.getRelation());
                List<SubPartitionElement> templates = partitionStmt.getSubPartitionOption().getTemplates();
                for (int i = 0; i < templates.size(); i++) {
                    DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                    SubPartitionElement subPartitionElement = templates.get(i);
                    fillSubPartitionValue(subDBTablePartitionType,subPartitionElement, partitionDefinition);
                    // for a templated subpartition table, the naming rule for the subpartition is
                    // '($part_name)s($subpart_name)'.
                    partitionDefinition.setName(
                            parentPartitionName + 's' + removeIdentifiers(subPartitionElement.getRelation()));
                    partitionDefinition.setOrdinalPosition(i);
                    partitionDefinition.setType(subDBTablePartitionType);
                    subPartitionDefinitions.add(partitionDefinition);
                }
            }
        }
    }

    private void fillSubPartitionValue(DBTablePartitionType subDBTablePartitionType, SubPartitionElement subPartitionElement,
        DBTablePartitionDefinition subPartitionDefinition) {
        if (subDBTablePartitionType == DBTablePartitionType.LIST) {
            SubListPartitionElement subListPartitionElement =
                    (SubListPartitionElement) subPartitionElement;
            List<List<String>> valuesList = new ArrayList<>();
            subListPartitionElement.getListExprs()
                    .forEach(item -> valuesList.add(Collections.singletonList(item.getText())));
            subPartitionDefinition.setValuesList(valuesList);
        } else if (subDBTablePartitionType == DBTablePartitionType.LIST_COLUMNS) {
            SubListPartitionElement subListPartitionElement =
                    (SubListPartitionElement) subPartitionElement;
            List<List<String>> valuesList = new ArrayList<>();
            for (Expression listExpr : subListPartitionElement.getListExprs()) {
                if (listExpr instanceof CollectionExpression) {
                    valuesList.add(
                            ((CollectionExpression) listExpr).getExpressionList().stream()
                                    .map(Expression::getText)
                                    .collect(Collectors.toList()));
                } else if (listExpr instanceof ConstExpression) {
                    valuesList.add(Collections.singletonList(listExpr.getText()));
                }
            }
            subPartitionDefinition.setValuesList(valuesList);
        } else if (subDBTablePartitionType == DBTablePartitionType.RANGE) {
            SubRangePartitionElement subRangePartitionElement =
                    (SubRangePartitionElement) subPartitionElement;
            subPartitionDefinition.setMaxValues(
                    Collections.singletonList(subRangePartitionElement.getRangeExprs().get(0).getText()));
        } else if (subDBTablePartitionType == DBTablePartitionType.RANGE_COLUMNS) {
            SubRangePartitionElement subRangePartitionElement =
                    (SubRangePartitionElement) subPartitionElement;
            subPartitionDefinition.setMaxValues(
                    subRangePartitionElement.getRangeExprs().stream().map(Expression::getText)
                            .collect(Collectors.toList()));
        }
    }

    private void parseHashPartitionStmt(HashPartition statement, DBTablePartition partition) {
        DBTablePartitionOption partitionOption = partition.getPartitionOption();
        List<DBTablePartitionDefinition> partitionDefinitions = partition.getPartitionDefinitions();
        Expression expression = statement.getPartitionTargets().get(0);
        if (expression instanceof ColumnReference) {
            partitionOption.setColumnNames(
                    statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                            .collect(Collectors.toList()));
        } else {
            partitionOption.setExpression(statement.getPartitionTargets().get(0).getText());
        }
        partitionOption.setType(DBTablePartitionType.HASH);
        if (statement.getPartitionElements().size() != 0) {
            int num = statement.getPartitionElements().size();
            partitionOption.setPartitionsNum(num);
            for (int i = 0; i < num; i++) {
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setName(removeIdentifiers(statement.getPartitionElements().get(i).getRelation()));
                partitionDefinition.setOrdinalPosition(i);
                partitionDefinition.setType(DBTablePartitionType.HASH);
                partitionDefinitions.add(partitionDefinition);
            }
        } else if (statement.getPartitionsNum() != null) {
            Integer num = statement.getPartitionsNum();
            partitionOption.setPartitionsNum(num);
            for (int i = 0; i < num; i++) {
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setName("p" + i);
                partitionDefinition.setOrdinalPosition(i);
                partitionDefinition.setType(DBTablePartitionType.HASH);
                partitionDefinitions.add(partitionDefinition);
            }
        }
    }

    private void parseKeyPartitionStmt(KeyPartition statement, DBTablePartition partition) {
        DBTablePartitionOption partitionOption = partition.getPartitionOption();
        List<DBTablePartitionDefinition> partitionDefinitions = partition.getPartitionDefinitions();
        partitionOption.setType(DBTablePartitionType.KEY);
        // Key partition may not specify the partition key
        if (statement.getPartitionTargets() != null) {
            partitionOption.setColumnNames(
                    statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                            .collect(Collectors.toList()));
        }
        if (statement.getPartitionElements().size() != 0) {
            int num = statement.getPartitionElements().size();
            partitionOption.setPartitionsNum(num);
            for (int i = 0; i < num; i++) {
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setName(removeIdentifiers(statement.getPartitionElements().get(i).getRelation()));
                partitionDefinition.setOrdinalPosition(i);
                partitionDefinition.setType(DBTablePartitionType.KEY);
                partitionDefinitions.add(partitionDefinition);
            }
        } else if (statement.getPartitionsNum() != null) {
            Integer num = statement.getPartitionsNum();
            partitionOption.setPartitionsNum(num);
            for (int i = 0; i < num; i++) {
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setName("p" + i);
                partitionDefinition.setOrdinalPosition(i);
                partitionDefinition.setType(DBTablePartitionType.KEY);
                partitionDefinitions.add(partitionDefinition);
            }
        }
    }

    private void parseRangePartitionStmt(RangePartition statement, DBTablePartition partition) {
        DBTablePartitionOption partitionOption = partition.getPartitionOption();
        List<DBTablePartitionDefinition> partitionDefinitions = partition.getPartitionDefinitions();
        int num = statement.getPartitionElements().size();
        partitionOption.setPartitionsNum(num);
        if (!statement.isColumns()) {
            partitionOption.setType(DBTablePartitionType.RANGE);
            if (statement.getPartitionTargets().get(0) instanceof ColumnReference) {
                partitionOption
                        .setColumnNames(
                                statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                                        .collect(Collectors.toList()));
            } else {
                partitionOption.setExpression((statement.getPartitionTargets().get(0).getText()));
            }
            for (int i = 0; i < num; i++) {
                RangePartitionElement element = (RangePartitionElement) statement.getPartitionElements().get(i);
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setOrdinalPosition(i);
                partitionDefinition.setMaxValues(Collections.singletonList(element.getRangeExprs().get(0).getText()));
                partitionDefinition.setName(removeIdentifiers(element.getRelation()));
                partitionDefinition.setType(DBTablePartitionType.RANGE);
                partitionDefinitions.add(partitionDefinition);
            }
        } else {
            partitionOption.setType(DBTablePartitionType.RANGE_COLUMNS);
            partitionOption.setColumnNames(
                    statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                            .collect(Collectors.toList()));
            for (int i = 0; i < num; i++) {
                RangePartitionElement element = (RangePartitionElement) statement.getPartitionElements().get(i);
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setOrdinalPosition(i);
                partitionDefinition.setMaxValues(
                        element.getRangeExprs().stream().map(Expression::getText).collect(Collectors.toList()));
                partitionDefinition.setName(removeIdentifiers(element.getRelation()));
                partitionDefinition.setType(DBTablePartitionType.RANGE_COLUMNS);
                partitionDefinitions.add(partitionDefinition);
            }
        }
    }

    private void parseListPartitionStmt(ListPartition statement, DBTablePartition partition) {
        DBTablePartitionOption partitionOption = partition.getPartitionOption();
        List<DBTablePartitionDefinition> partitionDefinitions = partition.getPartitionDefinitions();
        int num = statement.getPartitionElements().size();
        partitionOption.setPartitionsNum(num);
        if (!statement.isColumns()) {
            partitionOption.setType(DBTablePartitionType.LIST);
            if (statement.getPartitionTargets().get(0) instanceof ColumnReference) {
                partitionOption
                        .setColumnNames(
                                statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                                        .collect(Collectors.toList()));
            } else {
                partitionOption.setExpression((statement.getPartitionTargets().get(0).getText()));
            }
            for (int i = 0; i < num; i++) {
                ListPartitionElement element = (ListPartitionElement) statement.getPartitionElements().get(i);
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setOrdinalPosition(i);
                List<List<String>> valuesList = new ArrayList<>();
                element.getListExprs().forEach(item -> valuesList.add(Collections.singletonList(item.getText())));
                partitionDefinition.setValuesList(valuesList);
                partitionDefinition.setName(removeIdentifiers(element.getRelation()));
                partitionDefinition.setType(DBTablePartitionType.LIST);
                partitionDefinitions.add(partitionDefinition);
            }
        } else {
            partitionOption.setType(DBTablePartitionType.LIST_COLUMNS);
            partitionOption.setColumnNames(
                    statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                            .collect(Collectors.toList()));
            for (int i = 0; i < num; i++) {
                ListPartitionElement element = (ListPartitionElement) statement.getPartitionElements().get(i);
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setOrdinalPosition(i);
                List<List<String>> valuesList = new ArrayList<>();
                for (Expression listExpr : element.getListExprs()) {
                    if (listExpr instanceof CollectionExpression) {
                        valuesList.add(
                                ((CollectionExpression) listExpr).getExpressionList().stream().map(Expression::getText)
                                        .collect(Collectors.toList()));
                    } else if (listExpr instanceof ConstExpression) {
                        valuesList.add(Collections.singletonList(listExpr.getText()));
                    }
                }
                partitionDefinition.setValuesList(valuesList);
                partitionDefinition.setName(removeIdentifiers(element.getRelation()));
                partitionDefinition.setType(DBTablePartitionType.LIST_COLUMNS);
                partitionDefinitions.add(partitionDefinition);
            }
        }
    }

}
