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
package com.oceanbase.odc.service.datasecurity.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.service.datasecurity.accessor.ColumnAccessor;
import com.oceanbase.odc.service.datasecurity.extractor.model.ColumnType;
import com.oceanbase.odc.service.datasecurity.extractor.model.LogicalColumn;
import com.oceanbase.odc.service.datasecurity.extractor.model.LogicalTable;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.expression.CaseWhen;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;
import com.oceanbase.tools.sqlparser.statement.expression.CompoundExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ExpressionParam;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionCall;
import com.oceanbase.tools.sqlparser.statement.expression.FunctionParam;
import com.oceanbase.tools.sqlparser.statement.expression.IntervalExpression;
import com.oceanbase.tools.sqlparser.statement.expression.RelationReference;
import com.oceanbase.tools.sqlparser.statement.select.ExpressionReference;
import com.oceanbase.tools.sqlparser.statement.select.FromReference;
import com.oceanbase.tools.sqlparser.statement.select.JoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.JoinReference;
import com.oceanbase.tools.sqlparser.statement.select.NameReference;
import com.oceanbase.tools.sqlparser.statement.select.Projection;
import com.oceanbase.tools.sqlparser.statement.select.Select;
import com.oceanbase.tools.sqlparser.statement.select.SelectBody;
import com.oceanbase.tools.sqlparser.statement.select.UsingJoinCondition;
import com.oceanbase.tools.sqlparser.statement.select.WithTable;

/**
 * 
 * @author gaoda.xy
 * @date 2023/6/6 10:32
 */
public class OBColumnExtractor implements ColumnExtractor {

    private final DialectType dialectType;
    private final String currentDatabase;
    private final ColumnAccessor columnAccessor;

    private final LinkedList<LogicalTable> fromTables = new LinkedList<>();
    private final LinkedList<LogicalTable> cteTables = new LinkedList<>();

    private static final String COLUMN_NAME_WILDCARD = "*";
    private static final Set<String> ORACLE_PSEUDO_COLUMNS = new HashSet<String>() {
        {
            add("CONNECT_BY_ISCYCLE");
            add("CONNECT_BY_ISLEAF");
            add("ORA_ROWSCN");
            add("ROWNUM");
            add("ROWID");
            add("LEVEL");
        }
    };
    private static final Set<String> ORACLE_PSEUDO_TABLES = new HashSet<String>() {
        {
            add("DUAL");
        }
    };
    private static final Set<String> MYSQL_PSEUDO_TABLES = new HashSet<String>() {
        {
            add("dual");
        }
    };

    public OBColumnExtractor(DialectType dialectType, String currentDatabase, ColumnAccessor columnAccessor) {
        this.dialectType = dialectType;
        this.currentDatabase = currentDatabase;
        this.columnAccessor = columnAccessor;
    }

    @Override
    public LogicalTable extract(Statement statement) {
        if (!(statement instanceof Select)) {
            return null;
        }
        return extractSelect((Select) statement);
    }

    private LogicalTable extractSelect(Select select) {
        // TODO: May deal with ORDER BY clause?
        return extractSelectBody(select.getSelectBody());
    }

    private LogicalTable extractSelectBody(SelectBody selectBody) {
        // TODO: May deal with WHERE / HAVING clause?
        LogicalTable result = LogicalTable.empty();
        if (Objects.isNull(selectBody)) {
            return result;
        }
        selectBody.getWith().forEach(with -> cteTables.addFirst(extractWithTable(with, selectBody.isRecursive())));
        List<FromReference> fromRefs = selectBody.getFroms();
        LogicalTable fromTable = extractFromReferences(fromRefs);
        fromTables.addFirst(fromTable);
        List<Projection> selectItems = selectBody.getSelectItems();
        result = extractSelectItems(selectItems);
        fromTables.removeFirst();
        result.setName(selectBody.getText());
        if (Objects.nonNull(selectBody.getRelatedSelect())) {
            result = union(result, extractSelectBody(selectBody.getRelatedSelect().getSelect()));
        }
        selectBody.getWith().forEach(with -> cteTables.removeFirst());
        return result;
    }

