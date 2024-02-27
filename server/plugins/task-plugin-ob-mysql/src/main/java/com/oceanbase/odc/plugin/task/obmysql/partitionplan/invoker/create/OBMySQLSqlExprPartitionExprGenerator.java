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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.StringSubstitutor;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.SqlExprBasedPartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.PartitionPlanVariableKey;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.SqlExprBasedGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator.SqlExprResult;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessor;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessors;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OBMySQLSqlExprPartitionExprGenerator}
 *
 * @author yh263208
 * @date 2024-01-25 09:43
 * @since ODC_release_4.2.4
 * @see SqlExprBasedPartitionExprGenerator
 */
public class OBMySQLSqlExprPartitionExprGenerator implements SqlExprBasedPartitionExprGenerator {

    @Override
    public List<String> generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull String partitionKey,
            @NonNull Integer generateCount, @NonNull SqlExprBasedGeneratorConfig config) {
        String realInterval = config.getIntervalGenerateExpr();
        String realExpression = config.getGenerateExpr();
        for (PartitionPlanVariableKey key : PartitionPlanVariableKey.values()) {
            realInterval = replaceVariable(dbTable, partitionKey, realInterval, key);
            realExpression = replaceVariable(dbTable, partitionKey, realExpression, key);
        }
        List<String> intervals = new ArrayList<>(generateCount);
        intervals.add(realInterval);
        List<SqlExprResult> candidates = new ArrayList<>(generateCount);
        SqlExprCalculator calculator = getSqlExprCalculator(connection);
        for (int i = 0; i < generateCount; i++) {
            if (StringUtils.isEmpty(realInterval)) {
                candidates.add(calculator.calculate(realExpression));
            } else {
                Map<String, String> variables = new HashMap<>();
                variables.put(PartitionPlanVariableKey.INTERVAL.name(), "(" + String.join("+", intervals) + ")");
                intervals.add(realInterval);
                StringSubstitutor substitutor = new StringSubstitutor(variables);
                candidates.add(calculator.calculate(substitutor.replace(realExpression)));
            }
        }
        return candidates.stream().map(sqlExprResult -> {
            DataType type = sqlExprResult.getDataType();
            return getCellDataProcessor(type).convertToSqlLiteral(sqlExprResult.getValue(), type);
        }).collect(Collectors.toList());
    }

    protected SqlExprCalculator getSqlExprCalculator(Connection connection) {
        return new OBMySQLExprCalculator(connection);
    }

    protected CellDataProcessor getCellDataProcessor(@NonNull DataType dataType) {
        return CellDataProcessors.getByDataType(dataType);
    }

    protected String unquoteIdentifier(String identifier) {
        return new OBMySQLAutoPartitionExtensionPoint().unquoteIdentifier(identifier);
    }

    protected String replaceVariable(DBTable table, String partitionKey, String target, PartitionPlanVariableKey key) {
        if (StringUtils.isEmpty(target)) {
            return target;
        }
        switch (key) {
            case LAST_PARTITION_VALUE:
                DBTablePartitionOption option = table.getPartition().getPartitionOption();
                String lastPartiValue;
                List<DBTablePartitionDefinition> definitions = table.getPartition().getPartitionDefinitions();
                DBTablePartitionDefinition lastDef = definitions.get(definitions.size() - 1);
                if (CollectionUtils.isNotEmpty(option.getColumnNames())) {
                    int i;
                    String realName = unquoteIdentifier(partitionKey);
                    for (i = 0; i < option.getColumnNames().size(); i++) {
                        if (realName.equals(unquoteIdentifier(option.getColumnNames().get(i)))) {
                            break;
                        }
                    }
                    if (i >= option.getColumnNames().size()) {
                        throw new IllegalArgumentException("Unknown partition key, " + partitionKey);
                    }
                    lastPartiValue = lastDef.getMaxValues().get(i);
                } else if (StringUtils.isNotEmpty(option.getExpression())) {
                    lastPartiValue = lastDef.getMaxValues().get(0);
                } else {
                    throw new IllegalStateException("Partition type is unknown, expression and columns are both null");
                }
                Map<String, String> variables = new HashMap<>();
                variables.put(PartitionPlanVariableKey.LAST_PARTITION_VALUE.name(), lastPartiValue);
                return new StringSubstitutor(variables).replace(target);
            case INTERVAL:
                return target;
            default:
                throw new UnsupportedOperationException("Unsupported partition variable, " + key);
        }
    }

}
