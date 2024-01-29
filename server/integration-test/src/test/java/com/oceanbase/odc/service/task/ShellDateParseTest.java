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
package com.oceanbase.odc.service.task;

import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.common.util.ShellDateUtils;

/**
 * @author yaobin
 * @date 2024-01-29
 * @since 4.2.4
 */
public class ShellDateParseTest {

    @Test
    public void test_process() {

        String s = "五 1/26 19:51:43 2024";
        Date date = ShellDateUtils.parseWithWeek(s);
        Assert.assertNotNull(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Assert.assertEquals(2024, calendar.get(Calendar.YEAR));
        Assert.assertEquals(1, calendar.get(Calendar.MONTH) + 1);

        s = "Fri Jan 5 20:56:26 2024";
        date = ShellDateUtils.parseWithWeek(s);
        Assert.assertNotNull(date);
        calendar.setTime(date);
        Assert.assertEquals(2024, calendar.get(Calendar.YEAR));
        Assert.assertEquals(1, calendar.get(Calendar.MONTH) + 1);
    }

}
