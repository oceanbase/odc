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
import java.text.SimpleDateFormat;
import java.util.Date;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.DateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.DBTablePartitionUtil;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator.SqlExprResult;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link OBMySQLDateBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-25 15:01
 * @since ODC_release_4.2.4
 */
public class OBMySQLDateBasedPartitionNameGenerator implements DateBasedPartitionNameGenerator {

    @Override
    public String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Integer targetPartitionIndex, @NonNull DBTablePartitionDefinition target,
            @NonNull DateBasedPartitionNameGeneratorConfig config) {
        int index = DBTablePartitionUtil.getPartitionKeyIndex(
                dbTable, config.getRefPartitionKey(), this::unquoteIdentifier);
        Date baseDate = getPartitionUpperBound(
                connection, config.getRefPartitionKey(), target.getMaxValues().get(index));
        return config.getNamingPrefix() + new SimpleDateFormat(config.getNamingSuffixExpression()).format(baseDate);
    }

    protected String unquoteIdentifier(String identifier) {
        return new OBMySQLAutoPartitionExtensionPoint().unquoteIdentifier(identifier);
    }

    protected Date getPartitionUpperBound(@NonNull Connection connection,
            @NonNull String partitionKey, @NonNull String upperBound) {
        SqlExprCalculator calculator = new OBMySQLExprCalculator(connection);
        SqlExprResult value = calculator.calculate("convert(" + upperBound + ", datetime)");
        if (!(value.getValue() instanceof Date)) {
            throw new IllegalStateException(upperBound + " isn't a date, " + value.getDataType().getDataTypeName());
        }
        return (Date) value.getValue();
    }

}
