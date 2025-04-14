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
import com.oceanbase.odc.plugin.task.api.partitionplan.model.NumberIncreaseGeneratorConfig;
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
            @NonNull NumberIncreaseGeneratorConfig config) throws Exception {
        String numberInterval = config.getNumberInterval();
        DBTablePartitionOption option = dbTable.getPartition().getPartitionOption();
        String lastPartiValue;
        List<DBTablePartitionDefinition> definitions = dbTable.getPartition().getPartitionDefinitions();
        DBTablePartitionDefinition lastDef = definitions.get(definitions.size() - 1);
        List<String> columnNames = option.getColumnNames();
        String realName = unquoteIdentifier(partitionKey);
        if (CollectionUtils.isNotEmpty(columnNames)) {
            int i;
            for (i = 0; i < columnNames.size(); i++) {
                if (realName.equals(unquoteIdentifier(columnNames.get(i)))) {
                    break;
                }
            }
            if (i >= columnNames.size()) {
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
        boolean isQuoted = lastPartiValue.contains("'");
        return candidates.stream().map(sqlExprResult -> {
            DataType type = sqlExprResult.getDataType();
            String value = getCellDataProcessor(type).convertToSqlLiteral(sqlExprResult.getValue(), type);
            if (isQuoted) {
                return "'" + value + "'";
            }
            return value;
        }).collect(Collectors.toList());
    }

    protected CellDataProcessor getCellDataProcessor(@NonNull DataType dataType) {
        return CellDataProcessors.getByDataType(dataType);
    }

    protected String unquoteIdentifier(String identifier) {
        return StringUtils.unquoteMySqlIdentifier(identifier);
    }

    protected SqlExprCalculator getSqlExprCalculator(Connection connection) {
        return new OBMySQLExprCalculator(connection);
    }

}
