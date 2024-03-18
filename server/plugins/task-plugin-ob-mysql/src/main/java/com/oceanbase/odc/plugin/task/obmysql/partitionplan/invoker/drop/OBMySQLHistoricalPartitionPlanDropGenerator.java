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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.drop;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.DropPartitionGenerator;
import com.oceanbase.odc.plugin.task.api.partitionplan.util.ParameterUtil;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionOption;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link OBMySQLHistoricalPartitionPlanDropGenerator} is a deprecated invoker, only for historic
 * partition plan data migration
 *
 * @author yh263208
 * @date 2024-03-11 16:05
 * @since ODC_release_4.2.4
 * @see DropPartitionGenerator
 */
@Slf4j
@Deprecated
public class OBMySQLHistoricalPartitionPlanDropGenerator implements DropPartitionGenerator {
    /**
     * candidates:
     * <li>1 -> YEAR 2 -> MONTH 5 -> DAY</li>
     */
    public static String PERIOD_UNIT_KEY = "periodUnit";
    public static String EXPIRE_PERIOD_KEY = "expirePeriod";

    @Override
    public String getName() {
        return "HISTORICAL_PARTITION_PLAN_DROP_GENERATOR";
    }

    @Override
    public List<DBTablePartitionDefinition> invoke(@NonNull Connection connection,
            @NonNull DBTable dbTable, @NonNull Map<String, Object> parameters) throws ParseException {
        String partitionType = getPartitionType(dbTable);
        if (partitionType == null) {
            log.warn("Current table is not supported, dbTable={}", dbTable);
            return Collections.emptyList();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long baseDate = System.currentTimeMillis();
        int periodUnit = ParameterUtil.nullSafeExtract(parameters, PERIOD_UNIT_KEY, Integer.class);
        int expirePeriod = ParameterUtil.nullSafeExtract(parameters, EXPIRE_PERIOD_KEY, Integer.class);
        List<DBTablePartitionDefinition> toDelete = new ArrayList<>();
        for (DBTablePartitionDefinition definition : dbTable.getPartition().getPartitionDefinitions()) {
            String maxValue = definition.getMaxValues().get(0);
            long partitionRightBound = "UNIX_TIMESTAMP".equals(partitionType)
                    ? Long.parseLong(maxValue) * 1000
                    : sdf.parse(maxValue.substring(1, maxValue.length() - 1)).getTime();
            if (!isExpirePartition(baseDate, partitionRightBound, expirePeriod, periodUnit)) {
                break;
            }
            toDelete.add(definition);
        }
        return toDelete;
    }

    private String getPartitionType(DBTable table) {
        DBTablePartitionOption option = table.getPartition().getPartitionOption();
        if (option.getType() == DBTablePartitionType.RANGE
                && Pattern.matches("UNIX_TIMESTAMP\\(.*\\)", option.getExpression())) {
            return "UNIX_TIMESTAMP";
        } else if (option.getType() == DBTablePartitionType.RANGE_COLUMNS
                && option.getColumnNames().size() == 1) {
            Optional<DBTableColumn> rangeColumn = table.getColumns().stream()
                    .filter(c -> c.getName().equals(option.getColumnNames().get(0))).findFirst();
            if (rangeColumn.isPresent()) {
                if ("DATE".equalsIgnoreCase(rangeColumn.get().getTypeName())
                        || "DATETIME".equalsIgnoreCase(rangeColumn.get().getTypeName())) {
                    return "DATE";
                }
            }
        }
        return null;
    }

    private boolean isExpirePartition(Long baseDate, Long partitionRightBound,
            int expirePeriod, Integer unit) {
        return getPartitionRightBound(partitionRightBound, expirePeriod, unit) < baseDate;
    }

    private Long getPartitionRightBound(Long baseDate, int interval, Integer unit) {
        Calendar maxRightBound = Calendar.getInstance();
        maxRightBound.setTime(new Date(baseDate));
        maxRightBound.set(Calendar.HOUR_OF_DAY, 0);
        maxRightBound.set(Calendar.MINUTE, 0);
        maxRightBound.set(Calendar.SECOND, 0);
        maxRightBound.set(Calendar.MILLISECOND, 0);
        switch (unit) {
            case Calendar.MONTH: {
                maxRightBound.set(Calendar.DAY_OF_MONTH, 1);
                maxRightBound.add(Calendar.MONTH, interval);
                break;
            }
            case Calendar.YEAR: {
                maxRightBound.set(Calendar.DAY_OF_YEAR, 1);
                maxRightBound.set(Calendar.MONTH, 1);
                maxRightBound.add(Calendar.YEAR, interval);
                break;
            }
            case Calendar.DAY_OF_MONTH: {
                maxRightBound.add(Calendar.DAY_OF_MONTH, interval);
            }
            default:
                break;
        }
        return maxRightBound.getTime().getTime();
    }

}
