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
package com.oceanbase.odc.core.sql.execute.mapper;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

import com.oceanbase.jdbc.extend.datatype.DataTypeUtilities;
import com.oceanbase.jdbc.extend.datatype.TIMESTAMPTZ;
import com.oceanbase.odc.core.sql.util.TimeZoneUtil;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link JdbcColumnMapper} for data type {@code timestamp[0-9] with time zone}
 *
 * @author yh263208
 * @date 2022-06028 16:25
 * @since ODC_release_3.4.0
 * @see JdbcColumnMapper
 */
public class OracleGeneralTimestampTZMapper implements JdbcColumnMapper {

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        Object obj = data.getObject();
        if (obj == null) {
            return null;
        }
        TIMESTAMPTZ timestamptz = (TIMESTAMPTZ) obj;
        byte[] bytes = timestamptz.toBytes();
        String timeZoneStr = DataTypeUtilities.toTimezoneStr(bytes[12], bytes[13], "", true);
        return toString(bytes, timeZoneStr, false);
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return "TIMESTAMP WITH TIME ZONE".equalsIgnoreCase(dataType.getDataTypeName());
    }

    protected Long getTimeMillis(byte[] bytes, String timeZoneStr) throws SQLException {
        return DataTypeUtilities.getOriginTime(bytes, TimeZone.getTimeZone(timeZoneStr));
    }

    protected Integer getNanos(byte[] bytes) {
        return DataTypeUtilities.getNanos(bytes, 7);
    }

    protected String getTimeZoneId(byte[] bytes) {
        return DataTypeUtilities.toTimezoneStr(bytes[12], bytes[13], "", true);
    }

    protected String toString(@NonNull byte[] bytes, @NonNull String timeZoneStr, @NonNull boolean isZoneID)
            throws SQLException {
        TimeZone timeZone =
                TimeZone.getTimeZone(isZoneID ? timeZoneStr : TimeZoneUtil.createCustomTimeZoneId(timeZoneStr));
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(DataTypeUtilities.getOriginTime(bytes, timeZone));
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int nanos = DataTypeUtilities.getNanos(bytes, 7);
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(ZonedDateTime.of(year, month, day, hour, minute, second, nanos, timeZone.toZoneId()));
    }

    private String toStr(int temp) {
        return temp < 10 ? "0" + temp : Integer.toString(temp);
    }

}
