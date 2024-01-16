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
package com.oceanbase.tools.dbbrowser.editor.oracle;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.util.DBSchemaAccessorUtil;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @Author: Lebie
 * @Date: 2022/8/17 下午11:35
 * @Description: []
 */
public class OracleDBTablePartitionEditor extends DBTablePartitionEditor {

    @Override
    public String generateCreateObjectDDL(@NotNull DBTablePartition partition) {
        if (partition.getPartitionOption().getType() == DBTablePartitionType.NOT_PARTITIONED) {
            return StringUtils.EMPTY;
        }
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(getFullyQualifiedTableName(partition)).space()
                .append("MODIFY")
                .append(generateCreateDefinitionDDL(partition));
        return sqlBuilder.toString().trim() + ";\n";
    }

    @Override
    protected void appendDefinitions(DBTablePartition partition, SqlBuilder sqlBuilder) {
        DBTablePartitionType type = partition.getPartitionOption().getType();
        if (type == DBTablePartitionType.HASH) {
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
            appendDefinition(partition.getPartitionOption(), definition, sqlBuilder);
            isFirstColumn = false;
        }
        sqlBuilder.append("\n)");
    }

    @Override
    protected void appendDefinition(DBTablePartitionOption option,
            DBTablePartitionDefinition definition, SqlBuilder sqlBuilder) {
        sqlBuilder.append("PARTITION ").identifier(definition.getName());
        if (option.getType() == DBTablePartitionType.RANGE && Objects.nonNull(definition.getMaxValues())) {
            sqlBuilder.append(" VALUES LESS THAN (").append(String.join(",", definition.getMaxValues()))
                    .append(")");

        } else if (option.getType() == DBTablePartitionType.LIST && Objects.nonNull(definition.getValuesList())) {
            sqlBuilder.append(" VALUES (")
                    .append(DBSchemaAccessorUtil.parseListRangeValuesList(definition.getValuesList())).append(")");
        }
    }

    @Override
    protected String modifyPartitionType(@NotNull DBTablePartition oldPartition,
            @NotNull DBTablePartition newPartition) {
        return "/* Unsupported operation to modify table partition type */\n";
    }

    @Override
    protected String generateAddPartitionDefinitionDDL(
            @NotNull DBTablePartitionDefinition definition,
            @NotNull DBTablePartitionOption option, String fullyQualifiedTableName) {
        SqlBuilder sqlBuilder = sqlBuilder();
        sqlBuilder.append("ALTER TABLE ").append(fullyQualifiedTableName).append(" ADD ");
        appendDefinition(option, definition, sqlBuilder);
        sqlBuilder.append(";").line();
        return sqlBuilder.toString();
    }

    @Override
    protected SqlBuilder sqlBuilder() {
        return new OracleSqlBuilder();
    }

}
