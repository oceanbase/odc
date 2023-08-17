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

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class TimeUtilsTest {
    @Test
    public void test_absMillisBetween_Success() {
        Date oldDate = new Date(1000L);
        Date newDate = new Date(2000L);
        long gap1 = TimeUtils.absMillisBetween(oldDate.toInstant(), newDate.toInstant());
        long gap2 = TimeUtils.absMillisBetween(newDate.toInstant(), oldDate.toInstant());
        Assert.assertEquals(1000L, gap1);
        Assert.assertEquals(1000L, gap2);
    }
}