    private LogicalTable extractWithTable(WithTable withTable, boolean recursive) {
        LogicalTable result;
        if (!recursive) {
            result = extractSelectBody(withTable.getSelect());
            result.setName(processIdentifier(withTable.getRelation()));
            if (CollectionUtils.isNotEmpty(withTable.getAliasList())) {
                Verify.equals(result.getColumnList().size(), withTable.getAliasList().size(), "aliasList.size");
                for (int i = 0; i < result.getColumnList().size(); i++) {
                    result.getColumnList().get(i).setAlias(processIdentifier(withTable.getAliasList().get(i)));
                }
            }
        } else {
            result = LogicalTable.empty();
            SelectBody selectBody = withTable.getSelect();
            selectBody.getWith().forEach(with -> cteTables.addFirst(extractWithTable(with, selectBody.isRecursive())));
            List<FromReference> fromRefs = selectBody.getFroms();
            LogicalTable fromTable = extractFromReferences(fromRefs);
            fromTables.addFirst(fromTable);
            List<Projection> selectItems = selectBody.getSelectItems();
            LogicalTable cteTable = extractSelectItems(selectItems);
            fromTables.removeFirst();
            cteTable.setName(processIdentifier(withTable.getRelation()));
            if (CollectionUtils.isNotEmpty(withTable.getAliasList())) {
                Verify.equals(cteTable.getColumnList().size(), withTable.getAliasList().size(), "aliasList.size");
                for (int i = 0; i < cteTable.getColumnList().size(); i++) {
                    cteTable.getColumnList().get(i).setAlias(processIdentifier(withTable.getAliasList().get(i)));
                }
            }
            cteTables.addFirst(cteTable);
            if (Objects.nonNull(selectBody.getRelatedSelect())) {
                result = union(cteTable, extractSelectBodyForCte(selectBody.getRelatedSelect().getSelect(), cteTable));
            }
            cteTables.removeFirst();
            selectBody.getWith().forEach(with -> cteTables.removeFirst());
        }
        return result;
    }

    private LogicalTable extractSelectBodyForCte(SelectBody selectBody, LogicalTable cteTable) {
        // TODO: May deal with WHERE / HAVING clause?
        LogicalTable result = LogicalTable.empty();
        if (Objects.isNull(selectBody)) {
            return result;
        }
        selectBody.getWith().forEach(with -> cteTables.addFirst(extractWithTable(with, selectBody.isRecursive())));
        List<FromReference> fromRefs = selectBody.getFroms();
        List<Projection> selectItems = selectBody.getSelectItems();
        int cursor = 0;
        for (Projection item : selectItems) {
            LogicalTable fromTable = extractFromReferences(fromRefs);
            fromTables.addFirst(fromTable);
            List<LogicalColumn> columns = extractSelectItem(item);
            result.getColumnList().addAll(columns);
            for (LogicalColumn column : columns) {
                cteTable.getColumnList().set(cursor, union(cteTable.getColumnList().get(cursor), column));
                cursor++;
            }
            fromTables.removeFirst();
        }
        result.setName(selectBody.getText());
        if (Objects.nonNull(selectBody.getRelatedSelect())) {
            result = union(result, extractSelectBodyForCte(selectBody.getRelatedSelect().getSelect(), cteTable));
        }
        selectBody.getWith().forEach(with -> cteTables.removeFirst());
        return result;
    }

    private LogicalTable extractFromReferences(List<FromReference> fromReferences) {
        if (fromReferences.size() == 1) {
            return extractFromReference(fromReferences.get(0));
        }
        LogicalTable table = LogicalTable.empty();
        for (FromReference fromRef : fromReferences) {
            LogicalTable fromTable = extractFromReference(fromRef);
            table.getTableList().add(fromTable);
            for (LogicalColumn column : fromTable.getColumnList()) {
                table.getColumnList().add(inheritColumn(column));
            }
        }
        return table;
    }

