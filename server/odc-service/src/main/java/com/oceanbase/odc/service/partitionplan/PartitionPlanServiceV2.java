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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.AutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.PartitionPlanVariableKey;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanKeyConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanPreViewResp;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanVariable;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.plugin.TaskPluginUtil;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link PartitionPlanServiceV2}
 *
 * @author yh263208
 * @date 2024-01-11 15:36
 * @since ODC_release_4.2.4
 */
@Slf4j
@Service
public class PartitionPlanServiceV2 {

    @Autowired
    private ConnectSessionService sessionService;

    public List<PartitionPlanPreViewResp> preview(@NonNull String sessionId,
            @NonNull List<PartitionPlanTableConfig> tableConfigs, Boolean onlyForPartitionName) {
        ConnectionSession connectionSession = sessionService.nullSafeGet(sessionId, true);
        DialectType dialectType = connectionSession.getDialectType();
        AutoPartitionExtensionPoint extensionPoint = TaskPluginUtil.getAutoPartitionExtensionPoint(dialectType);
        if (extensionPoint == null) {
            throw new UnsupportedOperationException("Unsupported dialect " + dialectType);
        }
        List<String> tableNames = tableConfigs.stream().map(PartitionPlanTableConfig::getTableName)
                .collect(Collectors.toList());
        String schema = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        SyncJdbcExecutor jdbc = connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
        Map<String, DBTable> name2Table =
                jdbc.execute((ConnectionCallback<List<DBTable>>) con -> extensionPoint.listAllPartitionedTables(con,
                        schema, tableNames)).stream().collect(Collectors.toMap(DBTable::getName, dbTable -> dbTable));
        if (Boolean.TRUE.equals(onlyForPartitionName)) {
            return tableConfigs.stream().map(i -> jdbc.execute((ConnectionCallback<PartitionPlanPreViewResp>) con -> {
                try {
                    PartitionPlanPreViewResp returnVal = new PartitionPlanPreViewResp();
                    String tableName = i.getTableName();
                    returnVal.setTableName(tableName);
                    returnVal.setPartitionName(generatePartitionName(con, dialectType, name2Table.get(tableName), i));
                    return returnVal;
                } catch (Exception e) {
                    log.warn("Failed to generate partition name", e);
                    throw new IllegalStateException(e);
                }
            })).collect(Collectors.toList());
        }
        return tableConfigs.stream().map(i -> jdbc.execute((ConnectionCallback<PartitionPlanPreViewResp>) con -> {
            try {
                String tableName = i.getTableName();
                Map<PartitionPlanStrategy, List<String>> resp = generatePartitionDdl(
                        con, dialectType, name2Table.get(tableName), i);
                PartitionPlanPreViewResp returnVal = new PartitionPlanPreViewResp();
                returnVal.setTableName(tableName);
                returnVal.setSqls(resp.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
                return returnVal;
            } catch (Exception e) {
                log.warn("Failed to generate partition ddl", e);
                throw new IllegalStateException(e);
            }
        })).collect(Collectors.toList());
    }

    public List<PartitionPlanVariable> getSupportedVariables() {
        return Arrays.stream(PartitionPlanVariableKey.values())
                .map(PartitionPlanVariable::new).collect(Collectors.toList());
    }

    /**
     * generate the ddl of a partition plan
     */
    public Map<PartitionPlanStrategy, List<String>> generatePartitionDdl(@NonNull Connection connection,
            @NonNull DialectType dialectType, @NonNull String schema,
            @NonNull PartitionPlanTableConfig tableConfig) throws Exception {
        TableExtensionPoint tableExtensionPoint = SchemaPluginUtil.getTableExtension(dialectType);
        if (tableExtensionPoint == null) {
            throw new UnsupportedOperationException("Unsupported dialect " + dialectType);
        }
        DBTable dbTable = tableExtensionPoint.getDetail(connection, schema, tableConfig.getTableName());
        return generatePartitionDdl(connection, dialectType, dbTable, tableConfig);
    }

    public Map<PartitionPlanStrategy, List<String>> generatePartitionDdl(@NonNull Connection connection,
            @NonNull DialectType dialectType, @NonNull DBTable dbTable,
            @NonNull PartitionPlanTableConfig tableConfig) throws Exception {
        AutoPartitionExtensionPoint autoPartitionExtensionPoint = TaskPluginUtil
                .getAutoPartitionExtensionPoint(dialectType);
        if (autoPartitionExtensionPoint == null) {
            throw new UnsupportedOperationException("Unsupported dialect " + dialectType);
        }
        String tableName = tableConfig.getTableName();
        List<DBTableColumn> columns = dbTable.getColumns();
        if (CollectionUtils.isEmpty(columns)) {
            throw new NotFoundException(ResourceType.OB_TABLE, "tableName", tableName);
        }
        DBTablePartition partition = dbTable.getPartition();
        if (partition == null || partition.getPartitionOption() == null) {
            throw new IllegalArgumentException("Partition is null for " + tableName);
        } else {
            DBTablePartitionType partitionType = partition.getPartitionOption().getType();
            if (partitionType == DBTablePartitionType.UNKNOWN
                    || partitionType == DBTablePartitionType.NOT_PARTITIONED) {
                throw new IllegalArgumentException("Table " + tableName + " is not partitioned");
            } else if (!autoPartitionExtensionPoint.supports(partition)) {
                throw new IllegalArgumentException("Unsupported partition type, " + partitionType);
            }
        }
        Map<PartitionPlanStrategy, List<PartitionPlanKeyConfig>> strategy2PartitionKeyConfigs = tableConfig
                .getPartitionKeyConfigs().stream().collect(Collectors.groupingBy(PartitionPlanKeyConfig::getStrategy));
        checkPartitionKeyValue(strategy2PartitionKeyConfigs);
        Map<String, Integer> key2Index = new HashMap<>();
        DBTablePartitionOption partitionOption = partition.getPartitionOption();
        if (CollectionUtils.isNotEmpty(partitionOption.getColumnNames())) {
            List<String> keys = partitionOption.getColumnNames();
            for (int i = 0; i < keys.size(); i++) {
                key2Index.put(autoPartitionExtensionPoint.unquoteIdentifier(keys.get(i)), i);
            }
        } else if (StringUtils.isNotEmpty(partitionOption.getExpression())) {
            key2Index.put(autoPartitionExtensionPoint.unquoteIdentifier(partitionOption.getExpression()), 0);
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
                Integer idx1 = key2Index.get(autoPartitionExtensionPoint.unquoteIdentifier(p1));
                Integer idx2 = key2Index.get(autoPartitionExtensionPoint.unquoteIdentifier(p2));
                if (idx1 == null || idx2 == null) {
                    throw new IllegalStateException("Unknown error, " + p1 + ", " + p2);
                }
                return idx1.compareTo(idx2);
            });
        }
        Map<PartitionPlanStrategy, DBTablePartition> strategyListMap = doPartitionPlan(connection, dbTable,
                tableConfig, autoPartitionExtensionPoint, strategy2PartitionKeyConfigs);
        return strategyListMap.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> {
            switch (e.getKey()) {
                case DROP:
                    return autoPartitionExtensionPoint.generateDropPartitionDdls(connection, e.getValue(), true);
                case CREATE:
                    return autoPartitionExtensionPoint.generateCreatePartitionDdls(connection, e.getValue());
                default:
                    return Collections.emptyList();
            }
        }));
    }

    public String generatePartitionName(@NonNull Connection connection, @NonNull DialectType dialectType,
            @NonNull String schema, @NonNull PartitionPlanTableConfig tableConfig) throws Exception {
        TableExtensionPoint tableExtensionPoint = SchemaPluginUtil.getTableExtension(dialectType);
        if (tableExtensionPoint == null) {
            throw new UnsupportedOperationException("Unsupported dialect " + dialectType);
        }
        DBTable dbTable = tableExtensionPoint.getDetail(connection, schema, tableConfig.getTableName());
        return generatePartitionName(connection, dialectType, dbTable, tableConfig);
    }

    public String generatePartitionName(@NonNull Connection connection, @NonNull DialectType dialectType,
            @NonNull DBTable dbTable, @NonNull PartitionPlanTableConfig tableConfig) throws Exception {
        AutoPartitionExtensionPoint autoPartitionExtensionPoint = TaskPluginUtil
                .getAutoPartitionExtensionPoint(dialectType);
        if (autoPartitionExtensionPoint == null) {
            throw new UnsupportedOperationException("Unsupported dialect " + dialectType);
        }
        DBTablePartition partition = dbTable.getPartition();
        if (partition == null || partition.getPartitionOption() == null) {
            throw new IllegalArgumentException("Partition is null");
        } else if (!autoPartitionExtensionPoint.supports(partition)) {
            throw new IllegalArgumentException("Unsupported partition type");
        }
        PartitionNameGenerator generator = autoPartitionExtensionPoint
                .getPartitionNameGeneratorGeneratorByName(tableConfig.getPartitionNameInvoker());
        if (generator == null) {
            throw new IllegalStateException("Failed to get invoker by name, " + tableConfig.getPartitionNameInvoker());
        }
        Map<String, Object> parameters = tableConfig.getPartitionNameInvokerParameters();
        int size = partition.getPartitionDefinitions().size();
        if (size <= 0) {
            throw new IllegalStateException("Partition definitions is empty");
        }
        DBTablePartitionDefinition lastDef = partition.getPartitionDefinitions().get(size - 1);
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_KEY, lastDef);
        parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_INDEX_KEY, 0);
        return generator.invoke(connection, dbTable, parameters);
    }

    private Map<PartitionPlanStrategy, DBTablePartition> doPartitionPlan(Connection connection, DBTable dbTable,
            PartitionPlanTableConfig tableConfig, AutoPartitionExtensionPoint extensionPoint,
            Map<PartitionPlanStrategy, List<PartitionPlanKeyConfig>> strategy2PartitionKeyConfigs) throws Exception {
        Map<Integer, List<String>> lineNum2CreateExprs = new HashMap<>();
        List<DBTablePartitionDefinition> droppedPartitions = new ArrayList<>();
        Map<PartitionPlanStrategy, List<DBTablePartitionDefinition>> strategyListMap = new HashMap<>();
        for (PartitionPlanStrategy strategy : strategy2PartitionKeyConfigs.keySet()) {
            for (PartitionPlanKeyConfig config : strategy2PartitionKeyConfigs.get(strategy)) {
                switch (strategy) {
                    case CREATE:
                        PartitionExprGenerator createInvoker = extensionPoint
                                .getPartitionExpressionGeneratorByName(config.getPartitionKeyInvoker());
                        if (createInvoker == null) {
                            throw new IllegalStateException(
                                    "Failed to get invoker by name, " + config.getPartitionKeyInvoker());
                        }
                        List<String> exprs = createInvoker.invoke(connection, dbTable,
                                config.getPartitionKeyInvokerParameters());
                        for (int i = 0; i < exprs.size(); i++) {
                            lineNum2CreateExprs.computeIfAbsent(i, key -> new ArrayList<>()).add(exprs.get(i));
                        }
                        break;
                    case DROP:
                        DropPartitionGenerator dropInvoker = extensionPoint
                                .getDropPartitionGeneratorByName(config.getPartitionKeyInvoker());
                        if (dropInvoker == null) {
                            throw new IllegalStateException(
                                    "Failed to get invoker by name, " + config.getPartitionKeyInvoker());
                        }
                        droppedPartitions.addAll(dropInvoker.invoke(connection, dbTable,
                                config.getPartitionKeyInvokerParameters()));
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported partition strategy, " + strategy);
                }
            }
        }
        strategyListMap.put(PartitionPlanStrategy.CREATE, lineNum2CreateExprs.entrySet().stream().map(s -> {
            DBTablePartitionDefinition definition = new DBTablePartitionDefinition();
            definition.setMaxValues(s.getValue());
            PartitionNameGenerator invoker = extensionPoint
                    .getPartitionNameGeneratorGeneratorByName(tableConfig.getPartitionNameInvoker());
            if (invoker == null) {
                throw new IllegalStateException(
                        "Failed to get invoker by name, " + tableConfig.getPartitionNameInvoker());
            }
            Map<String, Object> parameters = tableConfig.getPartitionNameInvokerParameters();
            parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_KEY, definition);
            parameters.put(PartitionNameGenerator.TARGET_PARTITION_DEF_INDEX_KEY, s.getKey());
            try {
                definition.setName(invoker.invoke(connection, dbTable, parameters));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return definition;
        }).filter(d -> removeExistingPartitionElement(dbTable, d, extensionPoint)).collect(Collectors.toList()));
        strategyListMap.put(PartitionPlanStrategy.DROP, droppedPartitions);
        DBTablePartition partition = dbTable.getPartition();
        return strategyListMap.entrySet().stream().filter(e -> CollectionUtils.isNotEmpty(e.getValue()))
                .collect(Collectors.toMap(Entry::getKey, e -> {
                    DBTablePartition dbTablePartition = new DBTablePartition();
                    dbTablePartition.setPartitionDefinitions(e.getValue());
                    dbTablePartition.setTableName(dbTable.getName());
                    dbTablePartition.setSchemaName(dbTable.getSchemaName());
                    dbTablePartition.setPartitionOption(partition.getPartitionOption());
                    return dbTablePartition;
                }));
    }

    private boolean removeExistingPartitionElement(DBTable dbTable, DBTablePartitionDefinition definition,
            AutoPartitionExtensionPoint extensionPoint) {
        return dbTable.getPartition().getPartitionDefinitions().stream().noneMatch(i -> {
            if (Objects.equals(extensionPoint.unquoteIdentifier(i.getName()),
                    extensionPoint.unquoteIdentifier(definition.getName()))) {
                return true;
            }
            int size = Math.min(i.getMaxValues().size(), definition.getMaxValues().size());
            for (int k = 0; k < size; k++) {
                if (Objects.equals(i.getMaxValues().get(k), definition.getMaxValues().get(k))) {
                    return true;
                }
            }
            return false;
        });
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
