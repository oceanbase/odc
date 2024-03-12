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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.mapper;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import com.oceanbase.odc.core.sql.execute.mapper.CellData;
import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;
import com.oceanbase.tools.dbbrowser.model.datatype.DataType;

import lombok.NonNull;

/**
 * {@link OBMySQLTimeDataTypeProcessor}
 *
 * @author yh263208
 * @date 2024-01-24 21:39
 * @since ODC_release_4.2.4
 * @see CellDataProcessor
 */
public class OBMySQLTimeDataTypeProcessor implements CellDataProcessor {

    @Override
    public Object mapCell(@NonNull CellData cellData) throws SQLException {
        return cellData.getDate();
    }

    @Override
    public String convertToSqlLiteral(Object target, @NonNull DataType dataType) {
        if (target == null) {
            return "NULL";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime((Date) target);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int precision = dataType.getPrecision();
        StringBuilder builder = new StringBuilder("'");
        if ((precision & TimeDataType.YEAR) == TimeDataType.YEAR) {
            builder.append(String.format("%04d", year));
        }
        if ((precision & TimeDataType.MONTH) == TimeDataType.MONTH) {
            builder.append("-").append(String.format("%02d", month));
        }
        if ((precision & TimeDataType.DAY) == TimeDataType.DAY) {
            builder.append("-").append(String.format("%02d", day));
        }
        if ((precision & TimeDataType.HOUR) == TimeDataType.HOUR) {
            builder.append(" ").append(String.format("%02d", hour));
        }
        if ((precision & TimeDataType.MINUTE) == TimeDataType.MINUTE) {
            builder.append(":").append(String.format("%02d", minute));
        }
        if ((precision & TimeDataType.SECOND) == TimeDataType.SECOND) {
            builder.append(":").append(String.format("%02d", second));
        }
        return builder.append("'").toString();
    }

    @Override
    public boolean supports(@NonNull DataType dataType) {
        return dataType instanceof TimeDataType;
    }

}