    private LogicalTable extractFromReference(FromReference fromReference) {
        if (fromReference instanceof NameReference) {
            NameReference ref = (NameReference) fromReference;
            LogicalTable table = LogicalTable.empty();
            String schema = processIdentifier(ref.getSchema());
            String relation = processIdentifier(ref.getRelation());
            String alias = processIdentifier(ref.getAlias());
            table.setName(relation);
            table.setAlias(alias);
            if (StringUtils.isBlank(schema)) {
                List<LogicalColumn> columns;
                try {
                    columns = getColumnsFromCteTable(relation);
                } catch (NotFoundException e1) {
                    try {
                        columns = getColumnsFromVirtualTable(relation);
                    } catch (NotFoundException e2) {
                        columns = getColumnsFromPhysicalSchema(currentDatabase, relation);
                    }
                }
                table.setColumnList(columns);
            } else {
                table.setColumnList(getColumnsFromPhysicalSchema(schema, relation));
            }
            return table;
        } else if (fromReference instanceof JoinReference) {
            JoinReference ref = (JoinReference) fromReference;
            LogicalTable left = extractFromReference(ref.getLeft());
            LogicalTable right = extractFromReference(ref.getRight());
            JoinCondition condition = ref.getCondition();
            switch (ref.getType()) {
                case NATURAL_JOIN:
                case NATURAL_INNER_JOIN:
                case NATURAL_FULL_JOIN:
                case NATURAL_FULL_OUTER_JOIN:
                case NATURAL_LEFT_JOIN:
                case NATURAL_LEFT_OUTER_JOIN:
                case NATURAL_RIGHT_JOIN:
                case NATURAL_RIGHT_OUTER_JOIN:
                    return naturalJoin(left, right);
                default:
                    if (Objects.nonNull(condition) && condition.getConditionType() == JoinCondition.USING) {
                        UsingJoinCondition using = (UsingJoinCondition) condition;
                        return usingJoin(left, right, using.getColumnList());
                    } else {
                        return simpleJoin(left, right);
                    }
            }
        } else if (fromReference instanceof ExpressionReference) {
            ExpressionReference ref = (ExpressionReference) fromReference;
            SelectBody selectBody;
            if (ref.getTarget() instanceof Select) {
                selectBody = ((Select) ref.getTarget()).getSelectBody();
            } else if (ref.getTarget() instanceof SelectBody) {
                selectBody = (SelectBody) ref.getTarget();
            } else {
                throw new IllegalArgumentException("ExpressionReference#target");
            }
            LogicalTable result = extractSelectBody(selectBody);
            if (Objects.nonNull(result)) {
                result.setAlias(processIdentifier(ref.getAlias()));
            }
            return result;
        }
        return LogicalTable.empty();
    }

    private LogicalTable extractSelectItems(List<Projection> selectItems) {
        LogicalTable result = LogicalTable.empty();
        for (Projection item : selectItems) {
            result.getColumnList().addAll(extractSelectItem(item));
        }
        return result;
    }

    private List<LogicalColumn> extractSelectItem(Projection item) {
        if (item.isStar()) {
            List<LogicalColumn> result = new ArrayList<>();
            for (LogicalColumn c : fromTables.getFirst().getColumnList()) {
                result.add(inheritColumn(c));
            }
            return result;
        }
        Expression columnExpr = item.getColumn();
        String columnLabel = processIdentifier(item.getColumnLabel());
        if (columnExpr instanceof ColumnReference) {
            ColumnReference columnRef = (ColumnReference) columnExpr;
            List<LogicalColumn> result = extractColumnReferenceItem(columnRef);
            if (result.size() == 1) {
                result.get(0).setAlias(columnLabel);
            }
            return result;
        } else if (columnExpr instanceof RelationReference) {
            RelationReference relationRef = (RelationReference) columnExpr;
            List<LogicalColumn> result = extractRelationReferenceItem(relationRef);
            if (result.size() == 1) {
                result.get(0).setAlias(columnLabel);
            }
            return result;
        } else if (columnExpr instanceof ConstExpression) {
            ConstExpression constExpr = (ConstExpression) columnExpr;
            LogicalColumn c = extractConstExpressionItem(constExpr);
            c.setAlias(columnLabel);
            return Collections.singletonList(c);
        } else if (columnExpr instanceof CompoundExpression) {
            CompoundExpression compoundExpr = (CompoundExpression) columnExpr;
            LogicalColumn c = extractCompoundExpressionItem(compoundExpr);
            c.setAlias(columnLabel);
            return Collections.singletonList(c);
        } else if (columnExpr instanceof FunctionCall) {
            FunctionCall functionCall = (FunctionCall) columnExpr;
            LogicalColumn c = extractFunctionCallItem(functionCall);
            c.setAlias(columnLabel);
            return Collections.singletonList(c);
        } else if (columnExpr instanceof SelectBody) {
            SelectBody selectBody = (SelectBody) columnExpr;
            LogicalTable t = extractSelectBody(selectBody);
            Verify.singleton(t.getColumnList(), "extractSelectBodyItemResult");
            LogicalColumn c = t.getColumnList().get(0);
            c.setName(t.getName());
            c.setAlias(columnLabel);
            c.setType(ColumnType.SELECT);
            return Collections.singletonList(c);
        } else if (columnExpr instanceof CaseWhen) {
            CaseWhen caseWhen = (CaseWhen) columnExpr;
            LogicalColumn c = extractCaseWhenItem(caseWhen);
            c.setAlias(columnLabel);
            return Collections.singletonList(c);
        } else {
            throw new IllegalStateException("Unknown type of projection: " + columnExpr);
        }
    }

