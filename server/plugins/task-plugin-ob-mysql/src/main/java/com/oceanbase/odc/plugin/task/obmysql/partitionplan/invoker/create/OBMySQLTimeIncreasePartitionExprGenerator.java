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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.TimeIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeIncreaseGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.DBTablePartitionUtil;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.TimeDataTypeUtil;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPoint;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.datatype.OBMySQLPartitionKeyDataTypeFactory;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.OBMySQLExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.SqlExprCalculator.SqlExprResult;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessor;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper.CellDataProcessors;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
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
        DataType dataType = getDataTypeFactory(connection, dbTable, partitionKey).generate();
        if (!(dataType instanceof TimeDataType)) {
            throw new BadRequestException(ErrorCodes.PartitionKeyDataTypeMismatch,
                    new Object[] {partitionKey, dataType.getDataTypeName(), TimeDataType.getLocalizedName()},
                    "Time increment can only be applied to time types, actual: " + dataType);
        } else if (dataType.getPrecision() < config.getIntervalPrecision()) {
            throw new BadRequestException(ErrorCodes.TimeDataTypePrecisionMismatch, new Object[] {partitionKey},
                    "Illegal precision, interval precision > data type's precision");
        }
        List<Date> candidates = getCandidateDates(connection, dbTable, partitionKey, config, generateCount);
        CellDataProcessor processor = getCellDataProcessor(dataType);
        return candidates.stream().map(i -> processor.convertToSqlLiteral(
                convertCandidate(i, dataType, config), dataType)).collect(Collectors.toList());
    }

    protected SqlExprCalculator getSqlExprCalculator(Connection connection) {
        return new OBMySQLExprCalculator(connection);
    }

    protected DataTypeFactory getDataTypeFactory(Connection connection,
            DBTable dbTable, String partitionKey) {
        return new OBMySQLPartitionKeyDataTypeFactory(getSqlExprCalculator(connection), dbTable, partitionKey);
    }

    protected CellDataProcessor getCellDataProcessor(@NonNull DataType dataType) {
        return CellDataProcessors.getByDataType(dataType);
    }

    protected Object convertCandidate(Date candidate, DataType dataType, TimeIncreaseGeneratorConfig config) {
        return candidate;
    }

    protected Date getPartitionUpperBound(@NonNull SqlExprCalculator calculator, @NonNull String partitionKey,
            @NonNull String upperBound) {
        SqlExprResult value = calculator.calculate("convert(" + upperBound + ", datetime)");
        if (!(value.getValue() instanceof Date)) {
            throw new IllegalStateException(upperBound + " isn't a date, " + value.getDataType().getDataTypeName());
        }
        return (Date) value.getValue();
    }

    private Date getBaseTime(String partitionKey, List<String> partitionUpperBounds,
            SqlExprCalculator calculator, Date baseTime) {
        List<Date> upperBounds = new ArrayList<>();
        for (int i = partitionUpperBounds.size() - 1; i >= 0; i--) {
            Date upperBound = getPartitionUpperBound(calculator, partitionKey, partitionUpperBounds.get(i));
            if (baseTime.compareTo(upperBound) > 0) {
                break;
            }
            upperBounds.add(upperBound);
        }
        if (CollectionUtils.isEmpty(upperBounds)) {
            return baseTime;
        }
        return upperBounds.get(upperBounds.size() - 1);
    }

    protected String unquoteIdentifier(String identifier) {
        return new OBMySQLAutoPartitionExtensionPoint().unquoteIdentifier(identifier);
    }

    protected List<Date> getCandidateDates(@NonNull Connection connection, @NonNull DBTable dbTable,
            @NonNull String partitionKey, TimeIncreaseGeneratorConfig config, Integer generateCount) {
        Date baseTime;
        if (config.isFromCurrentTime()) {
            baseTime = new Date();
        } else if (config.getBaseTimestampMillis() > 0) {
            baseTime = new Date(config.getBaseTimestampMillis());
        } else {
            throw new IllegalArgumentException("Base time is missing");
        }
        int index = DBTablePartitionUtil.getPartitionKeyIndex(dbTable, partitionKey, this::unquoteIdentifier);
        if (index >= 0) {
            List<DBTablePartitionDefinition> defs = dbTable.getPartition().getPartitionDefinitions();
            baseTime = getBaseTime(partitionKey, defs.stream().map(d -> d.getMaxValues().get(index))
                    .collect(Collectors.toList()), getSqlExprCalculator(connection), baseTime);
        }
        List<Date> candidates = new ArrayList<>(generateCount);
        for (int i = 0; i < generateCount; i++) {
            baseTime = TimeDataTypeUtil.getNextDate(baseTime, config.getInterval(), config.getIntervalPrecision());
            candidates.add(baseTime);
        }
        return TimeDataTypeUtil.removeExcessPrecision(candidates, config.getIntervalPrecision());
    }

}
