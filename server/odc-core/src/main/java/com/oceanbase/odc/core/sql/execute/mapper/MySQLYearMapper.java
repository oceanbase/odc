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

import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;

import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link JdbcColumnMapper} for data type {@code year}
 *
 * @author yh263208
 * @date 2022-06-28 20:59
 * @since ODC_release_3.4.0
 * @see JdbcColumnMapper
 */
public class MySQLYearMapper implements JdbcColumnMapper {

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        Date date = data.getDate();
        if (date == null) {
            return date;
        }
        if (!isValidYear(data)) {
            return "0000";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        String yStr;
        String yZerosPadding = "0000";
        if (year < 1000) {
            yStr = "" + year;
            yStr = yZerosPadding.substring(0, (4 - yStr.length())) + yStr;
        } else {
            yStr = "" + year;
        }
        return yStr;
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return "YEAR".equalsIgnoreCase(dataType.getDataTypeName());
    }

    private boolean isValidYear(CellData data) throws SQLException {
        byte[] buffer = data.getBytes();
        if (buffer == null) {
            throw new NullPointerException("Year is null");
        }
        String stringValue = new String(buffer);
        return stringValue.length() <= 0
                || stringValue.charAt(0) != '0'
                || !"0000-00-00".equals(stringValue)
                        && !"0000-00-00 00:00:00".equals(stringValue)
                        && !"00000000000000".equals(stringValue)
                        && !"0".equals(stringValue)
                        && !"00000000".equals(stringValue)
                        && !"0000".equals(stringValue);
    }

}

