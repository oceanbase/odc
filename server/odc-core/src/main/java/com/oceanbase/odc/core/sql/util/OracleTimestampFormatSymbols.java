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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import lombok.NonNull;

/**
 * {@link OracleTimestampFormatSymbols}
 *
 * @author yh263208
 * @date 2022-11-06 02:06
 * @since ODC_release_4.1.0
 */
public class OracleTimestampFormatSymbols {

    public enum TimestampFormatPattern implements FormatPattern {
        /**
         * {@link java.sql.Timestamp} 的 nano 秒数
         */
        FF("FF", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_1("FF1", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_2("FF2", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_3("FF3", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_4("FF4", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_5("FF5", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_6("FF6", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_7("FF7", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_8("FF8", OracleDateFormatSymbols.UNDEFINE_FIELD),
        FF_9("FF9", OracleDateFormatSymbols.UNDEFINE_FIELD),
        /**
         * 未知格式化串
         */
        UNDEFINE(null, OracleDateFormatSymbols.UNDEFINE_FIELD);

        private final int field;
        private final String pattern;

        TimestampFormatPattern(String pattern, int field) {
            this.field = field;
            this.pattern = pattern;
        }

        @Override
        public int field() {
            return this.field;
        }

        @Override
        public String patternString() {
            return this.pattern;
        }
    }

    public TimestampFormatPattern matchPattern(@NonNull String pattern, int begin) {
        Validate.isTrue(begin >= 0, "Begin index can't be negative");
        Validate.isTrue(begin < pattern.length(), "Begin index can't be bigger than pattern's length");
        String str = pattern.substring(begin);
        TimestampFormatPattern r = null;
        for (TimestampFormatPattern p : TimestampFormatPattern.values()) {
            if (p == TimestampFormatPattern.UNDEFINE) {
                continue;
            }
            if (StringUtils.startsWithIgnoreCase(str, p.pattern)) {
                if (r == null) {
                    r = p;
                    continue;
                }
                if (r.pattern.length() < p.pattern.length()) {
                    r = p;
                }
            }
        }
        if (r != null) {
            return r;
        }
        return TimestampFormatPattern.UNDEFINE;
    }

}
