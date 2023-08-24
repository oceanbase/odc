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
package com.oceanbase.odc.service.partitionplan;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import com.oceanbase.odc.service.partitionplan.model.PeriodUnit;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

/**
 * @Author：tianke
 * @Date: 2022/9/22 11:41
 * @Descripition:
 */
public class PartitionPlanFunction {

    /**
     * 计算失效时间，小于当前时间则过期
     */
    public static boolean isExpirePartition(Long baseDate, Long partitionRightBound, int expirePeriod,
            PeriodUnit unit) {
        return getPartitionRightBound(partitionRightBound, expirePeriod, unit) < baseDate;
    }

    public static Long getPartitionRightBound(Long baseDate, int interval, PeriodUnit unit) {

        Calendar maxRightBound = Calendar.getInstance();
        maxRightBound.setTime(new Date(baseDate));
        maxRightBound.set(Calendar.HOUR_OF_DAY, 0);
        maxRightBound.set(Calendar.MINUTE, 0);
        maxRightBound.set(Calendar.SECOND, 0);
        maxRightBound.set(Calendar.MILLISECOND, 0);
        switch (unit.getId()) {
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

    public static PartitionExpressionType getPartitionExpressionType(DBTable table) {
        String expression = table.getPartition().getPartitionOption().getExpression();
        PartitionExpressionType type = PartitionExpressionType.OTHER;
        if (table.getPartition().getPartitionOption().getType() == DBTablePartitionType.RANGE) {
            if (Pattern.matches("UNIX_TIMESTAMP\\(.*\\)", expression)) {
                type = PartitionExpressionType.UNIX_TIMESTAMP;
            }
        }
        if (table.getPartition().getPartitionOption().getType() == DBTablePartitionType.RANGE_COLUMNS
                && table.getPartition().getPartitionOption().getColumnNames().size() == 1) {
            Optional<DBTableColumn> rangeColumn = table.getColumns().stream().filter(
                    column -> column.getName()
                            .equals(table.getPartition().getPartitionOption().getColumnNames().get(0)))
                    .findFirst();
            if (rangeColumn.isPresent()) {
                if (rangeColumn.get().getTypeName().equalsIgnoreCase("DATE")
                        || rangeColumn.get().getTypeName().equalsIgnoreCase("DATETIME")) {
                    type = PartitionExpressionType.DATE;
                }
            }
        }
        return type;

    }

}
