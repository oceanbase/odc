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
package com.oceanbase.odc.common.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class TimespanFormatUtil {

    public static String formatTimespan(Duration dValue) {
        return formatTimespan(dValue, "");
    }

    public static String formatTimespan(Duration dValue, String delimiter) {
        if (dValue == null) {
            return "0";
        }
        return formatTimespan(dValue.toNanos() / 1000, TimeUnit.MICROSECONDS, delimiter);
    }

    public static String formatTimespan(long time, TimeUnit timeUnit) {
        return formatTimespan(time, timeUnit, "");
    }

    /**
     * Formats the given time with a specified time unit according to predefined time units. The
     * formatting is done at a precision from microseconds up to days, depending on the length of the
     * time span provided.
     * 
     * <pre>
     * Examples:
     *  - (555555, TimeUnit.MICROSECONDS, "") ==> returns "555.555ms"
     *  - (60, TimeUnit.SECONDS, " ") ==> "1.000 min"
     *  - (24, TimeUnit.HOURS, "-") ==> "1.000-d"
     * </pre>
     *
     * @param time The time value to format.
     * @param timeUnit The unit of the given time.
     * @param delimiter The separator to use between the number and unit.
     * @return A formatted string representing the time span with the specified unit, or "0" if the time
     *         is zero.
     */
    public static String formatTimespan(long time, TimeUnit timeUnit, String delimiter) {
        if (time == 0) {
            return "0";
        }
        if (delimiter.contains(".")) {
            throw new IllegalArgumentException("delimiter should not contains '.'");
        }
        TimespanUnit result = TimespanUnit.MICROSECONDS;
        double converted = TimeUnit.MICROSECONDS.convert(time, timeUnit);
        for (TimespanUnit unit : TimespanUnit.values()) {
            result = unit;
            long amount = unit.amount;
            if (result == TimespanUnit.DAYS || converted < amount) {
                break;
            }
            converted /= amount;
        }
        return String.format("%.3f%s%s", converted, delimiter, result.text);
    }

    private enum TimespanUnit {
        MICROSECONDS("us", 1000),
        MILLISECONDS("ms", 1000),
        SECONDS("s", 60),
        MINUTES("min", 60),
        HOURS("h", 24),
        DAYS("d", 7);

        final String text;
        final long amount;

        TimespanUnit(String unit, long amount) {
            this.text = unit;
            this.amount = amount;
        }
    }

}
