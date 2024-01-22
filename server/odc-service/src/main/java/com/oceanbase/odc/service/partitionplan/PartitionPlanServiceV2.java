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
package com.oceanbase.odc.service.partitionplan;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.PartitionPlanExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanKeyConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.plugin.TaskPluginUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

import lombok.NonNull;

/**
 * {@link PartitionPlanServiceV2}
 *
 * @author yh263208
 * @date 2024-01-11 15:36
 * @since ODC_release_4.2.4
 */
@Service
public class PartitionPlanServiceV2 {

    /**
     * generate the ddl of a partition plan
     */
    public Map<PartitionPlanStrategy, List<String>> generatePartitionDdl(@NonNull Connection connection,
            @NonNull DialectType dialectType, @NonNull String schema, @NonNull PartitionPlanTableConfig tableConfig) {
        PartitionPlanExtensionPoint partitionPlanExtensionPoint = TaskPluginUtil
                .getPartitionPlanExtensionPoint(dialectType);
        TableExtensionPoint tableExtensionPoint = SchemaPluginUtil.getTableExtension(dialectType);
        if (tableExtensionPoint == null || partitionPlanExtensionPoint == null) {
            throw new UnsupportedOperationException("Unsupported dialect " + dialectType);
        }
        String tableName = tableConfig.getTableName();
        DBTable dbTable = tableExtensionPoint.getDetail(connection, schema, tableName);
        List<DBTableColumn> columns = dbTable.getColumns();
        if (CollectionUtils.isEmpty(columns)) {
            throw new NotFoundException(ResourceType.OB_TABLE, "tableName", tableName);
        }
        DBTablePartition partition = dbTable.getPartition();
        if (partition == null || partition.getPartitionOption() == null) {
            throw new IllegalArgumentException("Partition is null for " + schema + "." + tableName);
        } else {
            DBTablePartitionType partitionType = partition.getPartitionOption().getType();
            if (partitionType == DBTablePartitionType.UNKNOWN
                    || partitionType == DBTablePartitionType.NOT_PARTITIONED) {
                throw new IllegalArgumentException("Table " + schema + "." + tableName + " is not partitioned");
            } else if (partitionPlanExtensionPoint.supports(partition)) {
                throw new IllegalArgumentException("Unsupported partition type, " + partitionType);
            }
        }
        Map<PartitionPlanStrategy, List<PartitionPlanKeyConfig>> strategy2PartitionKeyConfigs = tableConfig
                .getPartitionKeyConfigs().stream().collect(Collectors.groupingBy(PartitionPlanKeyConfig::getStrategy));
        checkPartitionKeyValue(strategy2PartitionKeyConfigs);
        Map<String, Integer> key2Index = new HashMap<>();
        DBTablePartitionOption partitionOption = partition.getPartitionOption();
        if (StringUtils.isNotEmpty(partitionOption.getExpression())) {
            key2Index.put(partitionPlanExtensionPoint.unquoteIdentifier(partitionOption.getExpression()), 0);
        } else if (CollectionUtils.isNotEmpty(partitionOption.getColumnNames())) {
            List<String> keys = partitionOption.getColumnNames();
            for (int i = 0; i < keys.size(); i++) {
                key2Index.put(partitionPlanExtensionPoint.unquoteIdentifier(keys.get(i)), i);
            }
        } else {
            throw new IllegalStateException("Unknown partition key");
        }
        for (PartitionPlanStrategy key : strategy2PartitionKeyConfigs.keySet()) {
            List<PartitionPlanKeyConfig> configs = strategy2PartitionKeyConfigs.get(key);
            if (configs.size() == 1 || configs.stream().anyMatch(i -> i.getPartitionKey() == null)) {
                continue;
            }
            configs.sort((o1, o2) -> {
                String p1 = o1.getPartitionKey();
                String p2 = o2.getPartitionKey();
                Integer idx1 = key2Index.get(partitionPlanExtensionPoint.unquoteIdentifier(p1));
                Integer idx2 = key2Index.get(partitionPlanExtensionPoint.unquoteIdentifier(p2));
                if (idx1 == null || idx2 == null) {
                    throw new IllegalStateException("Unknown error, " + p1 + ", " + p2);
                }
                return idx1.compareTo(idx2);
            });
        }
        Map<PartitionPlanStrategy, List<DBTablePartitionDefinition>> strategyListMap = doPartitionPlan(connection,
                schema, tableName, tableConfig, partition.getPartitionDefinitions(), strategy2PartitionKeyConfigs,
                partitionPlanExtensionPoint);
        return strategyListMap.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> {
            switch (e.getKey()) {
                case DROP:
                    return partitionPlanExtensionPoint.getDropPartitionDdls(e.getValue());
                case CREATE:
                    return partitionPlanExtensionPoint.getCreatePartitionDdls(e.getValue());
                default:
                    return Collections.emptyList();
            }
        }));
    }

    private Map<PartitionPlanStrategy, List<DBTablePartitionDefinition>> doPartitionPlan(
            Connection connection, String schema, String tableName,
            PartitionPlanTableConfig tableConfig, List<DBTablePartitionDefinition> definitions,
            Map<PartitionPlanStrategy, List<PartitionPlanKeyConfig>> strategy2PartitionKeyConfigs,
            PartitionPlanExtensionPoint partitionPlanExtensionPoint) {
        Map<Integer, List<String>> lineNum2CreateExprs = new HashMap<>();
        List<DBTablePartitionDefinition> droppedPartitions = new ArrayList<>();
        Map<PartitionPlanStrategy, List<DBTablePartitionDefinition>> strategyListMap = new HashMap<>();
        for (PartitionPlanStrategy strategy : strategy2PartitionKeyConfigs.keySet()) {
            for (PartitionPlanKeyConfig config : strategy2PartitionKeyConfigs.get(strategy)) {
                switch (strategy) {
                    case CREATE:
                        PartitionExprGenerator createInvoker = partitionPlanExtensionPoint
                                .getPartitionExpressionGeneratorByName(config.getPartitionKeyInvoker());
                        if (createInvoker == null) {
                            throw new IllegalStateException(
                                    "Failed to get invoker by name, " + config.getPartitionKeyInvoker());
                        }
                        List<String> exprs = createInvoker.invoke(connection, schema,
                                tableName, config.getPartitionKeyInvokerParameters());
                        for (int i = 0; i < exprs.size(); i++) {
                            lineNum2CreateExprs.computeIfAbsent(i, key -> new ArrayList<>()).add(exprs.get(i));
                        }
                        break;
                    case DROP:
                        DropPartitionGenerator dropInvoker = partitionPlanExtensionPoint
                                .getDropPartitionGeneratorByName(config.getPartitionKeyInvoker());
                        if (dropInvoker == null) {
                            throw new IllegalStateException(
                                    "Failed to get invoker by name, " + config.getPartitionKeyInvoker());
                        }
                        Map<String, Object> parameters = config.getPartitionKeyInvokerParameters();
                        parameters.put(DropPartitionGenerator.PARTITION_CANDIDATE_KEY, definitions);
                        droppedPartitions.addAll(dropInvoker.invoke(connection, schema, tableName, parameters));
                    default:
                        throw new UnsupportedOperationException("Unsupported partition strategy, " + strategy);
                }
            }
        }
        strategyListMap.put(PartitionPlanStrategy.CREATE, lineNum2CreateExprs.values().stream().map(s -> {
            DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
            definition.setMaxValues(s);
            PartitionNameGenerator invoker = partitionPlanExtensionPoint
                    .getPartitionNameGeneratorGeneratorByName(tableConfig.getPartitionNameInvoker());
            if (invoker == null) {
                throw new IllegalStateException(
                        "Failed to get invoker by name, " + tableConfig.getPartitionNameInvoker());
            }
            Map<String, Object> parameters = tableConfig.getPartitionNameInvokerParameters();
            parameters.putIfAbsent(PartitionNameGenerator.TARGET_PARTITION_DEF_KEY, definition);
            definition.setName(invoker.invoke(connection, schema, tableName, parameters));
            return definition;
        }).collect(Collectors.toList()));
        strategyListMap.put(PartitionPlanStrategy.DROP, droppedPartitions);
        return strategyListMap;
    }

    private void checkPartitionKeyValue(
            Map<PartitionPlanStrategy, List<PartitionPlanKeyConfig>> strategy2PartitionKeyConfigs) {
        strategy2PartitionKeyConfigs.forEach((key, value) -> {
            switch (key) {
                case DROP:
                    Validate.isTrue(value.stream().anyMatch(i -> i.getPartitionKey() == null),
                            "Drop partition strategy is not for a specific key");
                    Validate.isTrue(value.size() == 1, "Drop strategy should be singleton");
                    break;
                case CREATE:
                    Validate.isTrue(value.stream().anyMatch(i -> i.getPartitionKey() != null),
                            "Create partition strategy is for a specific key");
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported partition strategy, " + key);
            }
        });
    }

}
