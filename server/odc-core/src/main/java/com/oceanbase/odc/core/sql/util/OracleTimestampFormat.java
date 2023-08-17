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

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.TimeZone;

import com.oceanbase.odc.core.sql.util.OracleTimestampFormatSymbols.TimestampFormatPattern;

import lombok.NonNull;

/**
 * {@link OracleTimestampFormat}
 *
 * @author yh263208
 * @date 2022-11-06 16:37
 * @since ODC_release_4.1.0
 */
public class OracleTimestampFormat extends OracleDateFormat {

    private static final int TAG_FF_1_FIELD = 120;
    private static final int TAG_FF_2_FIELD = 121;
    private static final int TAG_FF_3_FIELD = 122;
    private static final int TAG_FF_4_FIELD = 123;
    private static final int TAG_FF_5_FIELD = 124;
    private static final int TAG_FF_6_FIELD = 125;
    private static final int TAG_FF_7_FIELD = 126;
    private static final int TAG_FF_8_FIELD = 127;
    private static final int TAG_FF_9_FIELD = 128;
    private OracleTimestampFormatSymbols formData;
    private Timestamp timestamp;

    public OracleTimestampFormat(String pattern) {
        this(pattern, TimeZone.getDefault(), Locale.getDefault(Locale.Category.FORMAT), true);
    }

    public OracleTimestampFormat(String pattern, Locale locale) {
        this(pattern, TimeZone.getDefault(), locale, true);
    }

    public OracleTimestampFormat(@NonNull String pattern,
            @NonNull Locale locale, boolean ignoreUnknownFormat) {
        this(pattern, TimeZone.getDefault(), locale, ignoreUnknownFormat);
    }

    public OracleTimestampFormat(@NonNull String pattern, @NonNull TimeZone timeZone,
            @NonNull Locale locale, boolean ignoreUnknownFormat) {
        super(pattern, timeZone, locale, ignoreUnknownFormat);
    }

    public String format(Timestamp timestamp) {
        this.timestamp = timestamp;
        return super.format(timestamp);
    }

    @Override
    protected void doFormat(int field, int count, int style, StringBuffer buffer) {
        int nano = this.timestamp.getNanos();
        switch (field) {
            case TAG_FF_1_FIELD:
                zeroPaddingNano(nano, 1, buffer);
                break;
            case TAG_FF_2_FIELD:
                zeroPaddingNano(nano, 2, buffer);
                break;
            case TAG_FF_3_FIELD:
                zeroPaddingNano(nano, 3, buffer);
                break;
            case TAG_FF_4_FIELD:
                zeroPaddingNano(nano, 4, buffer);
                break;
            case TAG_FF_5_FIELD:
                zeroPaddingNano(nano, 5, buffer);
                break;
            case TAG_FF_6_FIELD:
                zeroPaddingNano(nano, 6, buffer);
                break;
            case TAG_FF_7_FIELD:
                zeroPaddingNano(nano, 7, buffer);
                break;
            case TAG_FF_8_FIELD:
                zeroPaddingNano(nano, 8, buffer);
                break;
            case TAG_FF_9_FIELD:
                zeroPaddingNano(nano, 9, buffer);
                break;
            default:
                super.doFormat(field, count, style, buffer);
        }
    }

    protected void zeroPaddingNano(int nano, int digits, StringBuffer buffer) {
        if (zeroDigit == 0) {
            zeroDigit = ((DecimalFormat) numberFormat).getDecimalFormatSymbols().getZeroDigit();
        }
        if (nano < 0) {
            throw new IllegalArgumentException("Nano can not be negative, " + nano);
        }
        int size = stringSize(nano);
        String nanosString = Integer.toString(nano);
        char[] strs = new char[9];
        int zeroPaddings = 9 - size;
        if (zeroPaddings < 0) {
            throw new IllegalArgumentException("Nano's length " + size + " is too long ");
        }
        for (int i = 0; i < digits; i++) {
            if (i < zeroPaddings) {
                strs[i] = zeroDigit;
            } else {
                strs[i] = nanosString.charAt(i - zeroPaddings);
            }
        }
        buffer.append(new String(strs, 0, digits));
    }

    @Override
    protected FormatPattern getFormatPattern(String patternStr, int begin) {
        FormatPattern p = super.getFormatPattern(patternStr, begin);
        if (p != null) {
            return p;
        }
        if (this.formData == null) {
            this.formData = new OracleTimestampFormatSymbols();
        }
        TimestampFormatPattern formatPattern = this.formData.matchPattern(patternStr, begin);
        if (formatPattern == TimestampFormatPattern.UNDEFINE) {
            return null;
        }
        int field = formatPattern.field();
        if (field != OracleDateFormatSymbols.UNDEFINE_FIELD) {
            return formatPattern;
        }
        int tag;
        switch (formatPattern) {
            case FF_1:
                tag = TAG_FF_1_FIELD;
                break;
            case FF_2:
                tag = TAG_FF_2_FIELD;
                break;
            case FF_3:
                tag = TAG_FF_3_FIELD;
                break;
            case FF_4:
                tag = TAG_FF_4_FIELD;
                break;
            case FF_5:
                tag = TAG_FF_5_FIELD;
                break;
            case FF_6:
                tag = TAG_FF_6_FIELD;
                break;
            case FF_7:
                tag = TAG_FF_7_FIELD;
                break;
            case FF_8:
                tag = TAG_FF_8_FIELD;
                break;
            case FF:
            case FF_9:
                tag = TAG_FF_9_FIELD;
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

}
