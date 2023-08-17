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
package com.oceanbase.odc.core.sql.util;

import java.util.TimeZone;

import lombok.NonNull;

/**
 * {@link TimeZoneUtil}
 *
 * @author yh263208
 * @date 2023-07-05 16:50
 * @since ODC_release_4.2.0
 */
public class TimeZoneUtil {

    public static boolean isValid(String timeZoneId) {
        TimeZone tz = TimeZone.getTimeZone(timeZoneId);
        // Validate the timezone ID. JDK maps invalid timezones to GMT
        return !"GMT".equals(tz.getID()) || "GMT".equals(timeZoneId);
    }

    public static TimeZone getTimeZone(String id) {
        TimeZone tz = TimeZone.getTimeZone(id);
        // Validate the timezone ID. JDK maps invalid timezones to GMT
        if ("GMT".equals(tz.getID()) && !"GMT".equals(id)) {
            throw new IllegalStateException("Invalid timezone id '" + id + "'");
        }
        return tz;
    }

    public static String createCustomTimeZoneId(@NonNull String fromTimeZoneId) {
        boolean negative = false;
        StringBuilder tzh = new StringBuilder();
        boolean tzhEnd = false;
        StringBuilder tzm = new StringBuilder();
        char[] chars = fromTimeZoneId.toCharArray();
        for (char c : chars) {
            if (c == '-') {
                negative = true;
            } else if (c >= 48 && c <= 57) {
                if (tzhEnd) {
                    tzm.append(c);
                } else {
                    tzh.append(c);
                }
            } else if (c == ':') {
                tzhEnd = true;
            }
        }
        if (tzh.length() == 0 || tzh.length() > 2) {
            return null;
        } else if (tzm.length() == 0 || tzm.length() > 2) {
            return null;
        }
        StringBuilder builder = new StringBuilder("GMT");
        if (negative) {
            builder.append("-");
        } else {
            builder.append("+");
        }
        if (tzh.length() == 1) {
            tzh.insert(0, '0');
        }
        if (tzm.length() == 1) {
            tzm.insert(0, '0');
        }
        if (Integer.parseInt(tzh.toString()) > 24) {
            return null;
        }
        if (Integer.parseInt(tzm.toString()) > 60) {
            return null;
        }
        return builder.append(tzh).append(":").append(tzm).toString();
    }

}
