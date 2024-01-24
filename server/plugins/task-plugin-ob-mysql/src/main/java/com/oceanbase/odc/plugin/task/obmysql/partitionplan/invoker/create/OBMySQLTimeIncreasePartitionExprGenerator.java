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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.TimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype.OBMySQLPartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessor;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessors;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeFactory;

import lombok.NonNull;

/**
 * {@link OBMySQLTimeIncreasePartitionExprGenerator}
 *
 * @author yh263208
 * @date 2024-01-24 16:37
 * @since ODC_release_4.2.4
 * @see TimeIncreasePartitionExprGenerator
 */
public class OBMySQLTimeIncreasePartitionExprGenerator implements TimeIncreasePartitionExprGenerator {

    @Override
    public List<String> generate(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull String partitionKey, @NonNull Integer generateCount,
            @NonNull TimeIncreaseGeneratorConfig config) throws IOException, SQLException {
        DataType dataType = getPartitionKeyDataType(connection, dbTable, partitionKey);
        if (!(dataType instanceof TimeDataType)) {
            throw new IllegalArgumentException("Time increment can only be applied to time types, actual: " + dataType);
        } else if (dataType.getPrecision() < config.getIntervalPrecision()) {
            throw new IllegalArgumentException("Illegal precision, interval precision > data type's precision");
        }
        Date baseTime;
        if (config.isFromCurrentTime()) {
            baseTime = new Date();
        } else if (config.getFromTimestampMillis() > 0) {
            baseTime = new Date(config.getFromTimestampMillis());
        } else {
            throw new IllegalArgumentException("Base time is missing");
        }
        List<Date> candidates = new ArrayList<>(generateCount);
        candidates.add(baseTime);
        for (int i = 1; i < generateCount; i++) {
            baseTime = getNextDate(baseTime, config.getInterval(), config.getIntervalPrecision());
            candidates.add(baseTime);
        }
        candidates = removeExcessPrecision(candidates, config.getIntervalPrecision());
        CellDataProcessor processor = getByDataType(dataType);
        return candidates.stream().map(i -> processor.convertToSqlLiteral(i, dataType)).collect(Collectors.toList());
    }

    protected DataType getPartitionKeyDataType(@NonNull Connection connection,
            @NonNull DBTable dbTable, @NonNull String partitionKey) throws IOException, SQLException {
        DataTypeFactory factory = new OBMySQLPartitionKeyDataTypeFactory(
                new OBMySQLExprCalculator(connection), dbTable, partitionKey);
        return factory.generate();
    }

    protected CellDataProcessor getByDataType(@NonNull DataType dataType) {
        return CellDataProcessors.getByDataType(dataType);
    }

    private List<Date> removeExcessPrecision(List<Date> candidates, int intervalUnit) {
        return candidates.stream().map(candidate -> {
            if ((intervalUnit & TimeDataType.SECOND) == TimeDataType.SECOND) {
                return candidate;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(candidate);
            if ((intervalUnit & TimeDataType.MINUTE) == TimeDataType.MINUTE) {
                calendar.set(Calendar.SECOND, 0);
            } else if ((intervalUnit & TimeDataType.HOUR) == TimeDataType.HOUR) {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
            } else if ((intervalUnit & TimeDataType.DAY) == TimeDataType.DAY) {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
            } else if ((intervalUnit & TimeDataType.MONTH) == TimeDataType.MONTH) {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            } else if ((intervalUnit & TimeDataType.YEAR) == TimeDataType.YEAR) {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.MONTH, 0);
            }
            return calendar.getTime();
        }).collect(Collectors.toList());
    }

    private Date getNextDate(Date base, int interval, int intervalUnit) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);
        if ((intervalUnit & TimeDataType.SECOND) == TimeDataType.SECOND) {
            calendar.add(Calendar.SECOND, interval);
        } else if ((intervalUnit & TimeDataType.MINUTE) == TimeDataType.MINUTE) {
            calendar.add(Calendar.MINUTE, interval);
        } else if ((intervalUnit & TimeDataType.HOUR) == TimeDataType.HOUR) {
            calendar.add(Calendar.HOUR_OF_DAY, interval);
        } else if ((intervalUnit & TimeDataType.DAY) == TimeDataType.DAY) {
            calendar.add(Calendar.DAY_OF_YEAR, interval);
        } else if ((intervalUnit & TimeDataType.MONTH) == TimeDataType.MONTH) {
            calendar.add(Calendar.MONTH, interval);
        } else if ((intervalUnit & TimeDataType.YEAR) == TimeDataType.YEAR) {
            calendar.add(Calendar.YEAR, interval);
        } else {
            throw new IllegalArgumentException("Interval unit is illegal, " + intervalUnit);
        }
        return calendar.getTime();
    }

}
