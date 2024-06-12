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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.oceanbase.odc.core.sql.util.OracleDateFormatSymbols.DateFormatPattern;

import lombok.NonNull;

/**
 * {@link OracleDateFormat}
 *
 * @author yh263208
 * @date 2022-11-01 20:22
 * @since ODC_release_4.1.0
 */
public class OracleDateFormat extends DateFormat {

    private static final int TAG_RR_FIELD = 100;
    private static final int TAG_J_FIELD = 101;
    private static final int TAG_SSSSS_FIELD = 102;
    private static final int TAG_Y_YYY_FIELD = 103;
    private static final int TAG_SYYYY_FIELD = 104;
    private static final int TAG_RRRR_FIELD = 105;
    private static final int TAG_DOUBLE_QUOTE_CHAR = 106;
    private static final int TAG_DELIMITER_CHAR = 107;
    private static final int TAG_TZD_FIELD = 108;
    private static final int TAG_TZR_FIELD = 109;
    private static final int TAG_TZM_FIELD = 110;
    private static final int TAG_TZH_FIELD = 111;
    private final static int[] SIZE_TABLE = {9, 99, 999, 9999, 99999,
            999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE};
    protected static final int LONG_STYLE_MAGIC_DIGIT = 8;
    protected static final int SHORT_STYLE_MAGIC_DIGIT = 7;
    protected static final int NON_STYLE_MAGIC_DIGIT = 9;

    private final Locale locale;
    private final OracleDateFormatSymbols formData;
    private final Calendar calendar;
    /**
     * The compiled pattern.
     */
    private final char[] compiledPattern;
    private final boolean ignoreUnknownFormat;
    protected char zeroDigit;

    public OracleDateFormat(String pattern) {
        this(pattern, TimeZone.getDefault(), Locale.getDefault(Locale.Category.FORMAT), true);
    }

    public OracleDateFormat(String pattern, Locale locale) {
        this(pattern, TimeZone.getDefault(), locale, true);
    }

    public OracleDateFormat(@NonNull String pattern, @NonNull Locale locale, boolean ignoreUnknownFormat) {
        this(pattern, TimeZone.getDefault(), locale, ignoreUnknownFormat);
    }

    public OracleDateFormat(@NonNull String pattern, @NonNull TimeZone timeZone,
            @NonNull Locale locale, boolean ignoreUnknownFormat) {
        this.ignoreUnknownFormat = ignoreUnknownFormat;
        this.locale = locale;
        this.formData = new OracleDateFormatSymbols();
        this.calendar = Calendar.getInstance(timeZone, locale);
        this.compiledPattern = compile(pattern);
        this.numberFormat = NumberFormat.getIntegerInstance(locale);
        this.numberFormat.setGroupingUsed(false);
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        fieldPosition.setBeginIndex(0);
        fieldPosition.setEndIndex(0);
        return format(date, toAppendTo);
    }

