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

import org.junit.Assert;
import org.junit.Test;

public class TimeZoneUtilTest {

    @Test
    public void createCustomTimeZoneId_invalidZonId_returnNull() {
        Assert.assertNull(TimeZoneUtil.createCustomTimeZoneId("Asia/Shanghai"));
    }

    @Test
    public void createCustomTimeZoneId_negativeTimeZone_createSucceed() {
        String actual = TimeZoneUtil.createCustomTimeZoneId("-08:00");
        Assert.assertEquals("GMT-08:00", actual);
    }

    @Test
    public void createCustomTimeZoneId_positiveTimeZone_createSucceed() {
        String actual = TimeZoneUtil.createCustomTimeZoneId("08:00");
        Assert.assertEquals("GMT+08:00", actual);
    }

    @Test
    public void createCustomTimeZoneId_zeroFillTimeZone_createSucceed() {
        String actual = TimeZoneUtil.createCustomTimeZoneId("8:00");
        Assert.assertEquals("GMT+08:00", actual);
    }

    @Test
    public void createCustomTimeZoneId_zeroFillTimeZoneTzM_createSucceed() {
        String actual = TimeZoneUtil.createCustomTimeZoneId("8:0");
        Assert.assertEquals("GMT+08:00", actual);
    }

    @Test
    public void createCustomTimeZoneId_noTzm_returnNull() {
        Assert.assertNull(TimeZoneUtil.createCustomTimeZoneId("8:"));
    }

    @Test
    public void createCustomTimeZoneId_noTzh_returnNull() {
        Assert.assertNull(TimeZoneUtil.createCustomTimeZoneId(":8"));
    }

    @Test
    public void createCustomTimeZoneId_tooLongTzH_returnNull() {
        Assert.assertNull(TimeZoneUtil.createCustomTimeZoneId("123:08"));
    }

    @Test
    public void createCustomTimeZoneId_tooLongTzM_returnNull() {
        Assert.assertNull(TimeZoneUtil.createCustomTimeZoneId("09:123"));
    }

    @Test
    public void createCustomTimeZoneId_tooLongTzH1_returnNull() {
        Assert.assertNull(TimeZoneUtil.createCustomTimeZoneId("25:08"));
    }

    @Test
    public void createCustomTimeZoneId_tooLongTzM1_returnNull() {
        Assert.assertNull(TimeZoneUtil.createCustomTimeZoneId("09:61"));
    }
}
