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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.create;

import java.sql.Connection;
import java.util.Date;

import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype.BasePartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator.SqlExprResult;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMySQLTimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessor;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.datatype.OBOraclePartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.OBOracleSqlExprCalculator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.mapper.CellDataProcessors;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OBOracleTimeIncreasePartitionExprGenerator}
 *
 * @author yh263208 @da 2024-01-26 15:45
 * @since ODC_release_4.2.4
 * @see OBMySQLTimeIncreasePartitionExprGenerator
 */
public class OBOracleTimeIncreasePartitionExprGenerator extends OBMySQLTimeIncreasePartitionExprGenerator {

    @Override
    protected SqlExprCalculator getSqlExprCalculator(Connection connection) {
        return new OBOracleSqlExprCalculator(connection);
    }

    @Override
    protected BasePartitionKeyDataTypeFactory getDataTypeFactory(Connection connection,
            DBTable dbTable, String partitionKey) {
        return new OBOraclePartitionKeyDataTypeFactory(getSqlExprCalculator(connection), dbTable, partitionKey);
    }

    @Override
    protected CellDataProcessor getCellDataProcessor(@NonNull DataType dataType) {
        return CellDataProcessors.getByDataType(dataType);
    }

    @Override
    protected Date getPartitionUpperBound(@NonNull SqlExprCalculator calculator, @NonNull String partitionKey,
            @NonNull String upperBound) {
        SqlExprResult value = calculator.calculate(upperBound);
        if (!(value.getValue() instanceof TimeFormatResult)) {
            throw new IllegalStateException(upperBound + " isn't a date, " + value.getDataType().getDataTypeName());
        }
        return new Date(((TimeFormatResult) value.getValue()).getTimestamp());
    }

    @Override
    protected Object convertCandidate(Date candidate, DataType dataType, TimeIncreaseGeneratorConfig config) {
        TimeFormatResult result = new TimeFormatResult();
        result.setTimestamp(candidate.getTime());
        return result;
    }

}
