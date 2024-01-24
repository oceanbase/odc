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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.task.api.partitionplan.AutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype.OBMySQLPartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OBMySQLAutoPartitionExtensionPoint}
 *
 * @author yh263208
 * @date 2024-01-23 09:59
 * @since ODC_release_4.2.4
 * @see
 */
public class OBMySQLAutoPartitionExtensionPoint implements AutoPartitionExtensionPoint {

    @Override
    public boolean supports(@NonNull DBTablePartition partition) {
        DBTablePartitionType type = partition.getPartitionOption().getType();
        if (type == null) {
            return false;
        }
        switch (type) {
            // only range partition is supported
            case RANGE:
            case RANGE_COLUMNS:
                return partition.getPartitionDefinitions().stream().noneMatch(
                        d -> d.getMaxValues().stream().anyMatch("maxvalue"::equalsIgnoreCase));
            default:
                return false;
        }
    }

    @Override
    public String unquoteIdentifier(@NonNull String identifier) {
        return StringUtils.unquoteMySqlIdentifier(identifier);
    }

    @Override
    public List<DataType> getPartitionKeyDataTypes(@NonNull Connection connection, @NonNull DBTable table) {
        if (!supports(table.getPartition())) {
            throw new UnsupportedOperationException("Unsupported db table");
        }
        DBTablePartitionOption option = table.getPartition().getPartitionOption();
        SqlExprCalculator calculator = new OBMySQLExprCalculator(connection);
        if (CollectionUtils.isNotEmpty(option.getColumnNames())) {
            return option.getColumnNames().stream().map(col -> {
                try {
                    return new OBMySQLPartitionKeyDataTypeFactory(calculator, table, col).generate();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).collect(Collectors.toList());
        } else if (StringUtils.isNotEmpty(option.getExpression())) {
            return Collections.singletonList(new OBMySQLPartitionKeyDataTypeFactory(
                    calculator, table, option.getExpression()).generate());
        }
        throw new IllegalStateException("Partition type is unknown, expression and columns are both null");
    }

    @Override
    public List<String> generateCreatePartitionDdls(@NonNull DBTablePartition partition) {
        return null;
    }

    @Override
    public List<String> generateDropPartitionDdls(@NonNull DBTablePartition partition, boolean reloadIndexes) {
        return null;
    }

    @Override
    public DropPartitionGenerator getDropPartitionGeneratorByName(@NonNull String name) {
        return null;
    }

    @Override
    public PartitionExprGenerator getPartitionExpressionGeneratorByName(@NonNull String name) {
        return null;
    }

    @Override
    public PartitionNameGenerator getPartitionNameGeneratorGeneratorByName(@NonNull String name) {
        return null;
    }

}
