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
package com.oceanbase.odc.core.datamasking.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.datamasking.SegmentTestUtil;
import com.oceanbase.odc.core.datamasking.algorithm.Hash.HashType;
import com.oceanbase.odc.core.datamasking.algorithm.pseudonymization.Pseudonymization;
import com.oceanbase.odc.core.datamasking.data.Data;
import com.oceanbase.odc.core.datamasking.data.metadata.DoubleMetadata;
import com.oceanbase.odc.core.datamasking.data.metadata.LongMetadata;

/**
 * @author wenniu.ly
 * @date 2022/8/29
 */
public class AlgorithmImplementTest {
    @Test
    public void test_pseudo() {
        Pseudonymization ps = new Pseudonymization("a,b,c,d,0,1,2,7,8,9");
        String origin = "aceg03429";
        Data data = new Data();
        data.setValue(origin);
        data = ps.mask(data);
        Assert.assertEquals(origin.length(), data.getValue().length());
        Assert.assertNotEquals(origin, data.getValue());
    }

    @Test
    public void test_mask() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.leftOver(true, null));
        segments.add(SegmentTestUtil.digitPercentage(25, false, null));

        Mask mask = new Mask(segments);
        String origin = "abcdefghij";
        Data data = mask.mask(Data.of(origin, null));
        Assert.assertEquals(origin.length(), data.getValue().length());
        Assert.assertEquals("abc****hij", data.getValue());
    }

    @Test
    public void test_mask_with_same_delimiter() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.delimiter(".", false, null));
        segments.add(SegmentTestUtil.delimiter(".", true, null));
        segments.add(SegmentTestUtil.delimiter(".", true, null));

        Mask mask = new Mask(segments);
        String origin = "abc.def.ijg.uuu";
        Data data = mask.mask(Data.of(origin, null));
        Assert.assertEquals(origin.length(), data.getValue().length());
        Assert.assertEquals("abc.***.***.uuu", data.getValue());
    }

    @Test
    public void test_mask_with_last_left_over() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(1, true, null));
        segments.add(SegmentTestUtil.delimiter(".", true, null));
        segments.add(SegmentTestUtil.digitPercentage(100, false, null));
        segments.add(SegmentTestUtil.leftOver(true, null));

        Mask mask = new Mask(segments);
        String origin = "123456.123";
        Data data = mask.mask(Data.of(origin, null));
        Assert.assertEquals(origin.length(), data.getValue().length());
        Assert.assertEquals("******.123", data.getValue());
    }

    @Test
    public void test_mask_shorter_than_segments_size() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.delimiter(".", false, null));
        segments.add(SegmentTestUtil.delimiter(".", true, null));
        segments.add(SegmentTestUtil.delimiter(".", true, null));
        segments.add(SegmentTestUtil.delimiter(".", true, null));

        Mask mask = new Mask(segments);
        String origin = "abc.def.ijg.uuu";
        Data data = mask.mask(Data.of(origin, null));
        Assert.assertEquals("abc.***.***.uuu", data.getValue());
    }

    @Test
    public void test_mask_value_shorter_than_segments() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.leftOver(true, null));
        segments.add(SegmentTestUtil.digit(4, false, null));

        Mask mask = new Mask(segments);
        String origin = "abcdef";
        Data data = mask.mask(Data.of(origin, null));
        Assert.assertEquals("abcdef", data.getValue());
    }

    @Test
    public void test_mask_digit_percentage_not_enough_content() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digitPercentage(10, true, null));
        segments.add(SegmentTestUtil.leftOver(false, null));
        Mask mask = new Mask(segments);
        String origin = "abcdefghi";
        Data masked = mask.mask(Data.of(origin, null));
        Assert.assertEquals("*bcdefghi", masked.getValue());
    }

    @Test
    public void test_substitution() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.leftOver(true, "1111"));
        segments.add(SegmentTestUtil.digitPercentage(25, false, null));

        Substitution substitution = new Substitution(segments);
        String origin = "abcdefghij";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals(10, data.getValue().length());
        Assert.assertEquals("abc1111hij", data.getValue());
    }

    @Test
    public void test_substitution_with_different_delimiter() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.delimiter(".", false, null));
        segments.add(SegmentTestUtil.delimiter("@", true, "cc"));
        segments.add(SegmentTestUtil.delimiter("&", true, "dd"));

        Substitution substitution = new Substitution(segments);
        String origin = "abc.def@ijg&uuu";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("abc.cc@dd&uuu", data.getValue());
    }

    @Test
    public void test_substitution_with_empty_segment() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.delimiter("@", true, "***"));
        segments.add(SegmentTestUtil.leftOver(false, null));

        Substitution substitution = new Substitution(segments);
        String origin = "example@email.com";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("exa***@email.com", data.getValue());
    }

    @Test
    public void test_substitution_with_empty_segment_test_enough_prior_content() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.delimiter("@", true, "***"));
        segments.add(SegmentTestUtil.leftOver(false, null));

        Substitution substitution = new Substitution(segments);
        String origin = "exa@email.com";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("exa***@email.com", data.getValue());
    }

    @Test
    public void test_substitution_with_empty_segment_test_not_enough_prior_content() {
        // TODO: Confirm the correctness of the test case (Why not match the top segment first?)
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(5, false, null));
        segments.add(SegmentTestUtil.delimiter("@", true, "***"));
        segments.add(SegmentTestUtil.leftOver(false, null));

        Substitution substitution = new Substitution(segments);
        String origin = "ex@email.com";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("ex***@email.com", data.getValue());
    }

    @Test
    public void test_substitution_post_4_digits() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.leftOver(false, null));
        segments.add(SegmentTestUtil.digit(4, true, "***"));

        Substitution substitution = new Substitution(segments);
        String origin = "abcde";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("a***", data.getValue());
        origin = "abcd";
        data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("***", data.getValue());
    }

    @Test
    public void test_substitution_post_4_digits_not_enough_content() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.leftOver(false, null));
        segments.add(SegmentTestUtil.digit(4, true, "***"));

        Substitution substitution = new Substitution(segments);
        String origin = "abc";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("***", data.getValue());
    }

    @Test
    public void test_substitution_digit_percentage_not_enough_content() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digitPercentage(10, true, "***"));
        segments.add(SegmentTestUtil.leftOver(false, null));
        Substitution substitution = new Substitution(segments);
        String origin = "abcdefghi";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("***bcdefghi", data.getValue());
    }

    @Test
    public void test_substitution_digit_percentage_and_delimiter() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(2, true, "**"));
        segments.add(SegmentTestUtil.digitPercentage(10, true, "++"));
        segments.add(SegmentTestUtil.delimiter("@", false, null));
        segments.add(SegmentTestUtil.leftOver(false, null));
        Substitution substitution = new Substitution(segments);
        String origin = "ab@email.com";
        Data data = substitution.mask(Data.of(origin, null));
        Assert.assertEquals("**++@email.com", data.getValue());
    }

    @Test
    public void test_rounding_decimal() {
        Rounding rounding = new Rounding(true, 2);
        Data data = new Data();
        data.setValue("3.1415926");
        data.setMetadata(new DoubleMetadata());

        data = rounding.mask(data);
        Assert.assertEquals("3.14", data.getValue());
    }

    @Test
    public void test_rounding_decimal_not_enough_places() {
        Rounding rounding = new Rounding(true, 2);
        Data data = new Data();
        data.setValue("3.1");
        data.setMetadata(new DoubleMetadata());

        data = rounding.mask(data);
        Assert.assertEquals("3.10", data.getValue());
    }

    @Test
    public void test_rounding_big_double() {
        Rounding rounding = new Rounding(true, 3);
        Data data = new Data();
        data.setValue("1234567890123456789012.44444");
        data.setMetadata(new DoubleMetadata());

        data = rounding.mask(data);
        Assert.assertEquals("1234567890123456789012.444", data.getValue());
    }

    @Test
    public void test_rounding_big_integer() {
        Rounding rounding = new Rounding(false, 4);
        Data data = new Data();
        data.setValue("1234567890123456789012");
        data.setMetadata(new LongMetadata());

        data = rounding.mask(data);
        Assert.assertEquals("1234567890123456780000", data.getValue());
    }

    @Test
    public void test_rounding_integer() {
        Rounding rounding = new Rounding(false, 2);
        Data data = new Data();
        data.setValue("1234.567");
        data.setMetadata(new DoubleMetadata());

        data = rounding.mask(data);
        Assert.assertEquals("1200", data.getValue());
    }

    @Test
    public void test_rounding_integer_not_enough_places() {
        Rounding rounding = new Rounding(false, 2);
        Data data = new Data();
        data.setValue("1.23");
        data.setMetadata(new DoubleMetadata());

        data = rounding.mask(data);
        Assert.assertEquals("0", data.getValue());
    }

    @Test
    public void test_hash_sha256() {
        Hash hash = new Hash(HashType.SHA256.toString());
        Data data = new Data();
        data.setValue("abcdefg1234");

        data = hash.mask(data);
        Assert.assertEquals(64, data.getValue().length());
    }

    @Test
    public void test_hash_sha512() {
        Hash hash = new Hash(HashType.SHA512.toString());
        Data data = new Data();
        data.setValue("abcdefg1234");

        data = hash.mask(data);
        Assert.assertEquals(128, data.getValue().length());
    }

    @Test
    public void test_hash_md5() {
        Hash hash = new Hash(HashType.MD5.toString());
        Data data = new Data();
        data.setValue("abcdefg1234");

        data = hash.mask(data);
        Assert.assertEquals(32, data.getValue().length());
    }

    @Test
    public void test_hash_sm3() {
        Hash hash = new Hash(HashType.SM3.toString());
        Data data = new Data();
        data.setValue("abcdefg1234");

        data = hash.mask(data);
        Assert.assertEquals(64, data.getValue().length());
    }
}
