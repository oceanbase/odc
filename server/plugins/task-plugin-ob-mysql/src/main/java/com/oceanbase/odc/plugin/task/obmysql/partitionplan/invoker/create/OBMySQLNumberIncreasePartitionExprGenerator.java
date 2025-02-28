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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.NumberIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.GenericIncreaseGeneratorConfig;
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

public class OBMySQLNumberIncreasePartitionExprGenerator implements NumberIncreasePartitionExprGenerator {

    @Override
    public List<String> generate(@NonNull Connection connection,
            @NonNull DBTable dbTable, @NonNull String partitionKey, @NonNull Integer generateCount,
            @NonNull GenericIncreaseGeneratorConfig config) throws Exception {
        String numberInterval = config.getNumberInterval();
        DBTablePartitionOption option = dbTable.getPartition().getPartitionOption();
        String lastPartiValue;
        List<DBTablePartitionDefinition> definitions = dbTable.getPartition().getPartitionDefinitions();
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

        List<String> intervals = new ArrayList<>(generateCount);
        intervals.add(numberInterval);
        List<SqlExprResult> candidates = new ArrayList<>(generateCount);
        SqlExprCalculator calculator = getSqlExprCalculator(connection);
        for (int i = 0; i < generateCount; i++) {
            String expr = lastPartiValue + "+" + "(" + String.join("+", intervals) + ")";
            intervals.add(numberInterval);
            candidates.add(calculator.calculate(expr));
        }
        return candidates.stream().map(sqlExprResult -> {
            DataType type = sqlExprResult.getDataType();
            return getCellDataProcessor(type).convertToSqlLiteral(sqlExprResult.getValue(), type);
        }).collect(Collectors.toList());
    }

    protected CellDataProcessor getCellDataProcessor(@NonNull DataType dataType) {
        return CellDataProcessors.getByDataType(dataType);
    }

    protected String unquoteIdentifier(String identifier) {
        return new OBMySQLAutoPartitionExtensionPoint().unquoteIdentifier(identifier);
    }

    protected SqlExprCalculator getSqlExprCalculator(Connection connection) {
        return new OBMySQLExprCalculator(connection);
    }

}