    private List<LogicalColumn> extractColumnReferenceItem(ColumnReference columnRef) {
        return extractItemFromReference(
                processIdentifier(columnRef.getSchema()),
                processIdentifier(columnRef.getRelation()),
                processIdentifier(columnRef.getColumn()));
    }

    private List<LogicalColumn> extractRelationReferenceItem(RelationReference relationRef) {
        LinkedList<String> relations = new LinkedList<>();
        relations.addFirst(relationRef.getRelationName());
        Expression expr = relationRef.getReference();
        while (Objects.nonNull(expr)) {
            if (expr instanceof RelationReference) {
                relations.addFirst(((RelationReference) expr).getRelationName());
                expr = ((RelationReference) expr).getReference();
                continue;
            }
            break;
        }
        Verify.verify(relations.size() <= 3, "RelationReference is deeper than expected");
        String columnName = processIdentifier(relations.pollFirst());
        String tableName = processIdentifier(relations.pollFirst());
        String databaseName = processIdentifier(relations.pollFirst());
        if (ORACLE_PSEUDO_COLUMNS.contains(columnName)) {
            LogicalColumn c = LogicalColumn.empty();
            c.setName(columnName);
            c.setType(ColumnType.CONSTANT);
            return Collections.singletonList(c);
        }
        return extractItemFromReference(databaseName, tableName, columnName);
    }

