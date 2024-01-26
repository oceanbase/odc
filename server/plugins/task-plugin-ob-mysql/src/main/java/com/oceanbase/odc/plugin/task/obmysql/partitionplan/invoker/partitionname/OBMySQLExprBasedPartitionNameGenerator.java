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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.SqlExprBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.PartitionPlanVariableKey;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.SqlExprBasedGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator.SqlExprResult;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessor;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessors;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
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
        SqlExprResult result = calculator.calculate(getRealGenerateExpression(targetPartitionIndex, config));
        return getCellDataProcessor(result.getDataType()).convertToSqlLiteral(result.getValue(), result.getDataType());
    }

    protected SqlExprCalculator getSqlExprCalculator(Connection connection) {
        return new OBMySQLExprCalculator(connection);
    }

    protected CellDataProcessor getCellDataProcessor(@NonNull DataType dataType) {
        return CellDataProcessors.getByDataType(dataType);
    }

    private String getRealGenerateExpression(Integer targetPartitionIndex, SqlExprBasedGeneratorConfig config) {
        if (StringUtils.isEmpty(config.getIntervalGenerateExpr())) {
            return config.getGenerateExpr();
        }
        List<String> intervals = new ArrayList<>();
        for (int i = 0; i <= targetPartitionIndex; i++) {
            intervals.add(config.getIntervalGenerateExpr());
        }
        Map<String, String> variables = new HashMap<>();
        variables.put(PartitionPlanVariableKey.INTERVAL.name(), "(" + String.join("+", intervals) + ")");
        return new StringSubstitutor(variables).replace(config.getGenerateExpr());
    }

}
