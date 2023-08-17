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

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.service.dml.DataValue;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link OracleTimeStampTZConverter}
 *
 * @author yh263208
 * @date 2022-06-24 20:31
 * @since ODC_release_3.4.0
 */
@Slf4j
public class OracleTimeStampTZConverter extends BaseDataConverter {

    private final static Pattern TIMEZONE_PATTERN = Pattern.compile("(\\+|\\-)(\\d+):(\\d+)");

    @Override
    protected Collection<String> getSupportDataTypeNames() {
        return Collections.singletonList("timestamp with time zone");
    }

    @Override
    protected String doConvert(@NonNull DataValue dataValue) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dataValue.getValue(), formatter);
        String timeStr = OracleDateConverter.format(zonedDateTime.toLocalDateTime(), true);
        String timeZoneId = zonedDateTime.getOffset().getId();
        if (StringUtils.equalsAnyIgnoreCase(timeZoneId, "z")) {
            timeZoneId = "+00:00";
        }
        timeStr = timeStr + " " + getServerTimeZoneId(timeZoneId);
        LocalTime localTime = zonedDateTime.toLocalTime();
        if (localTime.getNano() == 0) {
            return "to_timestamp_tz('" + timeStr + "', 'YYYY-MM-DD HH24:MI:SS TZH:TZM')";
        }
        return "to_timestamp_tz('" + timeStr + "', 'YYYY-MM-DD HH24:MI:SS.FF TZH:TZM')";
    }

    protected String getServerTimeZoneId(String timeZoneId) {
        if (timeZoneId == null) {
            return null;
        }
        Matcher zoneMatcher = TIMEZONE_PATTERN.matcher(timeZoneId);
        if (zoneMatcher.find()) {
            String hrs = zoneMatcher.group(2);
            String mins = zoneMatcher.group(3);
            if (hrs.length() == 1) {
                hrs = '0' + hrs;
            }
            if (mins.length() == 1) {
                mins = '0' + mins;
            }
            return String.format("%s%s:%s", zoneMatcher.group(1), hrs, mins);
        }
        return timeZoneId;
    }

}
