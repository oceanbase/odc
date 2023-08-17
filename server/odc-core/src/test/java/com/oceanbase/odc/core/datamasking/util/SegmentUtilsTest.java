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
package com.oceanbase.odc.core.datamasking.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.datamasking.SegmentTestUtil;
import com.oceanbase.odc.core.datamasking.algorithm.Segment;

/**
 * @author wenniu.ly
 * @date 2022/8/29
 */
public class SegmentUtilsTest {
    private static final String EMPTY_SEGMENT_CONTENT = "";

    @Test
    public void test_verify_segments_true() {
        Assert.assertEquals(true, SegmentUtils.verifySegments(prepareSegments(1)));
    }

    @Test
    public void test_verify_segments_false() {
        Assert.assertEquals(false, SegmentUtils.verifySegments(prepareSegments(2)));
    }

    @Test
    public void test_index_of_left_over() {
        Assert.assertEquals(2, SegmentUtils.indexOfLeftOver(prepareSegments(1)));
    }

    @Test
    public void test_index_of_none_left_over() {
        Assert.assertEquals(-1, SegmentUtils.indexOfLeftOver(prepareSegments(0)));
    }

    @Test
    public void test_split_segments_no_left_over() {
        List<Segment> segments = prepareSegments(0);
        List<Segment> result = SegmentUtils.splitSegments(segments, "abcdefg");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals("de", result.get(1).getContent());
        Assert.assertEquals("fg", result.get(2).getContent());
    }

    @Test
    public void test_split_segments_left_over_at_tail() {
        List<Segment> segments = prepareSegments(0);
        segments.add(SegmentTestUtil.leftOver(false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "abcdefg");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals("de", result.get(1).getContent());
        Assert.assertEquals("fg", result.get(2).getContent());
    }

    @Test
    public void test_split_segments_left_over_at_head() {
        List<Segment> segments = prepareSegments(0);
        segments.add(0, SegmentTestUtil.leftOver(false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "abcdefg");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("ab", result.get(0).getContent());
        Assert.assertEquals("cde", result.get(1).getContent());
        Assert.assertEquals("fg", result.get(2).getContent());
    }

    @Test
    public void test_split_segments_left_over_in_middle() {
        List<Segment> segments = prepareSegments(0);
        segments.add(1, SegmentTestUtil.leftOver(false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "abcdefg");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals("de", result.get(1).getContent());
        Assert.assertEquals("fg", result.get(2).getContent());
    }

    @Test
    public void test_split_segments_null_raw_string() {
        List<Segment> segments = prepareSegments(0);
        segments.add(1, SegmentTestUtil.leftOver(false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, null);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void test_split_segments_empty_raw_string() {
        List<Segment> segments = prepareSegments(0);
        segments.add(1, SegmentTestUtil.leftOver(false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void test_split_segments_raw_shorter_than_segments() {
        List<Segment> segments = prepareSegments(0);
        List<Segment> result = SegmentUtils.splitSegments(segments, "abc");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
    }

    @Test
    public void test_split_segments_raw_shorter_than_segments_with_left_over() {
        List<Segment> segments = prepareSegments(0);
        segments.add(2, SegmentTestUtil.leftOver(false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "abcd");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals("d", result.get(1).getContent());
    }

    @Test
    public void test_split_segments_with_delimiter() {
        List<Segment> segments = prepareSegments(0);
        segments.add(2, SegmentTestUtil.delimiter("@", false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "exa@email.com");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("exa", result.get(0).getContent());
        Assert.assertEquals(EMPTY_SEGMENT_CONTENT, result.get(1).getContent());
        Assert.assertEquals("email.com", result.get(2).getContent());
    }

    @Test
    public void test_split_segments_with_left_over_before_delimiter() {
        List<Segment> segments = prepareSegments(0);
        segments.add(2, SegmentTestUtil.leftOver(false, null));
        segments.add(3, SegmentTestUtil.delimiter("@", false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "example@email.com");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("exa", result.get(0).getContent());
        Assert.assertEquals("mple@", result.get(1).getContent());
        Assert.assertEquals("email.com", result.get(2).getContent());
    }

    private List<Segment> prepareSegments(int numberOfLeftOver) {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.digitPercentage(25, false, null));
        for (int i = 0; i < numberOfLeftOver; i++) {
            segments.add(SegmentTestUtil.leftOver(false, null));
        }
        return segments;
    }

    @Test
    public void test_split_segments_keep_empty_digit_percent_true() {
        List<Segment> segments = prepareSegments(0);
        List<Segment> result = SegmentUtils.splitSegments(segments, "abc", true);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals(EMPTY_SEGMENT_CONTENT, result.get(1).getContent());
    }

    @Test
    public void test_split_segments_keep_empty_digit_true() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.digit(3, false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "abc", true);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals(EMPTY_SEGMENT_CONTENT, result.get(1).getContent());
    }


    @Test
    public void test_split_segments_keep_empty_delimiter_true() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.delimiter(".", false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "abcdef", true);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals(EMPTY_SEGMENT_CONTENT, result.get(1).getContent());
        Assert.assertEquals("def", result.get(2).getContent());
    }

    @Test
    public void test_split_segments_keep_empty_left_over_last_true() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.leftOver(false, null));
        List<Segment> result = SegmentUtils.splitSegments(segments, "abc", true);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals(EMPTY_SEGMENT_CONTENT, result.get(1).getContent());
    }

    @Test
    public void test_split_segments_keep_empty_left_over_inside_true() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.leftOver(false, null));
        segments.add(SegmentTestUtil.digit(3, false, null));

        List<Segment> result = SegmentUtils.splitSegments(segments, "abcdef", true);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("abc", result.get(0).getContent());
        Assert.assertEquals(EMPTY_SEGMENT_CONTENT, result.get(1).getContent());
        Assert.assertEquals("def", result.get(2).getContent());
    }

    @Test
    public void test_split_segments_with_empty_segment_test_not_enough_prior_content() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(5, false, null));
        segments.add(SegmentTestUtil.delimiter("@", true, "***"));
        segments.add(SegmentTestUtil.leftOver(false, null));

        List<Segment> result = SegmentUtils.splitSegments(segments, "ex@email.com", true);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("ex", result.get(0).getContent());
        Assert.assertEquals(EMPTY_SEGMENT_CONTENT, result.get(1).getContent());
        Assert.assertEquals("email.com", result.get(2).getContent());
    }

    @Test
    public void test_split_segments_with_backward_empty_segment_test_not_enough_prior_content() {
        List<Segment> segments = new ArrayList<>();
        segments.add(SegmentTestUtil.digit(3, false, null));
        segments.add(SegmentTestUtil.leftOver(false, null));
        segments.add(SegmentTestUtil.delimiter("@", true, "***"));
        segments.add(SegmentTestUtil.digit(6, true, "xxx"));

        List<Segment> result = SegmentUtils.splitSegments(segments, "exabcd@emai", true);
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("exa", result.get(0).getContent());
        Assert.assertEquals(EMPTY_SEGMENT_CONTENT, result.get(1).getContent());
        Assert.assertEquals("bcd", result.get(2).getContent());
        Assert.assertEquals("emai", result.get(3).getContent());
    }
}
