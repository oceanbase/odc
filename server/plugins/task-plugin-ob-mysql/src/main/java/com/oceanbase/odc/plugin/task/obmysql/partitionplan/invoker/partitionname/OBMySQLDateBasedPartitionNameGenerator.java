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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.DateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.NamingSuffixStrategy;
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
            @NonNull Integer targetPartitionIndex, @NonNull List<DBTablePartitionDefinition> targets,
            @NonNull DateBasedPartitionNameGeneratorConfig config) {
        int index = DBTablePartitionUtil.getPartitionKeyIndex(
                dbTable, config.getRefPartitionKey(), this::unquoteIdentifier);
        DBTablePartitionDefinition baseDef;
        if (config.getNamingSuffixStrategy() == NamingSuffixStrategy.PARTITION_LOWER_BOUND) {
            baseDef = targetPartitionIndex == 0
                    ? dbTable.getPartition().getPartitionDefinitions()
                            .get(dbTable.getPartition().getPartitionDefinitions().size() - 1)
                    : targets.get(targetPartitionIndex - 1);
        } else {
            baseDef = targets.get(targetPartitionIndex);
        }
        Date baseDate = getPartitionUpperBound(
                connection, config.getRefPartitionKey(), baseDef.getMaxValues().get(index),
                config.getNamingSuffixExpression());
        return config.getNamingPrefix() + new SimpleDateFormat(config.getNamingSuffixExpression()).format(baseDate);
    }

    protected String unquoteIdentifier(String identifier) {
        return new OBMySQLAutoPartitionExtensionPoint().unquoteIdentifier(identifier);
    }

    protected Date getPartitionUpperBound(@NonNull Connection connection,
            @NonNull String partitionKey, @NonNull String upperBound, String namingSuffixExpression) {
        SqlExprCalculator calculator = new OBMySQLExprCalculator(connection);
        SqlExprResult value = calculator.calculate("convert(" + upperBound + ", datetime)");
        if ((value.getValue() instanceof Date)) {
            return (Date) value.getValue();
        }
        SimpleDateFormat sdf = new SimpleDateFormat(namingSuffixExpression);
        try {
            return sdf.parse(unquoteValue(upperBound).substring(0, namingSuffixExpression.length()));
        } catch (ParseException e) {
            throw new IllegalStateException(
                    "naming suffix expression is not a valid date format, please check the format as same as the partition key.");
        }
    }

    protected String unquoteValue(String value) {
        return StringUtils.unquoteMysqlValue(value);
    }


}
