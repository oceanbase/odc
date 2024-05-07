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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/4/19
 */
@RunWith(Parameterized.class)
public class TimespanFormatUtilTest {

    @Parameter(0)
    public long time;
    @Parameter(1)
    public TimeUnit timeUnit;
    @Parameter(2)
    public TemporalUnit temporalUnit;
    @Parameter(3)
    public String separator;
    @Parameter(4)
    public String output;


    @Parameters(name = "{index}: var[{0}]={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {1000, TimeUnit.MICROSECONDS, ChronoUnit.MICROS, "", "1.000ms"},
                {1550000, TimeUnit.MICROSECONDS, ChronoUnit.MICROS, "", "1.550s"},
                {180, TimeUnit.SECONDS, ChronoUnit.SECONDS, "", "3.000min"},
                {5400, TimeUnit.SECONDS, ChronoUnit.SECONDS, "", "1.500h"},
                {86400, TimeUnit.SECONDS, ChronoUnit.SECONDS, " ", "1.000 d"},
                {123000, TimeUnit.MICROSECONDS, ChronoUnit.MICROS, " ", "123.000 ms"},
                {6780000, TimeUnit.MILLISECONDS, ChronoUnit.MILLIS, " ", "1.883 h"},
                {4000, TimeUnit.SECONDS, ChronoUnit.SECONDS, "-", "1.111-h"},
                {1440, TimeUnit.MINUTES, ChronoUnit.MINUTES, "-", "1.000-d"},
                {1000000, TimeUnit.MICROSECONDS, ChronoUnit.MICROS, "-", "1.000-s"}
        });
    }

    @Test
    public void testFormatTimespan_TimeUnit() {
        Assert.assertEquals(output, TimespanFormatUtil.formatTimespan(time, timeUnit, separator));
    }

    @Test
    public void testFormatTimespan_Duration() {
        Assert.assertEquals(output, TimespanFormatUtil.formatTimespan(Duration.of(time, temporalUnit), separator));
    }

}
