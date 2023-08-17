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
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test cases for {@link OracleDateFormat}
 *
 * @author yh263208
 * @date 2022-11-02 11:08
 * @since ODC_release_4.0.1
 */
public class OracleDateFormatTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void format_beforeChristDateUsingB_C_Format_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(-221, Calendar.JANUARY, 1);
        DateFormat format = new OracleDateFormat("B.C.", Locale.US);
        Assert.assertEquals("BC", format.format(calendar.getTime()));
    }

    @Test
    public void format_beforeChristDateUsingADFormat_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(-221, Calendar.JANUARY, 1);
        DateFormat format = new OracleDateFormat("AD", Locale.US);
        Assert.assertEquals("BC", format.format(calendar.getTime()));
    }

    @Test
    public void format_afterChristDateUsingBCFormat_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(221, Calendar.JANUARY, 1);
        DateFormat format = new OracleDateFormat("BC", Locale.US);
        Assert.assertEquals("AD", format.format(calendar.getTime()));
    }

    @Test
    public void format_afterChristDateUsingA_D_Format_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(221, Calendar.JANUARY, 1);
        DateFormat format = new OracleDateFormat("A.D.", Locale.US);
        Assert.assertEquals("AD", format.format(calendar.getTime()));
    }

    @Test
    public void format_pmTimeUsingP_M_Format_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        DateFormat format = new OracleDateFormat("P.M.", Locale.US);
        Assert.assertEquals("PM", format.format(calendar.getTime()));
    }

    @Test
    public void format_pmTimeUsingAMFormat_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        DateFormat format = new OracleDateFormat("AM", Locale.US);
        Assert.assertEquals("PM", format.format(calendar.getTime()));
    }

    @Test
    public void format_amTimeUsingPMFormat_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        DateFormat format = new OracleDateFormat("PM", Locale.US);
        Assert.assertEquals("AM", format.format(calendar.getTime()));
    }

    @Test
    public void format_amTimeUsingA_M_Format_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        DateFormat format = new OracleDateFormat("A.M.", Locale.US);
        Assert.assertEquals("AM", format.format(calendar.getTime()));
    }

    @Test
    public void format_secondDayOfWeekUsingDFormat_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, 1);
        DateFormat format = new OracleDateFormat("D", Locale.US);
        Assert.assertEquals("01", format.format(calendar.getTime()));
    }

    @Test
    public void format_firstDayOfMonthUsingDDFormat_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        DateFormat format = new OracleDateFormat("DD", Locale.US);
        Assert.assertEquals("01", format.format(calendar.getTime()));
    }

    @Test
    public void format_324DayOfYearUsingDDDFormat_formatSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, 324);
        DateFormat format = new OracleDateFormat("DDD", Locale.US);
        Assert.assertEquals("324", format.format(calendar.getTime()));
    }

    @Test
    public void format_TueDateUsingDAYFormat_returnTUESDAY() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
        DateFormat format = new OracleDateFormat("DAY", Locale.US);
        Assert.assertEquals("TUESDAY", format.format(calendar.getTime()));
    }

    @Test
    public void format_WedDateUsingDYFormat_returnWED() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        DateFormat format = new OracleDateFormat("DY", Locale.US);
        Assert.assertEquals("WED", format.format(calendar.getTime()));
    }

    @Test
    public void format_15pmTimeUsingHHFormat_return03() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        DateFormat format = new OracleDateFormat("HH", Locale.US);
        Assert.assertEquals("03", format.format(calendar.getTime()));
    }

    @Test
    public void format_18pmTimeUsingHH12Format_return06() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        DateFormat format = new OracleDateFormat("HH12", Locale.US);
        Assert.assertEquals("06", format.format(calendar.getTime()));
    }

    @Test
    public void format_18pmTimeUsingHH24Format_return18() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        DateFormat format = new OracleDateFormat("HH24", Locale.US);
        Assert.assertEquals("18", format.format(calendar.getTime()));
    }

    @Test
    public void format_2022_11_02UsingJFormat_return2459886() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.NOVEMBER, 2);
        DateFormat format = new OracleDateFormat("J", Locale.US);
        Assert.assertEquals("2459886", format.format(calendar.getTime()));
    }

    @Test
    public void format_35MinUsingMIFormat_return35() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 35);
        DateFormat format = new OracleDateFormat("MI", Locale.US);
        Assert.assertEquals("35", format.format(calendar.getTime()));
    }

    @Test
    public void format_aprilDateUsingMMFormat_return4() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.MONTH, Calendar.APRIL);
        DateFormat format = new OracleDateFormat("MM", Locale.US);
        Assert.assertEquals("04", format.format(calendar.getTime()));
    }

    @Test
    public void format_novDateUsingMONFormat_returnNOV() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        DateFormat format = new OracleDateFormat("MON", Locale.US);
        Assert.assertEquals("NOV", format.format(calendar.getTime()));
    }

    @Test
    public void format_novDateUsingMONTHFormat_returnNOVEMBER() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        DateFormat format = new OracleDateFormat("MONTH", Locale.US);
        Assert.assertEquals("NOVEMBER", format.format(calendar.getTime()));
    }

    @Test
    public void format_2002UsingRRFormat_return02() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2002);
        DateFormat format = new OracleDateFormat("RR", Locale.US);
        Assert.assertEquals("02", format.format(calendar.getTime()));
    }

    @Test
    public void format_2002UsingRRRRFormat_return2002() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2002);
        DateFormat format = new OracleDateFormat("RRRR", Locale.US);
        Assert.assertEquals("2002", format.format(calendar.getTime()));
    }

    @Test
    public void format_15_34_23TimeUsingSSFormat_return23() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        calendar.set(Calendar.MINUTE, 34);
        calendar.set(Calendar.SECOND, 23);
        DateFormat format = new OracleDateFormat("SS", Locale.US);
        Assert.assertEquals("23", format.format(calendar.getTime()));
    }

    @Test
    public void format_15_34_23TimeUsingSSSSSFormat_return56063() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        calendar.set(Calendar.MINUTE, 34);
        calendar.set(Calendar.SECOND, 23);
        DateFormat format = new OracleDateFormat("SSSSS", Locale.US);
        String expect = 15 * 3600 + 34 * 60 + 23 + "";
        Assert.assertEquals(expect, format.format(calendar.getTime()));
    }

    @Test
    public void format_2022YearDateUsingY_YYYFormat_return2_022() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2022);
        DateFormat format = new OracleDateFormat("Y,YYY", Locale.US);
        Assert.assertEquals("2,022", format.format(calendar.getTime()));
    }

    @Test
    public void format_1234YearDateUsingYFormat_return4() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1234);
        DateFormat format = new OracleDateFormat("Y", Locale.US);
        Assert.assertEquals("4", format.format(calendar.getTime()));
    }

    @Test
    public void format_1234YearDateUsingYYFormat_return34() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1234);
        DateFormat format = new OracleDateFormat("YY", Locale.US);
        Assert.assertEquals("34", format.format(calendar.getTime()));
    }

    @Test
    public void format_1234YearDateUsingYYYFormat_return234() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1234);
        DateFormat format = new OracleDateFormat("YYY", Locale.US);
        Assert.assertEquals("234", format.format(calendar.getTime()));
    }

    @Test
    public void format_1234YearDateUsingYYYYFormat_return1234() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1234);
        DateFormat format = new OracleDateFormat("YYYY", Locale.US);
        Assert.assertEquals("1234", format.format(calendar.getTime()));
    }

    @Test
    public void format_yearBeforeChristDateUsingSYYYYFormat_returnMinusBeforeYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, -221);
        DateFormat format = new OracleDateFormat("SYYYY", Locale.US);
        Assert.assertEquals("-0221", format.format(calendar.getTime()));
    }

    @Test
    public void format_yearAfterChristDateUsingSYYYYFormat_returnNoMinusBeforeYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 21);
        DateFormat format = new OracleDateFormat("SYYYY", Locale.US);
        Assert.assertEquals("0021", format.format(calendar.getTime()));
    }

    @Test
    public void format_containsUnrecognizedPatternWithIgnoreSetting_ignoreUnRecognizedPattern() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.APRIL, 3);
        OracleDateFormat format = new OracleDateFormat("RR-bbnnmMM-DD", Locale.US);
        Assert.assertEquals("22-04-03", format.format(calendar.getTime()));
    }

    @Test
    public void format_containsUnrecognizedPatternWithNoIgnoreSetting_expThrown() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.APRIL, 3);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Illegal pattern 'RR-bbnnmMM-DD'");
        new OracleDateFormat("RR-bbnnmMM-DD", Locale.US, false);
    }

    @Test
    public void format_containsCustomStr_printSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.APRIL, 3);
        DateFormat dateFormat = new OracleDateFormat("RR-\"bb\"\"nnm\"MM-DD", Locale.US, false);
        Assert.assertEquals("22-bbnnm04-03", dateFormat.format(calendar.getTime()));
    }

    @Test
    public void format_containsUnclosedQuoteStr_expThrown() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.APRIL, 3);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unterminated quote");
        new OracleDateFormat("RR-\"bb\"\"nnmMM-DD", Locale.US, false);
    }

    @Test
    public void format_containsCustomStrLengthBiggerThan255_printSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.APRIL, 3);
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 257; i++) {
            buffer.append(i);
        }
        String custom = buffer.substring(0, 257);
        DateFormat dateFormat = new OracleDateFormat("RR-\"" + custom + "\"MM-DD", Locale.US, false);
        String expect = "22-" + custom + "04-03";
        Assert.assertEquals(expect, dateFormat.format(calendar.getTime()));
    }

    @Test
    public void format_containsTZDAndTZR_printTimeZoneSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.APRIL, 3);
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        DateFormat dateFormat = new OracleDateFormat("TZD,TZR", Locale.US, false);
        Assert.assertEquals("CST,ASIA/SHANGHAI", dateFormat.format(calendar.getTime()));
    }

    @Test
    public void format_containsTZHAndTZM_printTimeZoneSucceed() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.APRIL, 3);
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        DateFormat dateFormat = new OracleDateFormat("TZH:TZM", Locale.US, false);
        Assert.assertEquals("+08:00", dateFormat.format(calendar.getTime()));
    }

}
