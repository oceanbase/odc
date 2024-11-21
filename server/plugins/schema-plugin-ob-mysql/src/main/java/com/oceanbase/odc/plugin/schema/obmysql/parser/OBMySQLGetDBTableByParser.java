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
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartition;
import com.oceanbase.tools.sqlparser.statement.createtable.RangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionOption;
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

    /**
     * 获取数据库表的分区信息
     *
     * @return DBTablePartition对象，包含表的分区信息
     */
    @Override
    public DBTablePartition getPartition() {
        // 创建DBTablePartition对象和子分区对象
        DBTablePartition partition = new DBTablePartition();
        DBTablePartition subPartition = new DBTablePartition();
        partition.setSubpartition(subPartition);

        // 设置分区选项和子分区选项
        DBTablePartitionOption partitionOption = new DBTablePartitionOption();
        partitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        partition.setPartitionOption(partitionOption);
        DBTablePartitionOption subPartitionOption = new DBTablePartitionOption();
        subPartitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        subPartition.setPartitionOption(subPartitionOption);

        // 初始化分区定义列表和子分区定义列表
        List<DBTablePartitionDefinition> partitionDefinitions = new ArrayList<>();
        partition.setPartitionDefinitions(partitionDefinitions);
        List<DBTablePartitionDefinition> subPartitionDefinitions = new ArrayList<>();
        subPartition.setPartitionDefinitions(subPartitionDefinitions);

        // 如果createTableStmt为空，则设置警告信息并返回partition对象
        if (Objects.isNull(createTableStmt)) {
            partition.setWarning("Failed to parse table ddl");
            return partition;
        }
        // 获取分区语句
        Partition partitionStmt = createTableStmt.getPartition();
        if (Objects.isNull(partitionStmt)) {
            return partition;
        }
        // 根据不同的分区类型解析分区语句
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
        // 判断分区选项的类型是否支持表达式，并且表达式为空
        if (Objects.nonNull(partition.getPartitionOption().getType())
            && partition.getPartitionOption().getType().supportExpression()
            && StringUtils.isBlank(partition.getPartitionOption().getExpression())) {
            // 获取列名列表
            List<String> columnNames = partition.getPartitionOption().getColumnNames();
            // 如果列名列表不为空
            if (!columnNames.isEmpty()) {
                // 将列名列表中的元素用逗号连接起来，作为表达式
                partition.getPartitionOption().setExpression(String.join(", ", columnNames));
            }
        }
        // 如果子分区选项为空，则直接返回partition对象
        if (partitionStmt.getSubPartitionOption() == null) {
            return partition;
        }
        // TODO 目前 ODC 仅支持 HASH/KEY 二级分区, 其它类型后续需补充
        // 设置分区是否为子分区模板
        partition.setSubpartitionTemplated(partitionStmt.getSubPartitionOption().getTemplates() != null);
        // 获取子分区选项
        SubPartitionOption subOption = partitionStmt.getSubPartitionOption();
        // 获取子分区类型
        String type = partitionStmt.getSubPartitionOption().getType();
        if ("key".equals(type.toLowerCase())) {
            // 如果子分区类型为KEY
            subPartitionOption.setType(DBTablePartitionType.KEY);
            // 设置子分区列名
            subPartitionOption.setColumnNames(subOption.getSubPartitionTargets() == null ? null
                : subOption.getSubPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                    .collect(Collectors.toList()));
            // 设置子分区数量
            subPartitionOption
                .setPartitionsNum(partition.getSubpartitionTemplated() ? subOption.getTemplates().size()
                    : partitionStmt.getPartitionElements().get(0).getSubPartitionElements().size());
        } else if ("hash".equals(type.toLowerCase())) {
            // 如果子分区类型为HASH
            subPartitionOption.setType(DBTablePartitionType.HASH);
            Expression expression = subOption.getSubPartitionTargets().get(0);
            if (expression instanceof ColumnReference) {
                // 如果子分区表达式为列引用
                subPartitionOption.setColumnNames(Collections.singletonList(removeIdentifiers(expression.getText())));
            } else {
                // 如果子分区表达式不为列引用
                subPartitionOption.setExpression(expression.getText());
            }
            // 设置子分区数量
            subPartitionOption
                .setPartitionsNum(partition.getSubpartitionTemplated() ? subOption.getTemplates().size()
                    : partitionStmt.getPartitionElements().get(0).getSubPartitionElements().size());
        } else {
            // 如果子分区类型不为HASH或KEY
            partition.setWarning("Only support HASH/KEY subpartition currently");
        }
        return partition;
    }

    /**
     * 解析哈希分区语句
     *
     * @param statement 哈希分区语句
     * @param partition 数据库表分区
     */
    private void parseHashPartitionStmt(HashPartition statement, DBTablePartition partition) {
        // 获取返回值中的一级分区选项和一级分区定义列表
        DBTablePartitionOption partitionOption = partition.getPartitionOption();
        List<DBTablePartitionDefinition> partitionDefinitions = partition.getPartitionDefinitions();
        // 获取解析出来的一级分区表达式
        Expression expression = statement.getPartitionTargets().get(0);
        if (expression instanceof ColumnReference) {
            // 如果一级分区表达式是列引用，则设置列名
            partitionOption.setColumnNames(
                statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                    .collect(Collectors.toList()));
        } else {
            // 否则设置分区表达式
            partitionOption.setExpression(statement.getPartitionTargets().get(0).getText());
        }
        // 设置一级分区类型为哈希
        partitionOption.setType(DBTablePartitionType.HASH);
        if (statement.getPartitionElements().size() != 0) {
            // 如果存在一级分区元素，则设置分区数量和分区定义
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
            // 如果不存在分区元素但设置了分区数量，则设置分区数量和分区定义
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

    /**
     * 解析范围分区语句
     *
     * @param statement 范围分区语句
     * @param partition 数据库表分区
     */
    private void parseRangePartitionStmt(RangePartition statement, DBTablePartition partition) {
        // 获取返回值DBTablePartition中一级分区选项和一级分区定义列表
        DBTablePartitionOption partitionOption = partition.getPartitionOption();
        List<DBTablePartitionDefinition> partitionDefinitions = partition.getPartitionDefinitions();
        // 获取一级分区的数量
        int num = statement.getPartitionElements().size();
        // 响应结果中设置一级分区数量
        partitionOption.setPartitionsNum(num);
        // 判断一级分区是否是Range Columns 分区
        if (!statement.isColumns()) {
            // 获取一级分区类型
            partitionOption.setType(DBTablePartitionType.RANGE);
            // 获取一级分区分区键
            if (statement.getPartitionTargets().get(0) instanceof ColumnReference) {
                // 获取一级分区列名
                partitionOption
                    .setColumnNames(
                        statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                            .collect(Collectors.toList()));
            } else {
                // 获取一级分区表达式
                partitionOption.setExpression((statement.getPartitionTargets().get(0).getText()));
            }
            // 为每个一级分区构造分区定义DBTablePartitionDefinition
            for (int i = 0; i < num; i++) {
                RangePartitionElement element = (RangePartitionElement) statement.getPartitionElements().get(i);
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                partitionDefinition.setOrdinalPosition(i);
                // 获取一级分区最大值
                partitionDefinition.setMaxValues(Collections.singletonList(element.getRangeExprs().get(0).getText()));
                // 获取一级分区名称，类型
                partitionDefinition.setName(removeIdentifiers(element.getRelation()));
                partitionDefinition.setType(DBTablePartitionType.RANGE);
                partitionDefinitions.add(partitionDefinition);
            }
        } else {
            // 设置一级分区类型为范围列分区
            partitionOption.setType(DBTablePartitionType.RANGE_COLUMNS);
            // 设置一级分区包含的所有列名
            partitionOption.setColumnNames(
                statement.getPartitionTargets().stream().map(item -> removeIdentifiers(item.getText()))
                    .collect(Collectors.toList()));
            // 为每个一级分区构造分区定义DBTablePartitionDefinition
            for (int i = 0; i < num; i++) {
                RangePartitionElement element = (RangePartitionElement) statement.getPartitionElements().get(i);
                DBTablePartitionDefinition partitionDefinition = new DBTablePartitionDefinition();
                //设置一级分区的顺序
                partitionDefinition.setOrdinalPosition(i);
                //设置一级分区的最大值，可能有多个
                partitionDefinition.setMaxValues(
                    element.getRangeExprs().stream().map(Expression::getText).collect(Collectors.toList()));
                // 设置分区名称，类型
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
