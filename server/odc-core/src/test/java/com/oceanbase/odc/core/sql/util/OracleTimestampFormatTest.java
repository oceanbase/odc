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
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for {@link OracleTimestampFormat}
 *
 * @author yh263208
 * @date 2022-11-06 17:30
 * @since ODC_release_4.1.0
 */
public class OracleTimestampFormatTest {

    @Test
    public void format_withFF1Format_print1Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF1");
        Assert.assertEquals("1", format.format(timestamp));
    }

    @Test
    public void format_withFF2Format_print12Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF2");
        Assert.assertEquals("12", format.format(timestamp));
    }

    @Test
    public void format_withFF3Format_print123Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF3");
        Assert.assertEquals("123", format.format(timestamp));
    }

    @Test
    public void format_withFF4Format_print1234Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF4");
        Assert.assertEquals("1234", format.format(timestamp));
    }

    @Test
    public void format_withFF5Format_print12345Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF5");
        Assert.assertEquals("12345", format.format(timestamp));
    }

    @Test
    public void format_withFF6Format_print123456Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF6");
        Assert.assertEquals("123456", format.format(timestamp));
    }

    @Test
    public void format_withFF7Format_print1234567Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF7");
        Assert.assertEquals("1234567", format.format(timestamp));
    }

    @Test
    public void format_withFF8Format_print12345678Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF8");
        Assert.assertEquals("12345678", format.format(timestamp));
    }

    @Test
    public void format_withFF9Format_print123456789Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF9");
        Assert.assertEquals("123456789", format.format(timestamp));
    }

    @Test
    public void format_withFFFormat_print123456789Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123456789);
        OracleTimestampFormat format = new OracleTimestampFormat("FF");
        Assert.assertEquals("123456789", format.format(timestamp));
    }

    @Test
    public void format_withFF5Format_print00000Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(123);
        OracleTimestampFormat format = new OracleTimestampFormat("FF5");
        Assert.assertEquals("00000", format.format(timestamp));
    }

    @Test
    public void format_withFF6Format_print008089Nano() {
        Timestamp timestamp = new Timestamp(new Date().getTime());
        timestamp.setNanos(8089000);
        OracleTimestampFormat format = new OracleTimestampFormat("FF6");
        Assert.assertEquals("008089", format.format(timestamp));
    }
}
