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
package com.oceanbase.odc.service.sqlcheck.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.parser.CreateStatement;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;
import com.oceanbase.tools.sqlparser.statement.expression.ColumnReference;

import lombok.NonNull;

/**
 * {@link BaseObjectNameUsingReservedWords}
 *
 * @author yh263208
 * @date 2024-03-04 17:59
 * @since ODC-release_4.2.4
 */
public abstract class BaseObjectNameUsingReservedWords implements SqlCheckRule {

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.OBJECT_NAME_USING_RESERVED_WORDS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        List<CheckViolation> violations = new ArrayList<>();
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            if (isReservedWords(createTable.getTableName())) {
                violations.add(SqlCheckUtil.buildViolation(statement.getText(),
                        statement, getType(), new Object[] {createTable.getTableName()}));
            }
            violations.addAll(builds(statement, createTable.getPartition()));
            violations.addAll(createTable.getTableElements().stream().flatMap(tableElement -> {
                if (tableElement instanceof ColumnDefinition) {
                    return builds(statement, (ColumnDefinition) tableElement).stream();
                } else if (tableElement instanceof OutOfLineIndex) {
                    return builds(statement, (OutOfLineIndex) tableElement).stream();
                } else if (tableElement instanceof OutOfLineConstraint) {
                    return builds(statement, (OutOfLineConstraint) tableElement).stream();
                }
                return Stream.empty();
            }).collect(Collectors.toList()));
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            violations.addAll(alterTable.getAlterTableActions().stream().flatMap(action -> {
                List<CheckViolation> vs = new ArrayList<>();
                vs.addAll(builds(statement, action.getAddConstraint()));
                vs.addAll(builds(statement, action.getAddIndex()));
                if (CollectionUtils.isNotEmpty(action.getAddColumns())) {
                    vs.addAll(action.getAddColumns().stream()
                            .flatMap(definition -> builds(statement, definition).stream())
                            .collect(Collectors.toList()));
                }
                if (CollectionUtils.isNotEmpty(action.getAddPartitionElements())) {
                    vs.addAll(action.getAddPartitionElements().stream()
                            .flatMap(definition -> builds(statement, definition).stream())
                            .collect(Collectors.toList()));
                }
                if (CollectionUtils.isNotEmpty(action.getAddSubPartitionElements())) {
                    vs.addAll(action.getAddSubPartitionElements().stream()
                            .flatMap(definition -> builds(statement, definition).stream())
                            .collect(Collectors.toList()));
                }
                return vs.stream();
            }).collect(Collectors.toList()));
        } else if (statement instanceof CreateIndex) {
            CreateIndex createIndex = (CreateIndex) statement;
            if (isReservedWords(createIndex.getRelation().getRelation())) {
                violations.add(SqlCheckUtil.buildViolation(statement.getText(), createIndex.getRelation(),
                        getType(), new Object[] {createIndex.getRelation().getRelation()}));
            }
        } else if (statement instanceof CreateStatement) {
            CreateStatement createStatement = (CreateStatement) statement;
            violations.addAll(createStatement.getRelationFactors().stream().flatMap(r -> {
                if (!isReservedWords(r.getRelation())) {
                    return Stream.empty();
                }
                return Stream.of(SqlCheckUtil.buildViolation(statement.getText(), r,
                        getType(), new Object[] {r.getRelation()}));
            }).collect(Collectors.toList()));
        }
        return violations;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.ODP_SHARDING_OB_MYSQL,
                DialectType.OB_MYSQL, DialectType.MYSQL,
                DialectType.OB_ORACLE, DialectType.DORIS, DialectType.ORACLE);
    }

    protected abstract boolean isReservedWords(String objectName);

    private List<CheckViolation> builds(Statement statement, Partition partition) {
        List<CheckViolation> violations = new ArrayList<>();
        if (partition == null) {
            return violations;
        }
        if (CollectionUtils.isNotEmpty(partition.getPartitionElements())) {
            violations.addAll(partition.getPartitionElements().stream()
                    .flatMap(s -> builds(statement, s).stream()).collect(Collectors.toList()));
        }
        return violations;
    }

    private List<CheckViolation> builds(Statement statement, PartitionElement partitionElement) {
        List<CheckViolation> violations = new ArrayList<>();
        if (partitionElement == null) {
            return violations;
        }
        if (isReservedWords(partitionElement.getRelation())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(),
                    partitionElement, getType(), new Object[] {partitionElement.getRelation()}));
        }
        if (CollectionUtils.isNotEmpty(partitionElement.getSubPartitionElements())) {
            violations.addAll(partitionElement.getSubPartitionElements().stream()
                    .flatMap(s -> builds(statement, s).stream()).collect(Collectors.toList()));
        }
        return violations;
    }

    private List<CheckViolation> builds(Statement statement, SubPartitionElement subPartitionElement) {
        List<CheckViolation> violations = new ArrayList<>();
        if (subPartitionElement == null) {
            return violations;
        }
        if (isReservedWords(subPartitionElement.getRelation())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(),
                    subPartitionElement, getType(), new Object[] {subPartitionElement.getRelation()}));
        }
        return violations;
    }

    private List<CheckViolation> builds(Statement statement, ColumnDefinition columnDefinition) {
        List<CheckViolation> violations = new ArrayList<>();
        if (columnDefinition == null) {
            return violations;
        }
        violations.addAll(builds(statement, columnDefinition.getColumnReference()));
        if (columnDefinition.getColumnAttributes() != null
                && CollectionUtils.isNotEmpty(columnDefinition.getColumnAttributes().getConstraints())) {
            violations.addAll(columnDefinition.getColumnAttributes().getConstraints().stream()
                    .flatMap(c -> builds(statement, c).stream()).collect(Collectors.toList()));
        }
        return violations;
    }

    private List<CheckViolation> builds(Statement statement, OutOfLineConstraint outOfLineConstraint) {
        List<CheckViolation> violations = new ArrayList<>();
        if (outOfLineConstraint == null) {
            return violations;
        }
        if (isReservedWords(outOfLineConstraint.getConstraintName())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(), outOfLineConstraint,
                    getType(), new Object[] {outOfLineConstraint.getConstraintName()}));
        }
        if (isReservedWords(outOfLineConstraint.getIndexName())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(), outOfLineConstraint,
                    getType(), new Object[] {outOfLineConstraint.getIndexName()}));
        }
        return violations;
    }

    private List<CheckViolation> builds(Statement statement, OutOfLineIndex outOfLineIndex) {
        List<CheckViolation> violations = new ArrayList<>();
        if (outOfLineIndex == null) {
            return violations;
        }
        if (isReservedWords(outOfLineIndex.getIndexName())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(),
                    outOfLineIndex, getType(), new Object[] {outOfLineIndex.getIndexName()}));
        }
        return violations;
    }

    private List<CheckViolation> builds(Statement statement, InLineConstraint inLineConstraint) {
        List<CheckViolation> violations = new ArrayList<>();
        if (inLineConstraint == null) {
            return violations;
        }
        if (isReservedWords(inLineConstraint.getConstraintName())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(), inLineConstraint,
                    getType(), new Object[] {inLineConstraint.getConstraintName()}));
        }
        return violations;
    }

    private List<CheckViolation> builds(Statement statement, ColumnReference columnReference) {
        List<CheckViolation> violations = new ArrayList<>();
        if (columnReference == null) {
            return violations;
        }
        if (isReservedWords(columnReference.getColumn())) {
            violations.add(SqlCheckUtil.buildViolation(statement.getText(),
                    columnReference, getType(), new Object[] {columnReference.getColumn()}));
        }
        return violations;
    }

}
