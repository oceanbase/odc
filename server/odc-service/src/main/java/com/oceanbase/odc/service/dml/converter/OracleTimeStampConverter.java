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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.TimeZone;

import com.oceanbase.odc.core.sql.util.TimeZoneUtil;
import com.oceanbase.odc.service.dml.DataValue;

import lombok.NonNull;

/**
 * {@link OracleTimeStampConverter}
 *
 * @author yh263208
 * @date 2022-06-26 16:19
 * @since ODC_release_3.4.0
 * @see BaseDataConverter
 */
public class OracleTimeStampConverter extends BaseDataConverter {

    private final String serverTimeZoneId;

    public OracleTimeStampConverter(String serverTimeZoneId) {
        if (serverTimeZoneId == null) {
            this.serverTimeZoneId = TimeZone.getDefault().getID();
        } else if (TimeZoneUtil.isValid(serverTimeZoneId)) {
            this.serverTimeZoneId = serverTimeZoneId;
        } else {
            this.serverTimeZoneId = TimeZone.getDefault().getID();
        }
    }

    @Override
    protected Collection<String> getSupportDataTypeNames() {
        return Arrays.asList("timestamp", "timestamp with local time zone");
    }

    @Override
    protected String doConvert(@NonNull DataValue dataValue) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        /**
         * 注意：下面一行是定制化逻辑，前端在{@code timestamp with local time zone}类型上无法处理{@code Asia/Shanghai} 这样的时区。
         *
         * 所以就取巧了一下：对于这种特定类型，将所有的时间都转换成 0 时区传给后端，后端需要做一次时区转换保证正确性。
         */
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dataValue.getValue(), formatter);
        if ("timestamp".equalsIgnoreCase(dataValue.getDataType().getDataTypeName())) {
            zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
        } else if ("timestamp with local time zone".equalsIgnoreCase(dataValue.getDataType().getDataTypeName())) {
            zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of(this.serverTimeZoneId));
        }
        String timeStr = OracleDateConverter.format(zonedDateTime.toLocalDateTime(), true);
        LocalTime localTime = zonedDateTime.toLocalTime();
        if (localTime.getNano() == 0) {
            return "to_timestamp('" + timeStr + "', 'YYYY-MM-DD HH24:MI:SS')";
        }
        return "to_timestamp('" + timeStr + "', 'YYYY-MM-DD HH24:MI:SS.FF')";
    }

}
