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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.SqlExprBasedPartitionNameGenerator;
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
 * {@link OBMySQLExprBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-25 15:21
 * @since ODC_release_4.2.4
 * @see SqlExprBasedPartitionNameGenerator
 */
public class OBMySQLExprBasedPartitionNameGenerator implements SqlExprBasedPartitionNameGenerator {

    @Override
    public String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Integer targetPartitionIndex, @NonNull DBTablePartitionDefinition target,
            @NonNull SqlExprBasedGeneratorConfig config) {
        SqlExprCalculator calculator = getSqlExprCalculator(connection);
        SqlExprResult result = calculator.calculate(getRealGenerateExpression(
                dbTable, target, targetPartitionIndex, config));
        return getCellDataProcessor(result.getDataType()).convertToSqlLiteral(result.getValue(), result.getDataType());
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

    private String getRealGenerateExpression(DBTable dbTable,
            DBTablePartitionDefinition definition,
            Integer targetPartitionIndex, SqlExprBasedGeneratorConfig config) {
        Map<String, String> variables = new HashMap<>();
        setIntervalVariable(config, targetPartitionIndex, variables);
        setPartitionKeyUpperBoundVariable(dbTable, definition, variables);
        if (variables.isEmpty()) {
            return config.getGenerateExpr();
        }
        return new StringSubstitutor(variables).replace(config.getGenerateExpr());
    }

    private void setIntervalVariable(SqlExprBasedGeneratorConfig config,
            Integer targetPartitionIndex, Map<String, String> variables) {
        if (StringUtils.isEmpty(config.getIntervalGenerateExpr())) {
            return;
        }
        List<String> intervals = new ArrayList<>();
        for (int i = 0; i <= targetPartitionIndex; i++) {
            intervals.add(config.getIntervalGenerateExpr());
        }
        variables.put(PartitionPlanVariableKey.INTERVAL.name(), "(" + String.join("+", intervals) + ")");
    }

    private void setPartitionKeyUpperBoundVariable(DBTable dbTable,
            DBTablePartitionDefinition definition, Map<String, String> variables) {
        DBTablePartitionOption option = dbTable.getPartition().getPartitionOption();
        if (option == null) {
            throw new IllegalStateException("Partition option is missing");
        }
        List<String> cols = option.getColumnNames();
        if (CollectionUtils.isNotEmpty(cols)) {
            for (int i = 0; i < cols.size(); i++) {
                variables.put(unquoteIdentifier(cols.get(i)), definition.getMaxValues().get(i));
            }
        } else if (StringUtils.isNotEmpty(option.getExpression())) {
            variables.put(unquoteIdentifier(option.getExpression()), definition.getMaxValues().get(0));
        }
    }

}
