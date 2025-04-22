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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.partitionname;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator.SqlExprResult;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.partitionname.OBMySQLDateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.OBOracleAutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.OBOracleSqlExprCalculator;

import lombok.NonNull;

/**
 * {@link OBOracleDateBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-03-21 21:28
 * @since ODC_release_4.2.4
 * @see OBMySQLDateBasedPartitionNameGenerator
 */
public class OBOracleDateBasedPartitionNameGenerator extends OBMySQLDateBasedPartitionNameGenerator {

    @Override
    protected String unquoteIdentifier(String identifier) {
        return new OBOracleAutoPartitionExtensionPoint().unquoteIdentifier(identifier);
    }

    @Override
    protected Date getPartitionUpperBound(@NonNull Connection connection,
            @NonNull String partitionKey, @NonNull String upperBound, String namingSuffixExpression) {
        SqlExprCalculator calculator = new OBOracleSqlExprCalculator(connection);
        SqlExprResult value = calculator.calculate(upperBound);
        if ((value.getValue() instanceof TimeFormatResult)) {
            return new Date(((TimeFormatResult) value.getValue()).getTimestamp());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(namingSuffixExpression);
        try {
            return sdf.parse(unquoteValue(upperBound));
        } catch (ParseException e) {
            throw new IllegalStateException(
                    "naming suffix expression is not a valid date format, please check the format as same as the partition key.");
        }
    }

    @Override
    protected String unquoteValue(String value) {
        return StringUtils.unquoteOracleValue(value);
    }

}
