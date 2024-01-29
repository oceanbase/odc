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
package com.oceanbase.tools.dbbrowser.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @Author: Lebie
 * @Date: 2022/8/17 下午10:22
 * @Description: []
 */
public abstract class DBTablePartitionEditor implements DBObjectEditor<DBTablePartition> {

    @Override
    public boolean editable() {
        return false;
    }

    protected void appendExpression(DBTablePartition partition, SqlBuilder sqlBuilder) {
        String expression = partition.getPartitionOption().getExpression();
        List<String> columnNames = partition.getPartitionOption().getColumnNames();
        if (StringUtils.isNotEmpty(expression)) {
            sqlBuilder.append(expression);
        } else if (CollectionUtils.isNotEmpty(columnNames)) {
            List<String> quotedColumnNames =
                    columnNames.stream().map(columnName -> sqlBuilder().append(columnName).toString())
                            .collect(Collectors.toList());
            sqlBuilder.append(String.join(",", quotedColumnNames));
        }
    }

    @Override
    public String generateCreateDefinitionDDL(@NotNull DBTablePartition partition) {
        if (partition.getPartitionOption().getType() == null
                || partition.getPartitionOption().getType() == DBTablePartitionType.NOT_PARTITIONED) {
            return StringUtils.EMPTY;
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append(" PARTITION BY ").append(partition.getPartitionOption().getType().getValue())
                .append("(");
        appendExpression(partition, sqlBuilder);
        sqlBuilder.append(") ");
        if (Objects.nonNull(partition.getSubpartitionTemplated()) && partition.getSubpartitionTemplated()) {
            generateCreateTemplateSubPartition(partition, sqlBuilder);
        }
        if (Objects.nonNull(partition.getPartitionOption().getPartitionsNum())) {
            sqlBuilder.append("\nPARTITIONS ")
                    .append(String.valueOf(partition.getPartitionOption().getPartitionsNum()));
            if (partition.getPartitionOption().getType() == DBTablePartitionType.HASH
                    || partition.getPartitionOption().getType() == DBTablePartitionType.KEY) {
                return sqlBuilder.toString();
            }
        }
        appendDefinitions(partition, sqlBuilder);
        return sqlBuilder.toString();
    }

    protected void generateCreateTemplateSubPartition(DBTablePartition partition, SqlBuilder sqlBuilder) {}

    protected abstract void appendDefinitions(DBTablePartition partition, SqlBuilder sqlBuilder);

    protected abstract void appendDefinition(DBTablePartitionOption option, DBTablePartitionDefinition definition,
            SqlBuilder sqlBuilder);

    @Override
    public String generateUpdateObjectDDL(@NotNull DBTablePartition oldPartition,
            @NotNull DBTablePartition newPartition) {
        if (Objects.isNull(oldPartition) || Objects.isNull(newPartition)) {
            return StringUtils.EMPTY;
        }
        if (oldPartition.getPartitionOption().getType() != newPartition.getPartitionOption().getType()) {
            return handleUpdatePartitionType(oldPartition, newPartition);
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        String fullyQualifiedTableName = getFullyQualifiedTableName(newPartition);
        List<DBTablePartitionDefinition> oldDefinitions = oldPartition.getPartitionDefinitions();
        List<DBTablePartitionDefinition> newDefinitions = newPartition.getPartitionDefinitions();
        if (CollectionUtils.isEmpty(oldDefinitions)) {
            if (CollectionUtils.isNotEmpty(newDefinitions)) {
                newDefinitions.forEach(definition -> sqlBuilder
                        .append(generateAddPartitionDefinitionDDL(definition, newPartition.getPartitionOption(),
                                fullyQualifiedTableName)));
            }
            return sqlBuilder.toString();
        }
        if (CollectionUtils.isEmpty(newDefinitions)) {
            if (CollectionUtils.isNotEmpty(oldDefinitions)) {
                oldDefinitions.forEach(definition -> sqlBuilder
                        .append(generateDropPartitionDefinitionDDL(definition, fullyQualifiedTableName)));
            }
            return sqlBuilder.toString();
        }
        Map<Integer, DBTablePartitionDefinition> position2NewPartition = new HashMap<>();
        newDefinitions.forEach(newDefinition -> {
            if (Objects.nonNull(newDefinition.getOrdinalPosition())) {
                position2NewPartition.put(newDefinition.getOrdinalPosition(), newDefinition);
            }
        });
        for (DBTablePartitionDefinition newDefinition : newDefinitions) {
            // ordinaryPosition is NULL means this is a new partition
            if (Objects.isNull(newDefinition.getOrdinalPosition())) {
                sqlBuilder
                        .append(generateAddPartitionDefinitionDDL(newDefinition, newPartition.getPartitionOption(),
                                fullyQualifiedTableName));
            }
        }
        for (DBTablePartitionDefinition oldDefinition : oldDefinitions) {
            // means this partition should be dropped
            if (!position2NewPartition.containsKey(oldDefinition.getOrdinalPosition())) {
                sqlBuilder.append(generateDropPartitionDefinitionDDL(oldDefinition, fullyQualifiedTableName));
            }
        }
        return sqlBuilder.toString();
    }

    private String handleUpdatePartitionType(@NotNull DBTablePartition oldPartition,
            @NotNull DBTablePartition newPartition) {
        DBTablePartitionType oldType = oldPartition.getPartitionOption().getType();
        DBTablePartitionType newType = newPartition.getPartitionOption().getType();
        Validate.isTrue(oldType != newType, "Partition type should be different");
        if (newType == DBTablePartitionType.NOT_PARTITIONED) {
            // means convert partitioned table to non-partitioned table
            return generateDropObjectDDL(oldPartition);
        } else if (oldType == DBTablePartitionType.NOT_PARTITIONED) {
            // means convert non-partitioned table to partitioned table
            return generateCreateObjectDDL(newPartition);
        } else {
            // means modify table partition type
            return modifyPartitionType(oldPartition, newPartition);
        }
    }

    abstract protected String modifyPartitionType(@NotNull DBTablePartition oldPartition,
            @NotNull DBTablePartition newPartition);

    @Override
    public String generateUpdateObjectListDDL(Collection<DBTablePartition> oldObjects,
            Collection<DBTablePartition> newObjects) {
        if (CollectionUtils.isEmpty(oldObjects) || CollectionUtils.isEmpty(newObjects)) {
            return StringUtils.EMPTY;
        }
        List<DBTablePartition> oldPartitions = new ArrayList<>(oldObjects);
        List<DBTablePartition> newPartitions = new ArrayList<>(newObjects);
        return generateUpdateObjectDDL(oldPartitions.get(0), newPartitions.get(0));
    }

    public String generateShadowTableUpdateObjectDDL(@NotNull DBTablePartition oldPartition,
            @NotNull DBTablePartition newPartition) {
        if (Objects.isNull(oldPartition) || Objects.isNull(newPartition)) {
            return StringUtils.EMPTY;
        }
        if (oldPartition.getPartitionOption().getType() != newPartition.getPartitionOption().getType()) {
            return handleUpdatePartitionType(oldPartition, newPartition);
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        String fullyQualifiedTableName = getFullyQualifiedTableName(newPartition);
        List<DBTablePartitionDefinition> oldDefinitions = oldPartition.getPartitionDefinitions();
        List<DBTablePartitionDefinition> newDefinitions = newPartition.getPartitionDefinitions();
        if (CollectionUtils.isEmpty(oldDefinitions)) {
            if (CollectionUtils.isNotEmpty(newDefinitions)) {
                newDefinitions.forEach(definition -> sqlBuilder
                        .append(generateAddPartitionDefinitionDDL(definition, newPartition.getPartitionOption(),
                                fullyQualifiedTableName)));
            }
            return sqlBuilder.toString();
        }
        if (CollectionUtils.isEmpty(newDefinitions)) {
            if (CollectionUtils.isNotEmpty(oldDefinitions)) {
                oldDefinitions.forEach(definition -> sqlBuilder
                        .append(generateDropPartitionDefinitionDDL(definition, fullyQualifiedTableName)));
            }
            return sqlBuilder.toString();
        }
        Map<String, DBTablePartitionDefinition> name2NewPartition = new HashMap<>();
        Map<String, DBTablePartitionDefinition> name2OldPartition = new HashMap<>();

        newDefinitions.forEach(newDefinition -> {
            if (Objects.nonNull(newDefinition.getName())) {
                name2NewPartition.put(newDefinition.getName(), newDefinition);
            }
        });

        oldDefinitions.forEach(oldDefinition -> {
            if (Objects.nonNull(oldDefinition.getName())) {
                name2OldPartition.put(oldDefinition.getName(), oldDefinition);
            }
        });

        for (DBTablePartitionDefinition newDefinition : newDefinitions) {
            if (!name2OldPartition.containsKey(newDefinition.getName())) {
                sqlBuilder
                        .append(generateAddPartitionDefinitionDDL(newDefinition, newPartition.getPartitionOption(),
                                fullyQualifiedTableName));
            }
        }
        for (DBTablePartitionDefinition oldDefinition : oldDefinitions) {
            // means this partition should be dropped
            if (!name2NewPartition.containsKey(oldDefinition.getName())) {
                sqlBuilder.append(generateDropPartitionDefinitionDDL(oldDefinition, fullyQualifiedTableName));
            }
        }
        return sqlBuilder.toString();
    }

    @Override
    public String generateRenameObjectDDL(@NotNull DBTablePartition oldObject,
            @NotNull DBTablePartition newObject) {
        return StringUtils.EMPTY;
    }

    protected String generateDropPartitionDefinitionDDL(@NotNull DBTablePartitionDefinition definition,
            String fullyQualifiedTableName) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(fullyQualifiedTableName).append(" DROP PARTITION (")
                .append(definition.getName()).append(");").line();
        return sqlBuilder.toString();
    }

    protected abstract String generateAddPartitionDefinitionDDL(@NotNull DBTablePartitionDefinition definition,
            @NotNull DBTablePartitionOption option, String fullyQualifiedTableName);

    protected abstract SqlBuilder sqlBuilder();

    protected String getFullyQualifiedTableName(@NotNull DBTablePartition partition) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (StringUtils.isNotEmpty(partition.getSchemaName())) {
            sqlBuilder.identifier(partition.getSchemaName());
        }
        if (StringUtils.isNotEmpty(partition.getTableName())) {
            sqlBuilder.append(".").identifier(partition.getTableName());
        }
        return sqlBuilder.toString();
    }

}
