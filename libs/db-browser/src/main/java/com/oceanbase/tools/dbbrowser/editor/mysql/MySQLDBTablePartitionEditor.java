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
package com.oceanbase.tools.dbbrowser.editor.mysql;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2022/8/17 下午11:19
 * @Description: []
 */
public class MySQLDBTablePartitionEditor extends DBTablePartitionEditor {

    @Override
    protected SqlBuilder sqlBuilder() {
        return new MySQLSqlBuilder();
    }

    @Override
    public String generateCreateObjectDDL(@NotNull DBTablePartition partition) {
        if (partition.getPartitionOption().getType() == DBTablePartitionType.NOT_PARTITIONED) {
            return StringUtils.EMPTY;
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(partition)).space()
                .append(generateCreateDefinitionDDL(partition));
        return sqlBuilder.toString().trim() + ";\n";
    }

    @Override
    public String generateDropObjectDDL(DBTablePartition dbObject) {
        SqlBuilder sqlBuilder = sqlBuilder();
        String fullyQualifiedTableName = getFullyQualifiedTableName(dbObject);
        sqlBuilder.append("ALTER TABLE ").append(fullyQualifiedTableName).append(" REMOVE PARTITIONING").append(";")
                .line();
        return sqlBuilder.toString();
    }

    @Override
    protected void appendDefinitions(DBTablePartition partition, SqlBuilder sqlBuilder) {
        DBTablePartitionType type = partition.getPartitionOption().getType();
        if (type == DBTablePartitionType.HASH || type == DBTablePartitionType.KEY) {
            if (CollectionUtils.isEmpty(partition.getPartitionDefinitions())) {
                return;
            }
        }
        sqlBuilder.append("(\n");
        boolean isFirstColumn = true;
        for (DBTablePartitionDefinition definition : partition.getPartitionDefinitions()) {
            if (!isFirstColumn) {
                sqlBuilder.append(",").line();
            }
            isFirstColumn = false;
            appendDefinition(partition.getPartitionOption(), definition, sqlBuilder);
        }
        sqlBuilder.append("\n)");
    }

    @Override
    protected void appendDefinition(DBTablePartitionOption option,
            DBTablePartitionDefinition definition, SqlBuilder sqlBuilder) {
        sqlBuilder.append("PARTITION ").identifier(definition.getName());
        if (option.getType() == DBTablePartitionType.RANGE || option.getType() == DBTablePartitionType.RANGE_COLUMNS) {
            if (Objects.nonNull(definition.getMaxValues())) {
                sqlBuilder.append(" VALUES LESS THAN (").append(String.join(",", definition.getMaxValues()))
                        .append(")");
            }
        } else if (option.getType() == DBTablePartitionType.LIST
                || option.getType() == DBTablePartitionType.LIST_COLUMNS) {
            if (Objects.nonNull(definition.getValuesList())) {
                sqlBuilder.append(" VALUES IN (")
                        .append(DBSchemaAccessorUtil.parseListRangeValuesList(definition.getValuesList()))
                        .append(")");
            }
        }
    }

    @Override
    protected String modifyPartitionType(@NotNull DBTablePartition oldPartition,
            @NotNull DBTablePartition newPartition) {
        return generateCreateObjectDDL(newPartition);
    }

    @Override
    public String generateAddPartitionDefinitionDDL(
            @NotNull DBTablePartitionDefinition definition,
            @NotNull DBTablePartitionOption option, String fullyQualifiedTableName) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(fullyQualifiedTableName).append(" ADD PARTITION(");
        appendDefinition(option, definition, sqlBuilder);
        sqlBuilder.append(");").line();
        return sqlBuilder.toString();
    }

    @Override
    public String generateAddPartitionDefinitionDDL(String schemaName, @NonNull String tableName,
            @NonNull DBTablePartitionOption option, List<DBTablePartitionDefinition> definitions) {
        DBTablePartitionType partitionType = option.getType();
        if (partitionType != DBTablePartitionType.RANGE
                && partitionType != DBTablePartitionType.RANGE_COLUMNS
                && partitionType != DBTablePartitionType.LIST
                && partitionType != DBTablePartitionType.LIST_COLUMNS) {
            return "";
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ");
        if (StringUtils.isNotEmpty(schemaName)) {
            sqlBuilder.identifier(schemaName).append(".");
        }
        sqlBuilder.identifier(tableName).append(" ADD PARTITION (").append("\n\t");
        Validate.isTrue(!CollectionUtils.isEmpty(definitions), "Partition elements can not be empty");
        for (int i = 0; i < definitions.size(); i++) {
            appendDefinition(option, definitions.get(i), sqlBuilder);
            if (i < definitions.size() - 1) {
                sqlBuilder.append(",\n\t");
            }
        }
        return sqlBuilder.append(");").line().toString();
    }

    @Override
    protected void generateCreateTemplateSubPartition(DBTablePartition partition, SqlBuilder sqlBuilder) {
        if (Objects.isNull(partition.getSubpartition())) {
            return;
        }
        DBTablePartitionOption subPartOption = partition.getSubpartition().getPartitionOption();
        if (Objects.isNull(subPartOption)) {
            return;
        }
        // TODO 目前只支持 HASH/KEY 模板化二级分区
        if (partition.getSubpartitionTemplated() && (subPartOption.getType() == DBTablePartitionType.HASH
                || subPartOption.getType() == DBTablePartitionType.KEY)) {
            sqlBuilder.append("SUBPARTITION BY ").append(subPartOption.getType().getValue())
                    .append("(");
            appendExpression(partition.getSubpartition(), sqlBuilder);
            sqlBuilder.append(") subpartitions ").append(String.valueOf(subPartOption.getPartitionsNum()));
        }
    }

}
