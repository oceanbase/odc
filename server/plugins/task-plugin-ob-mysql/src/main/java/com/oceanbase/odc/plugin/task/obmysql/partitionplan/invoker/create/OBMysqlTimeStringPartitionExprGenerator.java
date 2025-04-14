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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.create.TimeStringIncreasePartitionExprGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.FieldType;
import com.oceanbase.odc.plugin.task.api.partitionplan.model.TimeStringIncreaseGeneratorConfig;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.DBTablePartitionUtil;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.TimeDataTypeUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

public class OBMysqlTimeStringPartitionExprGenerator implements TimeStringIncreasePartitionExprGenerator {

    @Override
    public List<String> generate(@NonNull Connection connection,
            @NonNull DBTable dbTable, @NonNull String partitionKey, @NonNull Integer generateCount,
            @NonNull TimeStringIncreaseGeneratorConfig config) throws Exception {
        // get exist partition values
        int index = DBTablePartitionUtil.getPartitionKeyIndex(dbTable, partitionKey, this::unquoteIdentifier);
        List<DBTablePartitionDefinition> defs = dbTable.getPartition().getPartitionDefinitions();
        List<String> existPartitionValues = defs.stream().map(d -> d.getMaxValues().get(index))
                .collect(Collectors.toList());
        Date baseTime;
        if (config.isFromCurrentTime()) {
            baseTime = new Date();
        } else if (config.getBaseTimestampMillis() > 0) {
            baseTime = new Date(config.getBaseTimestampMillis());
        } else {
            throw new IllegalArgumentException("Base time is missing");
        }
        List<Date> candidates = getCandidateDates(existPartitionValues, baseTime, config, generateCount);
        return candidates.stream().map(i -> timeStringProcess(i, config)).collect(Collectors.toList());
    }

    private String timeStringProcess(Date date, TimeStringIncreaseGeneratorConfig config) {
        if (config.getFieldType() == FieldType.TIME_STRING) {
            SimpleDateFormat sdf = new SimpleDateFormat(config.getTimeFormat());
            return "'" + sdf.format(date) + "'";
        } else if (config.getFieldType() == FieldType.TIMESTAMP) {
            return String.valueOf(date.getTime());
        } else {
            throw new IllegalArgumentException("Unsupported field type");
        }
    }


    private List<Date> getCandidateDates(List<String> existPartitionValues, Date baseTime,
            TimeStringIncreaseGeneratorConfig config, Integer generateCount) {
        if (config.getFieldType() == null) {
            throw new IllegalArgumentException("Unsupported field type");
        }
        if (config.getFieldType() == FieldType.TIME_STRING) {
            SimpleDateFormat sdf = new SimpleDateFormat(config.getTimeFormat());
            for (int i = existPartitionValues.size() - 1; i >= 0; i--) {
                String existPartitionValue = existPartitionValues.get(i);
                try {
                    Date lastValue =
                            sdf.parse(unquoteValue(existPartitionValue).substring(0, config.getTimeFormat().length()));
                    if (baseTime.compareTo(lastValue) > 0) {
                        break;
                    }
                    baseTime = lastValue;
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid partition value: " + existPartitionValue
                            + ", time format: " + config.getTimeFormat()
                            + ", please keep the same format as the existing partition value");
                }
            }
        }
        if (config.getFieldType() == FieldType.TIMESTAMP) {
            for (int i = existPartitionValues.size() - 1; i >= 0; i--) {
                String existPartitionValue = existPartitionValues.get(i);
                try {
                    Date lastValue = new Date(Long.parseLong(unquoteValue(existPartitionValue)));
                    if (baseTime.compareTo(lastValue) > 0) {
                        break;
                    }
                    baseTime = lastValue;
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid partition value: " + existPartitionValue);
                }
            }
        }
        List<Date> candidates = new ArrayList<>(generateCount);
        for (int i = 0; i < generateCount; i++) {
            baseTime = TimeDataTypeUtil.getNextDate(baseTime, config.getInterval(), config.getIntervalPrecision());
            candidates.add(baseTime);
        }
        return TimeDataTypeUtil.removeExcessPrecision(candidates, config.getIntervalPrecision());
    }

    protected String unquoteIdentifier(String identifier) {
        return StringUtils.unquoteMySqlIdentifier(identifier);
    }

    protected String unquoteValue(String value) {
        return StringUtils.unquoteMysqlValue(value);
    }



}
