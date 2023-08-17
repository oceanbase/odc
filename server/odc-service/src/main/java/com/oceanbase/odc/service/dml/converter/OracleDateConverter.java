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
package com.oceanbase.odc.service.dml.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;

import com.oceanbase.odc.service.dml.DataValue;

import lombok.NonNull;

/**
 * {@link OracleDateConverter}
 *
 * @author yh263208
 * @date 2022-06-26 16:34
 * @since ODC_release_3.4.0
 * @see BaseDataConverter
 */
public class OracleDateConverter extends BaseDataConverter {

    @Override
    protected Collection<String> getSupportDataTypeNames() {
        return Collections.singleton("date");
    }

    @Override
    protected String doConvert(@NonNull DataValue dataValue) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dataValue.getValue(), formatter)
                .withZoneSameInstant(ZoneId.systemDefault());
        String timeStr = format(zonedDateTime.toLocalDateTime(), false);
        return "to_date('" + timeStr + "', 'YYYY-MM-DD HH24:MI:SS')";
    }

    public static String format(@NonNull LocalDateTime localDateTime, boolean appendNano) {
        LocalDate localDate = localDateTime.toLocalDate();
        int yearValue = localDate.getYear();
        int monthValue = localDate.getMonthValue();
        int dayValue = localDate.getDayOfMonth();
        int absYear = Math.abs(yearValue);
        StringBuilder buf = new StringBuilder(10);
        if (absYear < 1000) {
            if (yearValue < 0) {
                buf.append(yearValue - 10000).deleteCharAt(1);
            } else {
                buf.append(yearValue + 10000).deleteCharAt(0);
            }
        } else {
            if (yearValue > 9999) {
                buf.append('+');
            }
            buf.append(yearValue);
        }
        buf.append(monthValue < 10 ? "-0" : "-")
                .append(monthValue)
                .append(dayValue < 10 ? "-0" : "-")
                .append(dayValue).append(" ");
        LocalTime localTime = localDateTime.toLocalTime();

        int hourValue = localTime.getHour();
        int minuteValue = localTime.getMinute();
        int secondValue = localTime.getSecond();
        int nanoValue = localTime.getNano();
        buf.append(hourValue < 10 ? "0" : "").append(hourValue)
                .append(minuteValue < 10 ? ":0" : ":").append(minuteValue);
        buf.append(secondValue < 10 ? ":0" : ":").append(secondValue);
        if (appendNano && nanoValue > 0) {
            buf.append('.');
            if (nanoValue % 1000_000 == 0) {
                buf.append(Integer.toString((nanoValue / 1000_000) + 1000).substring(1));
            } else if (nanoValue % 1000 == 0) {
                buf.append(Integer.toString((nanoValue / 1000) + 1000_000).substring(1));
            } else {
                buf.append(Integer.toString((nanoValue) + 1000_000_000).substring(1));
            }
        }
        return buf.toString();
    }

}
