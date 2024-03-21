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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.partitionname.DateBasedPartitionNameGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.DateBasedPartitionNameGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.TimeDataTypeUtil;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator.SqlExprResult;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;

import lombok.NonNull;

/**
 * {@link OBMySQLDateBasedPartitionNameGenerator}
 *
 * @author yh263208
 * @date 2024-01-25 15:01
 * @since ODC_release_4.2.4
 */
public class OBMySQLDateBasedPartitionNameGenerator implements DateBasedPartitionNameGenerator {

    private static final String TARGET_FUNCTION_NAME = "UNIX_TIMESTAMP";

    @Override
    public String generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull Integer targetPartitionIndex, @NonNull DBTablePartitionDefinition target,
            @NonNull DateBasedPartitionNameGeneratorConfig config) {
        Date baseDate;
        if (config.getRefUpperBoundIndex() != null) {
            DBTablePartitionOption option = dbTable.getPartition().getPartitionOption();
            String partitionKey = CollectionUtils.isEmpty(option.getColumnNames())
                    ? option.getExpression()
                    : option.getColumnNames().get(config.getRefUpperBoundIndex());
            baseDate = getPartitionUpperBound(connection, partitionKey,
                    target.getMaxValues().get(config.getRefUpperBoundIndex()));
        } else {
            int precision = config.getIntervalPrecision();
            int interval = (targetPartitionIndex + 1) * config.getInterval();
            Date from;
            if (config.isFromCurrentTime()) {
                from = new Date();
            } else {
                from = new Date(config.getBaseTimestampMillis());
            }
            baseDate = TimeDataTypeUtil.getNextDate(from, interval, precision);
            baseDate = TimeDataTypeUtil.removeExcessPrecision(baseDate, precision);
        }
        DateFormat format = new SimpleDateFormat(config.getNamingSuffixExpression());
        return config.getNamingPrefix() + format.format(baseDate);
    }

    protected Date getPartitionUpperBound(@NonNull Connection connection,
            @NonNull String partitionKey, @NonNull String upperBound) {
        if (StringUtils.startsWith(partitionKey, TARGET_FUNCTION_NAME)) {
            return new Date(Long.parseLong(upperBound) * 1000);
        }
        SqlExprCalculator calculator = new OBMySQLExprCalculator(connection);
        SqlExprResult value = calculator.calculate("convert(" + upperBound + ", datetime)");
        if (!(value.getValue() instanceof Date)) {
            throw new IllegalStateException(upperBound + " isn't a date, " + value.getDataType().getDataTypeName());
        }
        return (Date) value.getValue();
    }

}
