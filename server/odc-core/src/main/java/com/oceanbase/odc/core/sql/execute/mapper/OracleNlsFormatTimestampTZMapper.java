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

import org.apache.commons.lang3.Validate;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.core.sql.execute.model.TimeFormatResult;
import com.oceanbase.odc.core.sql.util.OracleTimestampFormat;

import lombok.NonNull;
import oracle.sql.TIMESTAMPTZ;

/**
 * {@link OracleNlsFormatTimestampTZMapper}
 *
 * @author jingtian
 * @date 2024/2/20
 * @since ODC_release_4.2.4
 */
public class OracleNlsFormatTimestampTZMapper extends OracleGeneralTimestampTZMapper {

    protected final OracleTimestampFormat format;

    public OracleNlsFormatTimestampTZMapper(@NonNull String pattern) {
        this.format = new OracleTimestampFormat(pattern, TimeZone.getDefault(), LocaleContextHolder.getLocale(), true);
    }

    @Override
    public Object mapCell(@NonNull CellData data) throws SQLException {
        TIMESTAMPTZ timestamptz = (TIMESTAMPTZ) data.getObject();
        Timestamp timestamp = data.getTimestamp();
        if (timestamptz == null || timestamp == null) {
            return null;
        }
        TimeZone timeZone = timestamptz.getTimeZone();
        Validate.notNull(timeZone, "timeZone can not be null");
        this.format.setTimeZone(timeZone);
        return new TimeFormatResult(this.format.format(timestamp), timestamp.getTime(), timestamp.getNanos(),
                timeZone.getID());
    }
}
