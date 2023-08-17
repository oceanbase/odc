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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import lombok.NonNull;

/**
 * {@link OracleDateFormatSymbols}
 *
 * @author yh263208
 * @date 2022-10-31 15:05
 * @since ODC_release_4.1.0
 * @see java.text.DateFormatSymbols
 */
public class OracleDateFormatSymbols implements Serializable {

    public static int UNDEFINE_FIELD = -1;
    private static final List<DateFormatPattern> PATTERN_BUFFER;

    static {
        PATTERN_BUFFER = Arrays.stream(DateFormatPattern.values())
                .filter(p -> Objects.nonNull(p.patternString()))
                .sorted((o1, o2) -> Integer.compare(o2.patternString().length(), o1.patternString().length()))
                .collect(Collectors.toList());
    }

    /**
     * 日期格式化（非时间戳）时 ODC 支持的格式化选项
     *
     * @author yh263208
     * @date 2022-11-01 17:30
     * @since ODC_release_4.1.0
     * @references https://docs.oracle.com/cd/E11882_01/server.112/e41084/sql_elements004.htm#SQLRF00216
     */
    public enum DateFormatPattern implements FormatPattern {
        /**
         * 显示当前日期的公元信息，公元前/后
         */
        A_D_("A.D.", Calendar.ERA),
        AD("AD", Calendar.ERA),
        B_C_("B.C.", Calendar.ERA),
        BC("BC", Calendar.ERA),
        /**
         * 12 小时时制下标记上午/下午
         */
        A_M_("A.M.", Calendar.AM_PM),
        AM("AM", Calendar.AM_PM),
        P_M_("P.M.", Calendar.AM_PM),
        PM("PM", Calendar.AM_PM),
        D("D", Calendar.DAY_OF_WEEK),
        DD("DD", Calendar.DAY_OF_MONTH),
        DDD("DDD", Calendar.DAY_OF_YEAR),
        /**
         * 日期的全写名称，这里表示为星期，例子：
         *
         * <pre>
         *     eng: January
         *     chn: 星期一
         * </pre>
         */
        DAY("DAY", Calendar.DAY_OF_WEEK),
        /**
         * 日期的缩写名称，这里表示为星期，例子：
         *
         * <pre>
         *     eng: Jan
         *     chn: 星期一
         * </pre>
         */
        DY("DY", Calendar.DAY_OF_WEEK),
        HH("HH", Calendar.HOUR),
        HH12("HH12", Calendar.HOUR),
        HH24("HH24", Calendar.HOUR_OF_DAY),
        /**
         * {@code Julian day}，从 Jan 1, 4712 BC 到现在的天数 没有{@link Calendar} 对应，需要定制化处理
         */
        J("J", UNDEFINE_FIELD),
        MI("MI", Calendar.MINUTE),
        /**
         * 数字形式的月份
         */
        MM("MM", Calendar.MONTH),
        /**
         * 月份的缩写形式，例子：
         *
         * <pre>
         *     eng: Feb
         *     chn: 二月
         * </pre>
         */
        MON("MON", Calendar.MONTH),
        /**
         * 月份的全写形式，例子：
         *
         * <pre>
         *     eng: February
         *     chn: 二月
         * </pre>
         */
        MONTH("MONTH", Calendar.MONTH),
        /**
         * oracle 特有的表示年份的方式，详见：
         * https://docs.oracle.com/cd/E11882_01/server.112/e41084/sql_elements004.htm#i116004 需要定制化处理
         */
        RR("RR", UNDEFINE_FIELD),
        RRRR("RRRR", UNDEFINE_FIELD),
        SS("SS", Calendar.SECOND),
        /**
         * 当前时间距离午夜的秒数，无对应 field，需要自定义处理
         */
        SSSSS("SSSSS", UNDEFINE_FIELD),
        /**
         * 在 4 位年份中的首位后增加一个逗号，无现成的 field，需要自定义处理
         */
        Y_YYY("Y,YYY", UNDEFINE_FIELD),
        Y("Y", Calendar.YEAR),
        /**
         * 年份，前面的 S 代表如果是公元前就加一个负号
         */
        SYYYY("SYYYY", UNDEFINE_FIELD),
        /**
         * 时区，例如 CST
         */
        TZD("TZD", UNDEFINE_FIELD),
        /**
         * time zone, eg. Asia/Shanghai
         */
        TZR("TZR", UNDEFINE_FIELD),
        TZM("TZM", UNDEFINE_FIELD),
        TZH("TZH", UNDEFINE_FIELD),
        /**
         * 未知格式化串
         */
        UNDEFINE(null, UNDEFINE_FIELD);

        private final int field;
        private final String pattern;

        DateFormatPattern(String pattern, int field) {
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

    public DateFormatPattern matchPattern(@NonNull String pattern, int begin) {
        Validate.isTrue(begin >= 0, "Begin index can't be negative");
        Validate.isTrue(begin < pattern.length(), "Begin index can't be bigger than pattern's length");
        String str = pattern.substring(begin);
        Optional<DateFormatPattern> optional =
                PATTERN_BUFFER.stream().filter(p -> StringUtils.startsWithIgnoreCase(str, p.patternString())).findAny();
        return optional.orElse(DateFormatPattern.UNDEFINE);
    }

}
