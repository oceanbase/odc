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
package com.oceanbase.odc.common.unit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test cases for {@link BinarySize}
 *
 * @author yh263208
 * @date 2022-11-10 14:48
 * @since ODC_release_4.1.0
 */
public class BinarySizeTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final Object[][] input2Str = new Object[][] {
            {0L, "0 B"}, {27L, "27 B"}, {999L, "999 B"}, {1000L, "1000 B"},
            {1023L, "1023 B"}, {1024L, "1 KB"}, {1728L, "1.7 KB"}, {110592L, "108 KB"},
            {7077888L, "6.8 MB"}, {452984832L, "432 MB"}, {28991029248L, "27 GB"},
            {1855425871872L, "1.7 TB"}, {9223372036854775807L, "8.0 EB"}
    };

    @Test
    public void convert_EB2B_convertSucceed() {
        long eb = 5L << 60;
        long pb = 12L << 50;
        long tb = 345L << 40;
        long gb = 567L << 30;
        long mb = 789L << 20;
        long kb = 456L << 10;
        long b = 12L;
        long size = eb + pb + tb + gb + mb + kb + b;
        BinarySize bSize = BinarySizeUnit.B.of(size);
        BinarySize ebSize = bSize.convert(BinarySizeUnit.EB);
        Assert.assertEquals(size, ebSize.convert(BinarySizeUnit.B).getSizeDigit());
    }

    @Test
    public void convert_EB2GB_convertSucceed() {
        long eb = 5L << 60;
        long pb = 12L << 50;
        long tb = 345L << 40;
        long gb = 567L << 30;
        long mb = 789L << 20;
        long kb = 456L << 10;
        long b = 12L;
        long size = eb + pb + tb + gb + mb + kb + b;
        BinarySize bSize = BinarySizeUnit.B.of(size);
        BinarySize gbSize = bSize.convert(BinarySizeUnit.GB);
        long gbSizeDigit = size / (1024L * 1024L * 1024L);
        Assert.assertEquals(gbSizeDigit, gbSize.getSizeDigit());
    }

    @Test
    public void convert_sizeIsOverFlow_expThrown() {
        long eb = 12L << 60;
        long pb = 12L << 50;
        long tb = 345L << 40;
        long gb = 567L << 30;
        long mb = 789L << 20;
        long kb = 456L << 10;
        long b = 12L;
        long size = eb + pb + tb + gb + mb + kb + b;
        thrown.expectMessage("Size is overflow");
        thrown.expect(IllegalArgumentException.class);
        BinarySizeUnit.B.of(size);
    }

    @Test
    public void convert_0EB2B_0BReturn() {
        BinarySize b = BinarySizeUnit.EB.of(0);
        Assert.assertEquals(0, b.convert(BinarySizeUnit.B).getSizeDigit());
    }

    @Test
    public void convert_27B2B_27BReturn() {
        BinarySize b = BinarySizeUnit.B.of(27);
        Assert.assertEquals(27, b.convert(BinarySizeUnit.B).getSizeDigit());
    }

    @Test
    public void convert_999B2B_999BReturn() {
        BinarySize b = BinarySizeUnit.B.of(999);
        Assert.assertEquals(999, b.convert(BinarySizeUnit.B).getSizeDigit());
    }

    @Test
    public void convert_1000B2B_1000BReturn() {
        BinarySize b = BinarySizeUnit.B.of(1000);
        Assert.assertEquals(1000, b.convert(BinarySizeUnit.B).getSizeDigit());
    }

    @Test
    public void convert_1023B2KB_0KBReturn() {
        BinarySize b = BinarySizeUnit.B.of(1023);
        Assert.assertEquals(0, b.convert(BinarySizeUnit.KB).getSizeDigit());
    }

    @Test
    public void convert_1024B2KB_1KBReturn() {
        BinarySize b = BinarySizeUnit.B.of(1024);
        Assert.assertEquals(1, b.convert(BinarySizeUnit.KB).getSizeDigit());
    }

    @Test
    public void convert_1728B2KB_1KB_704BReturn() {
        BinarySize b = BinarySizeUnit.B.of(1728);

        BinarySize kb = b.convert(BinarySizeUnit.KB);
        Assert.assertEquals(1, kb.getSizeDigit());
        Assert.assertEquals(704, kb.getMod().getSizeDigit());
    }

    @Test
    public void convert_110592B2KB_108KBReturn() {
        BinarySize b = BinarySizeUnit.B.of(110592);

        BinarySize kb = b.convert(BinarySizeUnit.KB);
        Assert.assertEquals(108, kb.getSizeDigit());
    }

    @Test
    public void convert_7077888B2MB_6MB_768KBReturn() {
        BinarySize b = BinarySizeUnit.B.of(7077888);

        BinarySize mb = b.convert(BinarySizeUnit.MB);
        Assert.assertEquals(6, mb.getSizeDigit());
        Assert.assertEquals(768, mb.getMod().getSizeDigit());
    }

    @Test
    public void convert_452984832B2MB_432MBReturn() {
        BinarySize b = BinarySizeUnit.B.of(452984832);

        BinarySize mb = b.convert(BinarySizeUnit.MB);
        Assert.assertEquals(432, mb.getSizeDigit());
        Assert.assertNull(mb.getMod());
    }

    @Test
    public void convert_28991029248B2GB_27GBReturn() {
        BinarySize b = BinarySizeUnit.B.of(28991029248L);

        BinarySize gb = b.convert(BinarySizeUnit.GB);
        Assert.assertEquals(27, gb.getSizeDigit());
        Assert.assertNull(gb.getMod());
    }

    @Test
    public void convert_1855425871872B2TB_1TB_704GBReturn() {
        BinarySize b = BinarySizeUnit.B.of(1855425871872L);

        BinarySize tb = b.convert(BinarySizeUnit.TB);
        Assert.assertEquals(1, tb.getSizeDigit());
        Assert.assertEquals(704, tb.getMod().getSizeDigit());
    }

    @Test
    public void convert_9223372036854775807B2EB_7EB_1023PB_1023TB_1023GB_1023MB_1023KB_1023BReturn() {
        BinarySize b = BinarySizeUnit.B.of(Long.MAX_VALUE);

        BinarySize eb = b.convert(BinarySizeUnit.EB);
        Assert.assertEquals(7, eb.getSizeDigit());

        BinarySize pb = eb.getMod();
        Assert.assertEquals(1023, pb.getSizeDigit());

        BinarySize tb = pb.getMod();
        Assert.assertEquals(1023, tb.getSizeDigit());

        BinarySize gb = tb.getMod();
        Assert.assertEquals(1023, gb.getSizeDigit());

        BinarySize mb = gb.getMod();
        Assert.assertEquals(1023, mb.getSizeDigit());

        BinarySize kb = mb.getMod();
        Assert.assertEquals(1023, kb.getSizeDigit());

        b = kb.getMod();
        Assert.assertEquals(1023, b.getSizeDigit());
    }

    @Test
    public void toString_givingInput_returnExpectOutput() {
        for (Object[] in2Out : input2Str) {
            Assert.assertEquals(in2Out[1], BinarySizeUnit.B.of((Long) in2Out[0]).toString());
        }
    }

    @Test
    public void compareTo_twoEqualSize_return0() {
        BinarySize mb_5 = BinarySizeUnit.B.of(1024 * 1024 * 5);
        BinarySize mb_5_1 = BinarySizeUnit.MB.of(5);
        Assert.assertEquals(0, mb_5.compareTo(mb_5_1));
    }

    @Test
    public void compareTo_firstBiggerThanSecond_return1() {
        BinarySize first = BinarySizeUnit.B.of(1024 * 1024 * 5 + 1024 * 4 + 221);
        BinarySize second = BinarySizeUnit.MB.of(5);
        Assert.assertEquals(1, first.compareTo(second));
    }

    @Test
    public void compareTo_firstSmallerThanSecond_return_1() {
        BinarySize second = BinarySizeUnit.B.of(1024 * 1024 * 543 + 1024 * 789 + 221);
        BinarySize first = BinarySizeUnit.MB.of(5);
        Assert.assertEquals(-1, first.compareTo(second));
    }

}
