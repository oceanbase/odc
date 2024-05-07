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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan;

import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.connect.oboracle.OBOracleInformationExtension;
import com.oceanbase.odc.plugin.schema.oboracle.browser.DBSchemaAccessors;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.PartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.PartitionNameGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.drop.OBMySQLKeepLatestPartitionGenerator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.datatype.OBOraclePartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.OBOracleSqlExprCalculator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.create.OBOracleSqlExprPartitionExprGenerator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.create.OBOracleTimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.partitionname.OBOracleDateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.partitionname.OBOracleExprBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.util.DBTablePartitionEditors;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.adapter.oracle.OracleSubPartitionElementFactory;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import com.oceanbase.tools.sqlparser.oboracle.OBParser.Range_expr_listContext;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;

/**
 * {@link OBOracleAutoPartitionExtensionPoint}
 *
 * @author yh263208
 * @date 2024-01-26 13:38
 * @since ODC_release_4.2.4
 * @see OBMySQLAutoPartitionExtensionPoint
 */
@Extension
public class OBOracleAutoPartitionExtensionPoint extends OBMySQLAutoPartitionExtensionPoint {

    @Override
    public String unquoteIdentifier(@NonNull String identifier) {
        return ConnectionSessionUtil.getUserOrSchemaString(identifier, DialectType.OB_ORACLE);
    }

    @Override
    public List<DataType> getPartitionKeyDataTypes(@NonNull Connection connection, @NonNull DBTable table) {
        if (!supports(table.getPartition())) {
            throw new UnsupportedOperationException("Unsupported db table");
        }
        SqlExprCalculator calculator = new OBOracleSqlExprCalculator(connection);
        DBTablePartitionOption option = table.getPartition().getPartitionOption();
        List<String> keys = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(option.getColumnNames())) {
            keys.addAll(option.getColumnNames());
        } else if (StringUtils.isNotEmpty(option.getExpression())) {
            keys.add(option.getExpression());
        } else {
            throw new IllegalStateException("Partition type is unknown, expression and columns are both null");
        }
        return keys.stream().map(c -> new OBOraclePartitionKeyDataTypeFactory(calculator, table, c).generate())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> generateCreatePartitionDdls(@NonNull Connection connection,
            @NonNull DBTablePartition partition) {
        InformationExtensionPoint extensionPoint = new OBOracleInformationExtension();
        DBTablePartitionEditor editor = DBTablePartitionEditors.generate(extensionPoint.getDBVersion(connection));
        return Collections.singletonList(editor.generateAddPartitionDefinitionDDL(partition.getSchemaName(),
                partition.getTableName(), partition.getPartitionOption(), partition.getPartitionDefinitions()));
    }

    @Override
    public List<String> generateDropPartitionDdls(@NonNull Connection connection,
            @NonNull DBTablePartition partition, boolean reloadIndexes) {
        InformationExtensionPoint extensionPoint = new OBOracleInformationExtension();
        DBTablePartitionEditor editor = DBTablePartitionEditors.generate(extensionPoint.getDBVersion(connection));
        String ddl = editor.generateDropPartitionDefinitionDDL(partition.getSchemaName(),
                partition.getTableName(), partition.getPartitionDefinitions());
        int index = ddl.indexOf(";");
        if (index < 0 || !reloadIndexes) {
            return Collections.singletonList(ddl);
        }
        return Collections.singletonList(ddl.substring(0, index) + " UPDATE GLOBAL INDEXES;");
    }

    @Override
    public DropPartitionGenerator getDropPartitionGeneratorByName(@NonNull String name) {
        List<DropPartitionGenerator> candidates = new ArrayList<>(4);
        candidates.add(new OBMySQLKeepLatestPartitionGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

    @Override
    public PartitionExprGenerator getPartitionExpressionGeneratorByName(@NonNull String name) {
        List<PartitionExprGenerator> candidates = new ArrayList<>(2);
        candidates.add(new OBOracleSqlExprPartitionExprGenerator());
        candidates.add(new OBOracleTimeIncreasePartitionExprGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

    @Override
    public PartitionNameGenerator getPartitionNameGeneratorGeneratorByName(@NonNull String name) {
        List<PartitionNameGenerator> candidates = new ArrayList<>(2);
        candidates.add(new OBOracleDateBasedPartitionNameGenerator());
        candidates.add(new OBOracleExprBasedPartitionNameGenerator());
        return candidates.stream().filter(i -> Objects.equals(i.getName(), name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find generator by name " + name));
    }

    @Override
    protected List<String> flatMapMaxValues(List<String> maxValues) {
        RangePartiExprParser parser = new RangePartiExprParser();
        return maxValues.stream().filter(StringUtils::isNotEmpty).flatMap(s -> {
            try {
                Range_expr_listContext cxt = (Range_expr_listContext) parser.buildAst(new StringReader(s));
                return OracleSubPartitionElementFactory.getRangePartitionExprs(cxt).stream().map(Statement::getText);
            } catch (Exception e) {
                return Stream.of(s);
            }
        }).collect(Collectors.toList());
    }

    @Override
    protected void processPartitionKey(DBTablePartitionOption option) {
        if (StringUtils.isNotEmpty(option.getExpression())) {
            option.setColumnNames(Arrays.stream(option.getExpression().split(","))
                    .map(String::trim).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(option.getColumnNames())) {
            option.setColumnNames(option.getColumnNames().stream().map(String::trim).collect(Collectors.toList()));
        }
    }

    @Override
    protected DBSchemaAccessor getDBSchemaAccessor(@NonNull Connection connection, String tenantName) {
        JdbcOperations jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, false));
        return DBSchemaAccessors.create(jdbc, new OBOracleInformationExtension().getDBVersion(connection));
    }

    static private class RangePartiExprParser extends OBOracleSQLParser {
        @Override
        protected ParseTree doParse(OBParser parser) {
            return parser.range_expr_list();
        }
    }

}
