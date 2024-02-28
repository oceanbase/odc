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
import java.util.TimeZone;

import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.jdbc.extend.datatype.TIMESTAMPLTZ;
import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.core.sql.util.OracleTimestampFormat;
import com.oceanbase.odc.core.sql.util.TimeZoneUtil;

import lombok.NonNull;

/**
 * {@link OBOracleNlsFormatTimestampLTZMapper}
 *
 * @author yh263208
 * @date 2023-07-04 21:01
 * @since ODC_release_4.2.0
 */
public class OBOracleNlsFormatTimestampLTZMapper extends OracleGeneralTimestampLTZMapper {

    private final OracleTimestampFormat format;

    public OBOracleNlsFormatTimestampLTZMapper(@NonNull String pattern, String serverTimeZoneStr) {
        super(serverTimeZoneStr);
        if (serverTimeZoneStr == null) {
            this.format = null;
        } else if (TimeZoneUtil.isValid(serverTimeZoneStr)) {
            this.format = new OracleTimestampFormat(pattern,
                    TimeZone.getTimeZone(serverTimeZoneStr), LocaleContextHolder.getLocale(), true);
        } else {
            this.format = null;
        }
    }

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        try {
            return map(data);
        } catch (Exception e) {
            Object obj = super.mapCell(data);
            return obj == null ? null : new TimeFormatResult(obj.toString());
        }
    }

    private Object map(CellData cellData) throws SQLException {
        Object obj = cellData.getObject();
        if (obj == null) {
            return null;
        }
        if (this.format == null) {
            Object cellObj = super.mapCell(cellData);
            return cellObj == null ? null : new TimeFormatResult(cellObj.toString());
        }
        byte[] bytes = ((TIMESTAMPLTZ) obj).toBytes();
        Long timestamp = getTimeMillis(bytes, serverTimeZoneStr);
        Integer nano = getNanos(bytes);

        Timestamp ts = new Timestamp(timestamp);
        ts.setNanos(nano);
        return new TimeFormatResult(format.format(ts), timestamp, nano, serverTimeZoneStr);
    }

}