    private List<LogicalColumn> extractItemFromReference(String databaseName, String tableName, String columnName) {
        List<LogicalColumn> result = new ArrayList<>();
        if (StringUtils.isBlank(databaseName) && StringUtils.isBlank(tableName)) {
            // 1. 库名、表名均为空，则列名必不为空，且不为 *。此时仅查询 fromTable 的 columnList，如果不能唯一确定列名，则报错
            for (LogicalTable fromTable : fromTables) {
                for (LogicalColumn column : fromTable.getColumnList()) {
                    if (StringUtils.firstNonBlank(column.getAlias(), column.getName()).equals(columnName)) {
                        return Collections.singletonList(inheritColumn(column));
                    }
                }
            }
        } else {
            // 2. 表名不为空，从 fromTable 和 fromTable#tableList 的 columnList 进行查询
            for (LogicalTable fromTable : fromTables) {
                List<LogicalTable> tables = new ArrayList<>(retrieveLogicalTable(fromTable));
                if (COLUMN_NAME_WILDCARD.equals(columnName)) {
                    // 2.1. 列名为 *，即只要库名和表名匹配的都输出
                    // 先查 fromTable，再查 fromTable#tableList
                    for (LogicalTable table : tables) {
                        if (tableName.equals(table.getAlias())
                                || tableName.equals(table.getName())) {
                            for (LogicalColumn column : table.getColumnList()) {
                                if (StringUtils.isNotBlank(databaseName)
                                        && !databaseName.equals(column.getDatabaseName())) {
                                    break;
                                }
                                result.add(inheritColumn(column));
                            }
                        }
                    }
                } else {
                    // 2.2 列名不为 *，即需要唯一确定一列（这里暂且不处理列名冲突的情况，因为运行此代码的前提是 SQL 语句被成功执行）
                    // 先查 fromTable，再查 fromTable#tableList
                    for (LogicalTable table : tables) {
                        if (tableName.equals(table.getAlias())
                                || tableName.equals(table.getName())) {
                            for (LogicalColumn column : table.getColumnList()) {
                                if (StringUtils.isNotBlank(databaseName)
                                        && !databaseName.equals(column.getDatabaseName())) {
                                    break;
                                }
                                if (StringUtils.firstNonBlank(column.getAlias(), column.getName())
                                        .equals(columnName)) {
                                    return Collections.singletonList(inheritColumn(column));
                                }
                            }
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(result)) {
                    return result;
                }
            }
        }
        return result;
    }

    private LogicalColumn extractConstExpressionItem(ConstExpression constExpr) {
        LogicalColumn column = LogicalColumn.empty();
        column.setName(constExpr.getExprConst());
        column.setType(ColumnType.CONSTANT);
        return column;
    }

    private LogicalColumn extractCompoundExpressionItem(CompoundExpression compoundExpr) {
        LogicalColumn column = LogicalColumn.empty();
        column.setName(compoundExpr.getText());
        column.setType(ColumnType.COMPUTATION);
        if (Objects.nonNull(compoundExpr.getLeft())) {
            column.getFromList().add(extractCommonExpression(compoundExpr.getLeft()));
        }
        if (Objects.nonNull(compoundExpr.getRight())) {
            column.getFromList().add(extractCommonExpression(compoundExpr.getRight()));
        }
        return column;
    }

    private LogicalColumn extractFunctionCallItem(FunctionCall call) {
        LogicalColumn column = LogicalColumn.empty();
        column.setName(call.getText());
        column.setType(ColumnType.FUNCTION_CALL);
        List<FunctionParam> params = call.getParamList();
        if (CollectionUtils.isNotEmpty(params)) {
            params.forEach(param -> column.getFromList().add(extractFunctionParam(param)));
        }
        return column;
    }

    private LogicalColumn extractFunctionParam(FunctionParam param) {
        LogicalColumn column = LogicalColumn.empty();
        if (param instanceof ExpressionParam) {
            ExpressionParam expressionParam = (ExpressionParam) param;
            column = extractCommonExpression(expressionParam.getTarget());
            column.setAlias(processIdentifier(expressionParam.getAlias()));
        }
        return column;
    }

    private LogicalColumn extractCaseWhenItem(CaseWhen caseWhen) {
        LogicalColumn column = LogicalColumn.empty();
        column.setName(caseWhen.getText());
        column.setType(ColumnType.CASE_WHEN);
        if (Objects.nonNull(caseWhen.getCaseValue())) {
            column.getFromList().add(extractCommonExpression(caseWhen.getCaseValue()));
        }
        if (Objects.nonNull(caseWhen.getCaseDefault())) {
            column.getFromList().add(extractCommonExpression(caseWhen.getCaseDefault()));
        }
        if (CollectionUtils.isNotEmpty(caseWhen.getWhenClauses())) {
            caseWhen.getWhenClauses().forEach(whenClause -> {
                column.getFromList().add(extractCommonExpression(whenClause.getWhen()));
                column.getFromList().add(extractCommonExpression(whenClause.getThen()));
            });
        }
        return column;
    }

    private LogicalColumn extractIntervalExpression(IntervalExpression intervalExpr) {
        LogicalColumn column = extractCommonExpression(intervalExpr.getTarget());
        column.setName(intervalExpr.getText());
        return column;
    }

    private LogicalColumn extractCollectionExpression(CollectionExpression collectionExpr) {
        LogicalColumn result = LogicalColumn.empty();
        result.setName(collectionExpr.getText());
        result.setType(ColumnType.INHERITANCE);
        for (Expression e : collectionExpr.getExpressionList()) {
            result.getFromList().add(extractCommonExpression(e));
        }
        return result;
    }

    private LogicalColumn extractCommonExpression(Expression expr) {
        if (expr instanceof ConstExpression) {
            return extractConstExpressionItem((ConstExpression) expr);
        } else if (expr instanceof ColumnReference) {
            List<LogicalColumn> result = extractColumnReferenceItem((ColumnReference) expr);
            Verify.singleton(result, "extractColumnReferenceItemResult");
            return result.get(0);
        } else if (expr instanceof RelationReference) {
            List<LogicalColumn> result = extractRelationReferenceItem((RelationReference) expr);
            Verify.singleton(result, "extractRelationReferenceItemResult");
            return result.get(0);
        } else if (expr instanceof SelectBody) {
            LogicalTable t = extractSelectBody((SelectBody) expr);
            Verify.singleton(t.getColumnList(), "extractSelectBodyItemResult");
            LogicalColumn c = t.getColumnList().get(0);
            c.setName(t.getName());
            c.setType(ColumnType.SELECT);
            return c;
        } else if (expr instanceof CollectionExpression) {
            return extractCollectionExpression((CollectionExpression) expr);
        } else if (expr instanceof CompoundExpression) {
            return extractCompoundExpressionItem((CompoundExpression) expr);
        } else if (expr instanceof FunctionCall) {
            return extractFunctionCallItem((FunctionCall) expr);
        } else if (expr instanceof IntervalExpression) {
            return extractIntervalExpression((IntervalExpression) expr);
        } else {
            throw new IllegalStateException("Unknown type of expression: " + expr);
        }
    }

    private LogicalTable simpleJoin(LogicalTable left, LogicalTable right) {
        LogicalTable result = LogicalTable.empty();
        result.getTableList().add(left);
        result.getTableList().add(right);
        result.getColumnList().addAll(joinColumns(left.getColumnList(), right.getColumnList()));
        return result;
    }

    private LogicalTable naturalJoin(LogicalTable left, LogicalTable right) {
        LogicalTable result = LogicalTable.empty();
        result.getTableList().add(left);
        result.getTableList().add(right);
        List<LogicalColumn> leftColumns = new ArrayList<>(left.getColumnList());
        List<LogicalColumn> rightColumns = new ArrayList<>(right.getColumnList());
        for (int i = 0; i < leftColumns.size(); i++) {
            LogicalColumn leftColumn = leftColumns.get(i);
            String leftLabel = StringUtils.firstNonBlank(leftColumn.getAlias(), leftColumn.getName());
            for (int j = 0; j < rightColumns.size(); j++) {
                LogicalColumn rightColumn = rightColumns.get(j);
                String rightLabel = StringUtils.firstNonBlank(rightColumn.getAlias(), rightColumn.getName());
                if (leftLabel.equals(rightLabel)) {
                    LogicalColumn c = LogicalColumn.empty();
                    c.setName(leftLabel);
                    c.setType(ColumnType.JOIN);
                    c.getFromList().add(leftColumn);
                    c.getFromList().add(rightColumn);
                    result.getColumnList().add(c);
                    leftColumns.remove(i);
                    rightColumns.remove(j);
                    i--;
                    break;
                }
            }
        }
        result.getColumnList().addAll(joinColumns(leftColumns, rightColumns));
        return result;
    }

    private LogicalTable usingJoin(LogicalTable left, LogicalTable right, List<ColumnReference> columnReferences) {
        LogicalTable result = LogicalTable.empty();
        result.getTableList().add(left);
        result.getTableList().add(right);
        Set<String> usingColumnLabels = columnReferences.stream()
                .map(ColumnReference::getColumn).collect(Collectors.toSet());
        Set<String> leftColumnLabels = left.getColumnList().stream()
                .map(c -> StringUtils.firstNonBlank(c.getAlias(), c.getName())).collect(Collectors.toSet());
        Set<String> rightColumnLabels = right.getColumnList().stream()
                .map(c -> StringUtils.firstNonBlank(c.getAlias(), c.getName())).collect(Collectors.toSet());
        Validate.isTrue(leftColumnLabels.containsAll(usingColumnLabels));
        Validate.isTrue(rightColumnLabels.containsAll(usingColumnLabels));
        List<LogicalColumn> leftColumns = new ArrayList<>(left.getColumnList());
        List<LogicalColumn> rightColumns = new ArrayList<>(right.getColumnList());
        for (int i = 0; i < leftColumns.size(); i++) {
            LogicalColumn leftColumn = leftColumns.get(i);
            String leftLabel = StringUtils.firstNonBlank(leftColumn.getAlias(), leftColumn.getName());
            if (!usingColumnLabels.contains(leftLabel)) {
                continue;
            }
            for (int j = 0; j < rightColumns.size(); j++) {
                LogicalColumn rightColumn = rightColumns.get(j);
                String rightLabel = StringUtils.firstNonBlank(rightColumn.getAlias(), rightColumn.getName());
                if (usingColumnLabels.contains(rightLabel)) {
                    LogicalColumn c = LogicalColumn.empty();
                    c.setName(leftLabel);
                    c.setType(ColumnType.JOIN);
                    c.getFromList().add(leftColumn);
                    c.getFromList().add(rightColumn);
                    result.getColumnList().add(c);
                    leftColumns.remove(i);
                    rightColumns.remove(j);
                    i--;
                    break;
                }
            }
        }
        result.getColumnList().addAll(joinColumns(leftColumns, rightColumns));
        return result;
    }

    private LogicalTable union(LogicalTable up, LogicalTable down) {
        Verify.equals(up.getColumnList().size(), down.getColumnList().size(), "columnList.size()");
        LogicalTable result = LogicalTable.empty();
        result.setName(up.getName());
        result.setAlias(up.getAlias());
        result.getTableList().addAll(Arrays.asList(up, down));
        for (int i = 0; i < up.getColumnList().size(); i++) {
            LogicalColumn upColumn = up.getColumnList().get(i);
            LogicalColumn downColumn = down.getColumnList().get(i);
            result.getColumnList().add(union(upColumn, downColumn));
        }
        return result;
    }

    private LogicalColumn union(LogicalColumn up, LogicalColumn down) {
        LogicalColumn column = LogicalColumn.empty();
        column.setName(StringUtils.firstNonBlank(up.getAlias(), up.getName()));
        column.setType(ColumnType.UNION);
        column.getFromList().addAll(Arrays.asList(up, down));
        return column;
    }

    private LogicalColumn inheritColumn(LogicalColumn parent) {
        LogicalColumn column = LogicalColumn.empty();
        column.setName(StringUtils.firstNonBlank(parent.getAlias(), parent.getName()));
        column.setType(ColumnType.INHERITANCE);
        column.getFromList().add(parent);
        return column;
    }

    private List<LogicalColumn> joinColumns(List<LogicalColumn> left, List<LogicalColumn> right) {
        List<LogicalColumn> result = new ArrayList<>();
        Stream.of(left, right).forEach(columns -> {
            for (LogicalColumn column : columns) {
                result.add(inheritColumn(column));
            }
        });
        return result;
    }

    private List<LogicalColumn> getColumnsFromPhysicalSchema(String databaseName, String tableName) {
        List<LogicalColumn> result = new ArrayList<>();
        List<String> columnNames = columnAccessor.getColumns(databaseName, tableName);
        for (String name : columnNames) {
            LogicalColumn c = LogicalColumn.empty();
            c.setName(name);
            c.setType(ColumnType.PHYSICAL);
            c.setDatabaseName(databaseName);
            c.setTableName(tableName);
            result.add(c);
        }
        return result;
    }

    private List<LogicalColumn> getColumnsFromCteTable(String tableName) throws NotFoundException {
        List<LogicalColumn> result = new ArrayList<>();
        for (LogicalTable table : cteTables) {
            if (tableName.equals(table.getName())) {
                for (LogicalColumn column : table.getColumnList()) {
                    result.add(inheritColumn(column));
                }
                return result;
            }
        }
        throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"table", "name", tableName}, null);
    }

