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
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.sqlparser.statement.Expression;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.Partition;
import com.oceanbase.tools.sqlparser.statement.createtable.PartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubListPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionElement;
import com.oceanbase.tools.sqlparser.statement.createtable.SubPartitionOption;
import com.oceanbase.tools.sqlparser.statement.createtable.SubRangePartitionElement;
import com.oceanbase.tools.sqlparser.statement.expression.CollectionExpression;
import com.oceanbase.tools.sqlparser.statement.expression.ConstExpression;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2024/11/29 14:34
 * @since: 4.3.3
 */
@Slf4j
public abstract class BaseOBGetDBTableByParser implements GetDBTableByParser {

    public final DBTablePartition getPartition() {
        DBTablePartition partition = new DBTablePartition();
        DBTablePartitionOption partitionOption = new DBTablePartitionOption();
        partitionOption.setType(DBTablePartitionType.NOT_PARTITIONED);
        partition.setPartitionOption(partitionOption);
        List<DBTablePartitionDefinition> partitionDefinitions = new ArrayList<>();
        partition.setPartitionDefinitions(partitionDefinitions);

        if (Objects.isNull(getCreateTableStmt())) {
            partition.setWarning("Failed to parse table ddl");
            return partition;
        }
        Partition partitionStmt = getCreateTableStmt().getPartition();
        if (Objects.isNull(partitionStmt)) {
            return partition;
        }
        parsePartitionStmt(partition, partitionStmt);

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

    private void fillSubPartitions(@NonNull DBTablePartition partition, @NonNull Partition partitionStmt) {
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
        fillSubPartitionOption(partitionStmt, subPartitionOption, subDBTablePartitionType);
        fillSubPartitionDefinitions(partition, partitionStmt, subPartitionDefinitions, subDBTablePartitionType);
    }

    private void fillSubPartitionOption(@NonNull Partition partitionStmt,
            @NonNull DBTablePartitionOption subPartitionOption,
            @NonNull DBTablePartitionType subDBTablePartitionType) {
        subPartitionOption.setType(subDBTablePartitionType);
        SubPartitionOption parsedSubPartitionOption = partitionStmt.getSubPartitionOption();
        fillSubPartitionKey(subPartitionOption, subDBTablePartitionType, parsedSubPartitionOption);
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
    }

    private void fillSubPartitionDefinitions(@NonNull DBTablePartition partition, @NonNull Partition partitionStmt,
            @NonNull List<DBTablePartitionDefinition> subPartitionDefinitions,
            @NonNull DBTablePartitionType subDBTablePartitionType) {
        List<? extends PartitionElement> partitionElements = partitionStmt.getPartitionElements();
        if (partitionElements == null || partitionElements.isEmpty()) {
            log.warn("no partitions found in partition statement");
            return;
        }
        for (int i = 0; i < partitionElements.size(); i++) {
            DBTablePartitionDefinition partitionDefinition = partition.getPartitionDefinitions().get(i);
            PartitionElement partitionElement = partitionElements.get(i);
            if (partitionElement == null) {
                continue;
            }
            List<SubPartitionElement> subPartitionElements = partitionElement.getSubPartitionElements();
            if (subPartitionElements != null) {
                fillNonTemplatedSubPartitionDefinitions(subPartitionDefinitions, subDBTablePartitionType,
                        partitionElement, partitionDefinition);
            } else {
                fillTemplatedSubPartitionDefinitions(partitionStmt, subPartitionDefinitions, subDBTablePartitionType,
                        partitionElement, partitionDefinition);
            }
        }
    }

    private void fillNonTemplatedSubPartitionDefinitions(
            @NonNull List<DBTablePartitionDefinition> subPartitionDefinitions,
            @NonNull DBTablePartitionType subDBTablePartitionType, @NonNull PartitionElement partitionElement,
            @NonNull DBTablePartitionDefinition partitionDefinition) {
        List<SubPartitionElement> subPartitionElements = partitionElement.getSubPartitionElements();
        if (subPartitionElements == null || subPartitionElements.isEmpty()) {
            log.warn("no non-templated sub-partitions found for partition {}", partitionDefinition.getName());
            return;
        }
        for (int j = 0; j < subPartitionElements.size(); j++) {
            SubPartitionElement subPartitionElement = subPartitionElements.get(j);
            if (subPartitionElement == null) {
                continue;
            }
            DBTablePartitionDefinition subPartitionDefinition = new DBTablePartitionDefinition();
            fillSubPartitionValue(subPartitionElement, subPartitionDefinition);
            subPartitionDefinition.setParentPartitionDefinition(partitionDefinition);
            subPartitionDefinition.setName(
                    removeIdentifiers(subPartitionElement.getRelation()));
            subPartitionDefinition.setOrdinalPosition(j);
            subPartitionDefinition.setType(subDBTablePartitionType);
            subPartitionDefinitions.add(subPartitionDefinition);
        }
    }

    private void fillTemplatedSubPartitionDefinitions(@NonNull Partition partitionStmt,
            @NonNull List<DBTablePartitionDefinition> subPartitionDefinitions,
            @NonNull DBTablePartitionType subDBTablePartitionType, @NonNull PartitionElement partitionElement,
            @NonNull DBTablePartitionDefinition partitionDefinition) {
        String parentPartitionName = removeIdentifiers(partitionElement.getRelation());
        List<SubPartitionElement> templates = partitionStmt.getSubPartitionOption().getTemplates();
        if (templates == null || templates.isEmpty()) {
            log.warn("no templated sub-partitions found for partition {}", parentPartitionName);
            return;
        }
        for (int j = 0; j < templates.size(); j++) {
            SubPartitionElement subPartitionElement = templates.get(j);
            if (subPartitionElement == null) {
                continue;
            }
            DBTablePartitionDefinition subPartitionDefinition = new DBTablePartitionDefinition();
            fillSubPartitionValue(subPartitionElement, subPartitionDefinition);
            subPartitionDefinition.setParentPartitionDefinition(partitionDefinition);
            subPartitionDefinition.setName(
                    generateTemplateSubPartitionName(parentPartitionName, subPartitionElement.getRelation()));
            subPartitionDefinition.setOrdinalPosition(j);
            subPartitionDefinition.setType(subDBTablePartitionType);
            subPartitionDefinitions.add(subPartitionDefinition);
        }
    }

    private void fillSubPartitionValue(@NonNull SubPartitionElement subPartitionElement,
            @NonNull DBTablePartitionDefinition subPartitionDefinition) {
        if (subPartitionElement instanceof SubListPartitionElement) {
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
        } else if (subPartitionElement instanceof SubRangePartitionElement) {
            SubRangePartitionElement subRangePartitionElement =
                    (SubRangePartitionElement) subPartitionElement;
            subPartitionDefinition.setMaxValues(
                    subRangePartitionElement.getRangeExprs().stream().map(Expression::getText)
                            .collect(Collectors.toList()));
        }
    }

    protected abstract String generateTemplateSubPartitionName(String partitionName, String subPartitionName);

    protected abstract void fillSubPartitionKey(DBTablePartitionOption subPartitionOption,
            DBTablePartitionType subDBTablePartitionType,
            SubPartitionOption parsedSubPartitionOption);

    protected abstract void parsePartitionStmt(DBTablePartition partition, Partition partitionStmt);

    protected abstract String removeIdentifiers(String str);

    protected abstract CreateTable getCreateTableStmt();

    @Override
    public List<DBTableIndex> listIndexes() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public List<DBTableColumn> listColumns() {
        throw new UnsupportedOperationException("Not supported yet");
    }

}
