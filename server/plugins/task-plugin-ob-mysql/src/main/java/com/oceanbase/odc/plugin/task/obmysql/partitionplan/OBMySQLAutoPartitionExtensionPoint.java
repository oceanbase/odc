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

import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.collections4.CollectionUtils;
import org.pf4j.Extension;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLInformationExtension;
import com.oceanbase.odc.plugin.schema.obmysql.browser.DBSchemaAccessors;
import com.oceanbase.odc.plugin.task.api.partitionplan.AutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype.OBMySQLPartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMySQLHistoricalPartitionPlanCreateGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMySQLSqlExprPartitionExprGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMySQLTimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.drop.OBMySQLHistoricalPartitionPlanDropGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.drop.OBMySQLKeepLatestPartitionGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLDateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLExprBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLHistoricalPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.util.DBTablePartitionEditors;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableAbstractPartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLSubPartitionElementFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Range_partition_exprContext;
import com.oceanbase.tools.sqlparser.statement.Statement;

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
    public List<DBTable> listAllPartitionedTables(@NonNull Connection connection,
            String tenantName, @NonNull String schemaName, List<String> tableNames) {
        DBSchemaAccessor accessor = getDBSchemaAccessor(connection, tenantName);
        Map<String, DBTablePartition> tblName2Parti = accessor.listTablePartitions(schemaName, tableNames);
        tblName2Parti = tblName2Parti.entrySet().stream()
                .filter(e -> supports(e.getValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        if (tblName2Parti.values().stream().anyMatch(p -> p.getPartitionDefinitions().stream()
                .anyMatch(definition -> definition.getOrdinalPosition() != null))) {
            tblName2Parti.values().forEach(p -> p.getPartitionDefinitions()
                    .sort(Comparator.comparing(DBTableAbstractPartitionDefinition::getOrdinalPosition)));
        }
        Map<String, List<DBTableColumn>> tblName2Cols = accessor.listTableColumns(
                schemaName, new ArrayList<>(tblName2Parti.keySet()));
        return tblName2Parti.entrySet().stream().map(e -> {
            DBTable dbTable = new DBTable();
            dbTable.setSchemaName(schemaName);
            dbTable.setName(e.getKey());
            dbTable.setPartition(e.getValue());
            dbTable.setColumns(tblName2Cols.get(e.getKey()));
            return dbTable;
        }).peek(dbTable -> {
            dbTable.getPartition().getPartitionDefinitions()
                    .forEach(d -> d.setMaxValues(flatMapMaxValues(d.getMaxValues())));
            processPartitionKey(dbTable.getPartition().getPartitionOption());
        }).filter(dbTable -> supports(dbTable.getPartition())).collect(Collectors.toList());
    }

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
        List<DropPartitionGenerator> candidates = new ArrayList<>(4);
        candidates.add(new OBMySQLKeepLatestPartitionGenerator());
        candidates.add(new OBMySQLHistoricalPartitionPlanDropGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

    @Override
    public PartitionExprGenerator getPartitionExpressionGeneratorByName(@NonNull String name) {
        List<PartitionExprGenerator> candidates = new ArrayList<>(4);
        candidates.add(new OBMySQLSqlExprPartitionExprGenerator());
        candidates.add(new OBMySQLTimeIncreasePartitionExprGenerator());
        candidates.add(new OBMySQLHistoricalPartitionPlanCreateGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

    @Override
    public PartitionNameGenerator getPartitionNameGeneratorGeneratorByName(@NonNull String name) {
        List<PartitionNameGenerator> candidates = new ArrayList<>(3);
        candidates.add(new OBMySQLDateBasedPartitionNameGenerator());
        candidates.add(new OBMySQLExprBasedPartitionNameGenerator());
        candidates.add(new OBMySQLHistoricalPartitionNameGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

    protected List<String> flatMapMaxValues(List<String> maxValues) {
        RangePartiExprParser parser = new RangePartiExprParser();
        return maxValues.stream().filter(StringUtils::isNotEmpty).flatMap(s -> {
            try {
                Range_partition_exprContext cxt =
                        (Range_partition_exprContext) parser.buildAst(new StringReader("(" + s + ")"));
                return MySQLSubPartitionElementFactory.getRangePartitionExprs(cxt).stream().map(Statement::getText);
            } catch (Exception e) {
                return Stream.of(s);
            }
        }).collect(Collectors.toList());
    }

    protected void processPartitionKey(DBTablePartitionOption option) {
        if (CollectionUtils.isNotEmpty(option.getColumnNames())) {
            option.setColumnNames(option.getColumnNames().stream().map(String::trim).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(option.getExpression())) {
            option.setExpression(option.getExpression().trim());
        }
    }

    protected DBSchemaAccessor getDBSchemaAccessor(@NonNull Connection connection, String tenantName) {
        JdbcOperations jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, false));
        return DBSchemaAccessors.create(jdbc, new OBMySQLInformationExtension().getDBVersion(connection), tenantName);
    }

    static private class RangePartiExprParser extends OBMySQLParser {
        @Override
        protected ParseTree doParse(OBParser parser) {
            return parser.range_partition_expr();
        }
    }

}
