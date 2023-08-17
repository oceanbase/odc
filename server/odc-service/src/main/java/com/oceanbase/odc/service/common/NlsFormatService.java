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
package com.oceanbase.odc.service.common;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.Validate;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.sql.util.OracleDateFormat;
import com.oceanbase.odc.core.sql.util.OracleTimestampFormat;
import com.oceanbase.odc.core.sql.util.TimeZoneUtil;
import com.oceanbase.odc.service.common.model.NlsFormatReq;

import lombok.NonNull;

/**
 * {@link NlsFormatService}
 *
 * @author yh263208
 * @date 2023-07-04 16:28
 * @since ODC_release_4.2.0
 */
@Service
@Validated
@SkipAuthorize
public class NlsFormatService {

    public String format(@NotNull ConnectionSession session, @Valid @NotNull NlsFormatReq req) {
        switch (req.getDataType().toUpperCase()) {
            case "DATE":
                return formatDate(session, req.getTimestamp(), null);
            case "TIMESTAMP":
                return formatTimestamp(session, req.getTimestamp(), req.getNano(), null);
            case "TIMESTAMP WITH TIME ZONE":
                return formatTimestampTZ(session, req.getTimestamp(), req.getNano(), req.getTimeZoneId());
            case "TIMESTAMP WITH LOCAL TIME ZONE":
                return formatTimestampLTZ(session, req.getTimestamp(), req.getNano());
            default:
                throw new UnsupportedOperationException("Unsupported data type, " + req.getDataType());
        }
    }

    public String formatDate(@NotNull ConnectionSession session, @NotNull Long timestamp, String timeZoneId) {
        String pattern = ConnectionSessionUtil.getNlsDateFormat(session);
        if (pattern == null) {
            throw new IllegalStateException("nls_date_format is null");
        }
        return formatDate(pattern, timestamp, timeZoneId);
    }

    public String formatDate(@NotNull String pattern, @NotNull Long timestamp, String timeZoneId) {
        Locale locale = LocaleContextHolder.getLocale();
        TimeZone timeZone = TimeZone.getDefault();
        if (timeZoneId != null) {
            timeZone = generateTimeZone(timeZoneId);
        }
        OracleDateFormat dateFormat = new OracleDateFormat(pattern, timeZone, locale, true);
        return dateFormat.format(new Date(timestamp));
    }

    public String formatTimestamp(@NotNull ConnectionSession session,
            @NotNull Long timestamp, @NotNull Integer nano, String timeZoneId) {
        String pattern = ConnectionSessionUtil.getNlsTimestampFormat(session);
        if (pattern == null) {
            throw new IllegalStateException("nls_timestamp_format is null");
        }
        return formatTimestamp(pattern, timestamp, nano, timeZoneId);
    }

    public String formatTimestampTZ(@NotNull ConnectionSession session,
            @NotNull Long timestamp, @NotNull Integer nano, @NotNull String timeZoneId) {
        String pattern = ConnectionSessionUtil.getNlsTimestampTZFormat(session);
        if (pattern == null) {
            throw new IllegalStateException("nls_timestamp_tz_format is null");
        }
        return formatTimestamp(pattern, timestamp, nano, timeZoneId);
    }

    public String formatTimestampLTZ(@NotNull ConnectionSession session,
            @NotNull Long timestamp, @NotNull Integer nano) {
        String pattern = ConnectionSessionUtil.getNlsTimestampTZFormat(session);
        if (pattern == null) {
            throw new IllegalStateException("nls_timestamp_tz_format is null");
        }
        String timeZoneId = ConnectionSessionUtil.getConsoleSessionTimeZone(session);
        if (timeZoneId == null) {
            throw new IllegalStateException("Server time zone is null");
        }
        return formatTimestamp(pattern, timestamp, nano, timeZoneId);
    }

    public String formatTimestamp(@NotNull String pattern, @NotNull Long timestamp,
            @NotNull Integer nano, String timeZoneId) {
        Validate.isTrue(nano >= 0, "Nano can not be negative");
        Locale locale = LocaleContextHolder.getLocale();
        TimeZone timeZone = TimeZone.getDefault();
        if (timeZoneId != null) {
            timeZone = generateTimeZone(timeZoneId);
        }
        OracleTimestampFormat dateFormat = new OracleTimestampFormat(pattern, timeZone, locale, true);
        Timestamp ts = new Timestamp(timestamp);
        ts.setNanos(nano);
        return dateFormat.format(ts);
    }

    private TimeZone generateTimeZone(@NonNull String timeZoneId) {
        if (TimeZoneUtil.isValid(timeZoneId)) {
            return TimeZoneUtil.getTimeZone(timeZoneId);
        }
        if (TimeZoneUtil.isValid("GMT" + timeZoneId)) {
            return TimeZoneUtil.getTimeZone("GMT" + timeZoneId);
        }
        String newTimeZoneId = TimeZoneUtil.createCustomTimeZoneId(timeZoneId);
        if (newTimeZoneId == null) {
            throw new IllegalArgumentException("Illegal time zone id, " + timeZoneId);
        }
        return TimeZoneUtil.getTimeZone(newTimeZoneId);
    }

}