    @Override
    public void setTimeZone(TimeZone zone) {
        this.calendar.setTimeZone(zone);
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    private StringBuffer format(Date date, StringBuffer toAppendTo) {
        calendar.setTime(date);
        for (int i = 0; i < compiledPattern.length;) {
            int tag = compiledPattern[i] >>> 8;
            int count = compiledPattern[i++] & 0xff;
            if (count == 255) {
                count = compiledPattern[i++] << 16;
                count |= compiledPattern[i++];
            }
            switch (tag) {
                case TAG_DELIMITER_CHAR:
                    /**
                     * 如果是分隔符，那么分隔符的值就存储在 count 中
                     */
                    toAppendTo.append((char) count);
                    break;
                case TAG_DOUBLE_QUOTE_CHAR:
                    /**
                     * 如果是双引号中的字符原样输出，那么 count 存储的是原样输出 的字符数量，后面 count 个字节存储要输出的字符
                     */
                    toAppendTo.append(compiledPattern, i, count);
                    i += count;
                    break;
                case TAG_J_FIELD:
                    formatJPattern(toAppendTo);
                    break;
                case TAG_RR_FIELD:
                    formatRRPattern(toAppendTo);
                    break;
                case TAG_SSSSS_FIELD:
                    formatSSSSSPattern(toAppendTo);
                    break;
                case TAG_SYYYY_FIELD:
                    formatSYYYYPattern(toAppendTo);
                    break;
                case TAG_Y_YYY_FIELD:
                    formatY_YYYPattern(toAppendTo);
                    break;
                case TAG_RRRR_FIELD:
                    formatRRRRPattern(toAppendTo);
                    break;
                case TAG_TZH_FIELD:
                case TAG_TZM_FIELD:
                case TAG_TZR_FIELD:
                case TAG_TZD_FIELD:
                    formatTimeZonePattern(tag, toAppendTo);
                    break;
                default:
                    doFormat(tag, getCount(count), getStyle(count), toAppendTo);
                    break;
            }
        }
        return toAppendTo;
    }

    private int getStyle(int count) {
        if (count % LONG_STYLE_MAGIC_DIGIT == 0) {
            return Calendar.LONG;
        } else if (count % SHORT_STYLE_MAGIC_DIGIT == 0) {
            return Calendar.SHORT;
        } else if (count % NON_STYLE_MAGIC_DIGIT == 0) {
            return -1;
        } else {
            throw new IllegalStateException("Illegal length " + count);
        }
    }

    private int getCount(int count) {
        if (count % LONG_STYLE_MAGIC_DIGIT == 0) {
            return count / LONG_STYLE_MAGIC_DIGIT;
        } else if (count % SHORT_STYLE_MAGIC_DIGIT == 0) {
            return count / SHORT_STYLE_MAGIC_DIGIT;
        } else if (count % NON_STYLE_MAGIC_DIGIT == 0) {
            return count / NON_STYLE_MAGIC_DIGIT;
        } else {
            throw new IllegalStateException("Illegal length " + count);
        }
    }

    /**
     * {@link DateFormatPattern#J}, 儒略历，佛教历法。
     *
     * 返回的是当前时间到{@code Jan 1, 4712 B.C.} 之间的天数，计算的时候由于是格里历 因此使用一个快速计算公式算出
     */
    private void formatJPattern(StringBuffer buffer) {
        int era = calendar.get(Calendar.ERA);
        int year = calendar.get(Calendar.YEAR);
        if (era == 0) {
            year = 1 - year;
        }
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        buffer.append(day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045);
    }

    /**
     * {@link DateFormatPattern#RR}，简明纪年法 在格式化输出的时候，这种格式化方法和YY一致，区别在于输入
     *
     * @references https://docs.oracle.com/cd/E11882_01/server.112/e41084/sql_elements004.htm#i116004
     */
    private void formatRRPattern(StringBuffer buffer) {
        int year = calendar.get(Calendar.YEAR);
        int era = calendar.get(Calendar.ERA);
        if (era == 0) {
            year = 1 - year;
        }
        zeroPaddingNumber(year, 2, 2, buffer);
    }

    private void formatSSSSSPattern(StringBuffer buffer) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);
        buffer.append(hour * 3600 + min * 60 + sec);
    }

    private void formatY_YYYPattern(StringBuffer buffer) {
        int year = calendar.get(Calendar.YEAR);
        int era = calendar.get(Calendar.ERA);
        if (era == 0) {
            year = 1 - year;
        }
        year = year < 0 ? -year : year;
        int thound = year / 1000;
        int left = year % 1000;
        buffer.append(thound).append(",");
        zeroPaddingNumber(left, 3, 3, buffer);
    }

    private void formatRRRRPattern(StringBuffer stringBuffer) {
        int value = calendar.get(Calendar.YEAR);
        zeroPaddingNumber(value, 1, 4, stringBuffer);
    }

    private void formatSYYYYPattern(StringBuffer buffer) {
        int year = calendar.get(Calendar.YEAR);
        int era = calendar.get(Calendar.ERA);
        if (era == 0) {
            year = 1 - year;
        }
        zeroPaddingNumber(year, 4, 4, buffer);
    }

    private void formatTimeZonePattern(int field, StringBuffer buffer) {
        TimeZone tz = calendar.getTimeZone();
        int offsetMinutes = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / 60000;
        switch (field) {
            case TAG_TZD_FIELD:
                boolean daylight = (calendar.get(Calendar.DST_OFFSET) != 0);
                buffer.append(tz.getDisplayName(daylight, TimeZone.SHORT, this.locale));
                break;
            case TAG_TZR_FIELD:
                String id = tz.getID();
                buffer.append(id == null ? "" : id.toUpperCase());
                break;
            case TAG_TZH_FIELD:
                if (offsetMinutes > 0) {
                    buffer.append("+");
                }
                int hours = offsetMinutes / 60;
                zeroPaddingNumber(hours, 2, 2, buffer);
                break;
            case TAG_TZM_FIELD:
                int mins = offsetMinutes % 60;
                zeroPaddingNumber(mins, 2, 2, buffer);
                break;
            default:
                throw new IllegalStateException("Unknown field, " + field);
        }

    }

    protected void doFormat(int field, int count, int style, StringBuffer buffer) {
        int value = calendar.get(field);
        String displayName = null;
        if (style == Calendar.SHORT || style == Calendar.LONG) {
            displayName = calendar.getDisplayName(field, style, locale);
        }
        switch (field) {
            case Calendar.YEAR: // YYYY, RRRR
                displayName = null;
                zeroPaddingNumber(value, 1, count, buffer);
                break;
            case Calendar.DAY_OF_YEAR:
                zeroPaddingNumber(value, 1, 4, buffer);
                break;
            case Calendar.MONTH:
                value += 1;
            default:
                if (displayName == null) {
                    zeroPaddingNumber(value, 2, 2, buffer);
                } else {
                    displayName = displayName.toUpperCase();
                }
                break;
        }
        if (displayName != null) {
            buffer.append(displayName);
        }
    }

    protected static int stringSize(int x) {
        for (int i = 0;; i++) {
            if (x <= SIZE_TABLE[i]) {
                return i + 1;
            }
        }
    }

    protected void zeroPaddingNumber(int value, int minDigits, int maxDigits, StringBuffer buffer) {
        try {
            if (zeroDigit == 0) {
                zeroDigit = ((DecimalFormat) numberFormat).getDecimalFormatSymbols().getZeroDigit();
            }
            if (value < 0) {
                buffer.append("-");
                zeroPaddingNumber(-value, minDigits, maxDigits, buffer);
                return;
            }
        } catch (Exception e) {
            // eat exception
        }
        int size = stringSize(value);
        if (size >= minDigits && size <= maxDigits) {
            buffer.append(value);
        } else if (size < minDigits) {
            int interval = minDigits - size;
            for (int i = 0; i < interval; i++) {
                buffer.append(zeroDigit);
            }
            buffer.append(value);
        } else {
            int mod = 10;
            for (int i = 1; i < maxDigits; i++) {
                mod *= 10;
            }
            zeroPaddingNumber(value % mod, minDigits, maxDigits, buffer);
        }
        numberFormat.setMinimumIntegerDigits(minDigits);
        numberFormat.setMaximumIntegerDigits(maxDigits);
    }

    private char[] compile(String pattern) {
        int length = pattern.length();
        boolean inDoubleQuote = false;
        StringBuilder compiledBuilder = new StringBuilder(length + 2);
        StringBuilder tmpBuilder = new StringBuilder(length);
        int count = 0;
        int lastTag = -1;

        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);
            if (c == '"') {
                /**
                 * 检测到双引号，双引号中间的东西需要原样输出 注意：oracle 无法输出双引号自身即使 double 转义也无效，这里考虑兼容这个效果 对于 ob
                 * 来说不支持格式化字符串中携带双引号的语法，亲测会报错：
                 *
                 * <code>
                 *     obclient> set session nls_date_format='YYYY "asddsdd"';
                 *     OBE-01821: date format not recognized
                 * </code>
                 *
                 * 所以这里只考虑兼容 oracle 的行为就好
                 */
                if (!inDoubleQuote) {
                    if (count != 0) {
                        encode(lastTag, count, compiledBuilder);
                        lastTag = -1;
                        count = 0;
                    }
                    tmpBuilder.setLength(0);
                    inDoubleQuote = true;
                } else {
                    encode(TAG_DOUBLE_QUOTE_CHAR, tmpBuilder.length(), compiledBuilder);
                    compiledBuilder.append(tmpBuilder);
                    inDoubleQuote = false;
                }
                continue;
            }
            if (inDoubleQuote) {
                tmpBuilder.append(c);
                continue;
            }
            if (c == '-' || c == '/' || c == ',' || c == '.' || c == ';' || c == ':' || c == ' ') {
                /**
                 * 扫描到 oracle 定义的格式化分隔符
                 */
                if (count != 0) {
                    encode(lastTag, count, compiledBuilder);
                    lastTag = -1;
                    count = 0;
                }
                compiledBuilder.append((char) (TAG_DELIMITER_CHAR << 8 | c));
                continue;
            }
            FormatPattern formatPattern = getFormatPattern(pattern, i);
            if (formatPattern == null) {
                if (ignoreUnknownFormat) {
                    if (count != 0) {
                        encode(lastTag, count, compiledBuilder);
                        lastTag = -1;
                        count = 0;
                    }
                    continue;
                }
                throw new IllegalArgumentException("Illegal pattern " + "'" + pattern + "'");
            }
            int tag = formatPattern.field();
            int styleMagicDigit = getStyle(formatPattern);
            i += (formatPattern.patternString().length() - 1);
            if (lastTag == -1 || lastTag == tag) {
                lastTag = tag;
                count += styleMagicDigit;
                continue;
            }
            encode(lastTag, count, compiledBuilder);
            lastTag = tag;
            count = styleMagicDigit;
        }
        if (inDoubleQuote) {
            throw new IllegalArgumentException("Unterminated quote");
        }
        if (count != 0) {
            encode(lastTag, count, compiledBuilder);
        }
        int len = compiledBuilder.length();
        char[] r = new char[len];
        compiledBuilder.getChars(0, len, r, 0);
        return r;
    }

    protected FormatPattern getFormatPattern(String patternStr, int begin) {
        DateFormatPattern formatPattern = this.formData.matchPattern(patternStr, begin);
        if (formatPattern == DateFormatPattern.UNDEFINE) {
            return null;
        }
        int field = formatPattern.field();
        if (field != OracleDateFormatSymbols.UNDEFINE_FIELD) {
            return formatPattern;
        }
        int tag;
        switch (formatPattern) {
            case J:
                tag = TAG_J_FIELD;
                break;
            case RR:
                tag = TAG_RR_FIELD;
                break;
            case SSSSS:
                tag = TAG_SSSSS_FIELD;
                break;
            case Y_YYY:
                tag = TAG_Y_YYY_FIELD;
                break;
            case SYYYY:
                tag = TAG_SYYYY_FIELD;
                break;
            case RRRR:
                tag = TAG_RRRR_FIELD;
                break;
            case TZD:
                tag = TAG_TZD_FIELD;
                break;
            case TZR:
                tag = TAG_TZR_FIELD;
                break;
            case TZM:
                tag = TAG_TZM_FIELD;
                break;
            case TZH:
                tag = TAG_TZH_FIELD;
                break;
            default:
                throw new IllegalStateException("Unknown pattern " + formatPattern);
        }
        return new FormatPattern() {
            @Override
            public int field() {
                return tag;
            }

            @Override
            public String patternString() {
                return formatPattern.patternString();
            }
        };
    }

    protected int getStyle(FormatPattern formatPattern) {
        String pattern = formatPattern.patternString().toUpperCase();
        switch (pattern) {
            case "MM":
            case "D":
                return NON_STYLE_MAGIC_DIGIT;
            case "DY":
            case "MON":
                return SHORT_STYLE_MAGIC_DIGIT;
            default:
                return LONG_STYLE_MAGIC_DIGIT;
        }
    }

    private void encode(int tag, int length, StringBuilder buffer) {
        if (tag != TAG_DOUBLE_QUOTE_CHAR && getCount(length) > 4) {
            /**
             * 除非是打印字符串，否则长度不能超过 4
             */
            throw new IllegalArgumentException("Invalid pattern length=" + getCount(length));
        }
        if (length < 255) {
            /**
             * 如果长度小于 255 则 2 个字节可以存下
             */
            buffer.append((char) (tag << 8 | length));
        } else {
            /**
             * 长度大于 2 个字节需要把 tag 和 length 分开存储 tag 放在第一个字节 length 的前 2 个字节放前面，后 2 个字节放后面
             */
            buffer.append((char) ((tag << 8) | 0xff));
            buffer.append((char) (length >>> 16));
            buffer.append((char) (length & 0xffff));
        }
    }

}