    private List<LogicalColumn> getColumnsFromVirtualTable(String tableName) throws NotFoundException {
        if (Objects.nonNull(dialectType) && dialectType.isMysql()) {
            if (MYSQL_PSEUDO_TABLES.contains(tableName)) {
                return Collections.emptyList();
            }
        }
        if (Objects.nonNull(dialectType) && dialectType.isOracle()) {
            if (ORACLE_PSEUDO_TABLES.contains(tableName)) {
                return Collections.emptyList();
            }
        }
        throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"table", "name", tableName}, null);
    }

    private List<LogicalTable> retrieveLogicalTable(LogicalTable table) {
        List<LogicalTable> returnValue = new ArrayList<>();
        if (Objects.nonNull(table)) {
            returnValue.add(table);
            if (CollectionUtils.isNotEmpty(table.getTableList())) {
                for (LogicalTable t : table.getTableList()) {
                    returnValue.addAll(retrieveLogicalTable(t));
                }
            }
        }
        return returnValue;
    }

    private String processIdentifier(String identifier) {
        if (Objects.nonNull(dialectType) && dialectType.isMysql()) {
            String unquoted = StringUtils.unquoteMySqlIdentifier(identifier);
            return StringUtils.isBlank(unquoted) ? unquoted : unquoted.toLowerCase();
        } else if (dialectType == DialectType.OB_ORACLE) {
            String unquoted = StringUtils.unquoteOracleIdentifier(identifier);
            if (StringUtils.isBlank(unquoted)) {
                return unquoted;
            } else {
                return StringUtils.checkOracleIdentifierQuoted(identifier) ? unquoted : unquoted.toUpperCase();
            }
        } else {
            throw new IllegalStateException("Unknown dialect type: " + dialectType);
        }
    }

}
