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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.pf4j.Extension;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLInformationExtension;
import com.oceanbase.odc.plugin.task.api.partitionplan.AutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype.OBMySQLPartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMySQLSqlExprPartitionExprGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMySQLTimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.drop.OBMySQLKeepLatestPartitionGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLDateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLExprBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.util.DBTablePartitionEditors;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
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
 * @see AutoPartitionExtensionPoint
 */
@Extension
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
        SqlExprCalculator calculator = new OBMySQLExprCalculator(connection);
        DBTablePartitionOption option = table.getPartition().getPartitionOption();
        List<String> keys = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(option.getColumnNames())) {
            keys.addAll(option.getColumnNames());
        } else if (StringUtils.isNotEmpty(option.getExpression())) {
            keys.add(option.getExpression());
        } else {
            throw new IllegalStateException("Partition type is unknown, expression and columns are both null");
        }
        return keys.stream().map(c -> new OBMySQLPartitionKeyDataTypeFactory(calculator, table, c).generate())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> generateCreatePartitionDdls(@NonNull Connection connection,
            @NonNull DBTablePartition partition) {
        InformationExtensionPoint extensionPoint = new OBMySQLInformationExtension();
        DBTablePartitionEditor editor = DBTablePartitionEditors.generate(extensionPoint.getDBVersion(connection));
        return Collections.singletonList(editor.generateAddPartitionDefinitionDDL(partition.getSchemaName(),
                partition.getTableName(), partition.getPartitionOption(), partition.getPartitionDefinitions()));
    }

    @Override
    public List<String> generateDropPartitionDdls(@NonNull Connection connection,
            @NonNull DBTablePartition partition, boolean reloadIndexes) {
        InformationExtensionPoint extensionPoint = new OBMySQLInformationExtension();
        DBTablePartitionEditor editor = DBTablePartitionEditors.generate(extensionPoint.getDBVersion(connection));
        return Collections.singletonList(editor.generateDropPartitionDefinitionDDL(partition.getSchemaName(),
                partition.getTableName(), partition.getPartitionDefinitions()));
    }

    @Override
    public DropPartitionGenerator getDropPartitionGeneratorByName(@NonNull String name) {
        List<DropPartitionGenerator> candidates = new ArrayList<>(2);
        candidates.add(new OBMySQLKeepLatestPartitionGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

    @Override
    public PartitionExprGenerator getPartitionExpressionGeneratorByName(@NonNull String name) {
        List<PartitionExprGenerator> candidates = new ArrayList<>(2);
        candidates.add(new OBMySQLSqlExprPartitionExprGenerator());
        candidates.add(new OBMySQLTimeIncreasePartitionExprGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

    @Override
    public PartitionNameGenerator getPartitionNameGeneratorGeneratorByName(@NonNull String name) {
        List<PartitionNameGenerator> candidates = new ArrayList<>(2);
        candidates.add(new OBMySQLDateBasedPartitionNameGenerator());
        candidates.add(new OBMySQLExprBasedPartitionNameGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

}
