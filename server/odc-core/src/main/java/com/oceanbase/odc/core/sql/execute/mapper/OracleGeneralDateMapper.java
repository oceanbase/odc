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
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OracleGeneralDateMapper}
 *
 * @author yh263208
 * @date 2022-06-24 19:33
 * @since ODC_release_3.4.0
 * @see JdbcColumnMapper
 */
public class OracleGeneralDateMapper implements JdbcColumnMapper {

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        Timestamp date = data.getTimestamp();
        if (date == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return formatter.format(
                ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault()));
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return "DATE".equalsIgnoreCase(dataType.getDataTypeName());
    }

}
