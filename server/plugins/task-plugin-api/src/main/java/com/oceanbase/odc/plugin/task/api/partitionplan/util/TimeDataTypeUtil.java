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
package com.oceanbase.odc.plugin.task.api.partitionplan.util;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;

/**
 * {@link TimeDataTypeUtil}
 *
 * @author yh263208
 * @date 2024-01-26 10:16
 * @since ODC_release_4.2.4
 */
public class TimeDataTypeUtil {

    public static Date getNextDate(Date base, int interval, int intervalPrecision) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);
        if ((intervalPrecision & TimeDataType.SECOND) == TimeDataType.SECOND) {
            calendar.add(Calendar.SECOND, interval);
        } else if ((intervalPrecision & TimeDataType.MINUTE) == TimeDataType.MINUTE) {
            calendar.add(Calendar.MINUTE, interval);
        } else if ((intervalPrecision & TimeDataType.HOUR) == TimeDataType.HOUR) {
            calendar.add(Calendar.HOUR_OF_DAY, interval);
        } else if ((intervalPrecision & TimeDataType.DAY) == TimeDataType.DAY) {
            calendar.add(Calendar.DAY_OF_YEAR, interval);
        } else if ((intervalPrecision & TimeDataType.MONTH) == TimeDataType.MONTH) {
            calendar.add(Calendar.MONTH, interval);
        } else if ((intervalPrecision & TimeDataType.YEAR) == TimeDataType.YEAR) {
            calendar.add(Calendar.YEAR, interval);
        } else {
            throw new IllegalArgumentException("Interval precision is illegal, " + intervalPrecision);
        }
        return calendar.getTime();
    }

    public static List<Date> removeExcessPrecision(List<Date> candidates, int realPrecision) {
        return candidates.stream().map(candidate -> {
            if ((realPrecision & TimeDataType.SECOND) == TimeDataType.SECOND) {
                return candidate;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(candidate);
            if ((realPrecision & TimeDataType.MINUTE) == TimeDataType.MINUTE) {
                calendar.set(Calendar.SECOND, 0);
            } else if ((realPrecision & TimeDataType.HOUR) == TimeDataType.HOUR) {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
            } else if ((realPrecision & TimeDataType.DAY) == TimeDataType.DAY) {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
            } else if ((realPrecision & TimeDataType.MONTH) == TimeDataType.MONTH) {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
            } else if ((realPrecision & TimeDataType.YEAR) == TimeDataType.YEAR) {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.MONTH, 0);
            }
            return calendar.getTime();
        }).collect(Collectors.toList());
    }

    public static Date removeExcessPrecision(Date candidate, int realPrecision) {
        return removeExcessPrecision(Collections.singletonList(candidate), realPrecision).get(0);
    }

}
