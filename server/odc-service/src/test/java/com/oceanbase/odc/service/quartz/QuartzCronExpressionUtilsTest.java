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
package com.oceanbase.odc.service.quartz;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.CronExpression;

import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;

/**
 * @Author：tinker
 * @Date: 2023/1/4 17:35
 * @Descripition:
 */
public class QuartzCronExpressionUtilsTest {

    @Test
    public void adaptCronExpression() throws ParseException {

        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.DECEMBER, 31, 0, 0);
        String cron = "0 0 0 ? * 7L";
        cron = QuartzCronExpressionUtils.adaptCronExpression(cron);
        CronExpression cronExpression = new CronExpression(cron);
        Date nextValidTime = cronExpression.getNextValidTimeAfter(calendar.getTime());
        calendar.set(2023, Calendar.JANUARY, 29, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Assert.assertEquals(cron, "0 0 0 ? * 1L");
        Assert.assertEquals(calendar.getTimeInMillis(), nextValidTime.getTime());

    }

    @Test
    public void adaptCronExpression_more_space() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.DECEMBER, 31, 0, 0);
        String cron = "0   0  0 ? * 7L   ";
        cron = QuartzCronExpressionUtils.adaptCronExpression(cron);
        CronExpression cronExpression = new CronExpression(cron);
        Date nextValidTime = cronExpression.getNextValidTimeAfter(calendar.getTime());
        calendar.set(2023, Calendar.JANUARY, 29, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Assert.assertEquals(cron, "0 0 0 ? * 1L");
        Assert.assertEquals(calendar.getTimeInMillis(), nextValidTime.getTime());
    }

    @Test
    public void adaptCronExpression_week() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.DECEMBER, 31, 0, 0);
        String cron = "0   0  0 ? * 1#1    ";
        cron = QuartzCronExpressionUtils.adaptCronExpression(cron);
        CronExpression cronExpression = new CronExpression(cron);
        Date nextValidTime = cronExpression.getNextValidTimeAfter(calendar.getTime());
        calendar.set(2023, Calendar.JANUARY, 2, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Assert.assertEquals(cron, "0 0 0 ? * 2#1");
        Assert.assertEquals(calendar.getTimeInMillis(), nextValidTime.getTime());
    }

    @Test
    public void adaptCronExpression_L() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2022, Calendar.DECEMBER, 31, 0, 0);
        String cron = "0   0  0 ? * L   ";
        cron = QuartzCronExpressionUtils.adaptCronExpression(cron);
        CronExpression cronExpression = new CronExpression(cron);
        Date nextValidTime = cronExpression.getNextValidTimeAfter(calendar.getTime());
        calendar.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Assert.assertEquals(cron, "0 0 0 ? * 1");
        Assert.assertEquals(calendar.getTimeInMillis(), nextValidTime.getTime());
    }
}
