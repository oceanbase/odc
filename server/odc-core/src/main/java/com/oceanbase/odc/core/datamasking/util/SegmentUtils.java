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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.datamasking.algorithm.Segment;
import com.oceanbase.odc.core.datamasking.algorithm.SegmentType;

/**
 * @author wenniu.ly
 * @date 2022/8/28
 */
public class SegmentUtils {
    private static final String EMPTY_SEGMENT_CONTENT = "";

    public static boolean verifySegments(List<Segment> segments) {
        long leftOverCount = segments.stream().filter(segment -> SegmentType.LEFT_OVER == segment.getType()).count();
        return leftOverCount <= 1;
    }

    public static int indexOfLeftOver(List<Segment> segments) {
        for (int i = 0; i < segments.size(); i++) {
            if (SegmentType.LEFT_OVER == segments.get(i).getType()) {
                return i;
            }
        }
        return -1;
    }

    public static List<Segment> splitSegments(List<Segment> segments, String raw) {
        return splitSegments(segments, raw, false);
    }

    // keepEmptySegment only apply to substitution algorithm
    public static List<Segment> splitSegments(List<Segment> segments, String raw, boolean keepEmptySegment) {
        List<Segment> result = new ArrayList<>();
        if (StringUtils.isEmpty(raw)) {
            return result;
        }
        int indexOfLeftOver = indexOfLeftOver(segments);

        int currentHead = 0;
        if (indexOfLeftOver >= 0) {
            int leftOverHead;
            int leftOverTail;

            // traverse from head
            for (int i = 0; i < indexOfLeftOver; i++) {
                Segment segment = segments.get(i);
                String nextDelimiter = getNextDelimiter(false, i + 1, indexOfLeftOver - 1, segments);
                currentHead = traverse(currentHead, raw, segment, result, keepEmptySegment, nextDelimiter);
            }
            leftOverHead = currentHead;

            // traverse from tail
            currentHead = raw.length();
            List<Segment> backwardResult = new ArrayList<>();
            for (int i = segments.size() - 1; i > indexOfLeftOver; i--) {
                Segment segment = segments.get(i);
                String nextDelimiter = getNextDelimiter(true, indexOfLeftOver + 1, i - 1, segments);
                currentHead = backwardTraverse(currentHead, raw, segment, backwardResult, keepEmptySegment,
                        nextDelimiter, leftOverHead);
            }
            Collections.reverse(backwardResult);
            leftOverTail = currentHead;

            // add left over
            if (leftOverHead < raw.length() && leftOverHead < leftOverTail) {
                Segment segment = SerializationUtils.clone(segments.get(indexOfLeftOver));
                segment.setContent(raw.substring(leftOverHead, leftOverTail));
                result.add(segment);
            } else if (keepEmptySegment) {
                addEmptySegment(true, segments.get(indexOfLeftOver), result);
            }
            result.addAll(backwardResult);
        } else {
            // traverse from head
            for (int i = 0; i < segments.size(); i++) {
                Segment segment = segments.get(i);
                String nextDelimiter = getNextDelimiter(false, i + 1, segments.size() - 1, segments);
                currentHead = traverse(currentHead, raw, segment, result, keepEmptySegment, nextDelimiter);
            }
            if (currentHead < raw.length()) {
                Segment segment = new Segment();
                segment.setType(SegmentType.NULL);
                segment.setContent(raw.substring(currentHead));
                result.add(segment);
            }
        }
        return result;
    }

    private static int traverse(int currentHead, String raw, Segment segment, List<Segment> result,
            boolean keepEmptySegment, String nextDelimiter) {
        if (currentHead >= raw.length()) {
            addEmptySegment(keepEmptySegment, segment, result);
            return currentHead;
        }

        int currentTail = raw.length();
        int nextHead = currentHead;
        if (SegmentType.DIGIT == segment.getType()) {
            currentTail = currentHead + segment.getDigitNumber();
            nextHead = currentTail;
        } else if (SegmentType.DIGIT_PERCENTAGE == segment.getType()) {
            int subLength = (int) Math.ceil(segment.getDigitPercentage() * raw.length() / 100.0);
            currentTail = currentHead + subLength;
            nextHead = currentTail;
        } else if (SegmentType.DELIMITER == segment.getType()) {
            int indexOfDelimiter = raw.indexOf(segment.getDelimiter(), currentHead);
            if (indexOfDelimiter < 0) {
                addEmptySegment(keepEmptySegment, segment, result);
                return currentHead;
            } else {
                currentTail = indexOfDelimiter;
            }
            nextHead = currentTail + segment.getDelimiter().length();
        }
        if (currentTail > raw.length()) {
            currentTail = raw.length();
        }

        if (Objects.nonNull(nextDelimiter) && raw.substring(currentHead, currentTail).contains(nextDelimiter)) {
            int indexOfNextDelimiter = raw.indexOf(nextDelimiter, currentHead);
            currentTail = indexOfNextDelimiter;
            nextHead = currentTail + nextDelimiter.length();
        }

        String subStr = raw.substring(currentHead, currentTail);
        Segment resultSegment = SerializationUtils.clone(segment);
        resultSegment.setContent(subStr);
        result.add(resultSegment);
        return nextHead;
    }

    private static int backwardTraverse(int currentHead, String raw, Segment segment, List<Segment> result,
            boolean keepEmptySegment, String nextDelimiter, int leftIndexLimit) {
        if (currentHead < leftIndexLimit) {
            addEmptySegment(keepEmptySegment, segment, result);
            return currentHead;
        }
        int currentTail = leftIndexLimit;
        int nextHead = currentHead;
        if (SegmentType.DIGIT == segment.getType()) {
            currentTail = currentHead - segment.getDigitNumber();
            nextHead = currentTail;
        } else if (SegmentType.DIGIT_PERCENTAGE == segment.getType()) {
            int subLength = (int) Math.ceil(segment.getDigitPercentage() * raw.length() / 100.0);
            currentTail = currentHead - subLength;
            nextHead = currentTail;
        } else if (SegmentType.DELIMITER == segment.getType()) {
            int indexOfDelimiter = raw.indexOf(segment.getDelimiter(), currentTail);
            if (indexOfDelimiter < 0) {
                addEmptySegment(keepEmptySegment, segment, result);
                return nextHead;
            } else {
                if (indexOfDelimiter < currentHead) {
                    Segment s = new Segment();
                    s.setType(SegmentType.NULL);
                    s.setContent(raw.substring(indexOfDelimiter + 1, currentHead));
                    result.add(s);
                }
                currentHead = indexOfDelimiter;
            }
            nextHead = currentTail;
        }
        if (currentTail < 0) {
            currentTail = 0;
        }
        currentTail = Math.max(currentTail, leftIndexLimit);

        if (Objects.nonNull(nextDelimiter) && raw.substring(currentTail, currentHead).contains(nextDelimiter)) {
            int indexOfNextDelimiter = raw.indexOf(nextDelimiter, currentTail);
            currentTail = indexOfNextDelimiter + nextDelimiter.length();
            nextHead = currentTail - nextDelimiter.length();
        }

        String subStr = raw.substring(currentTail, currentHead);
        Segment resultSegment = SerializationUtils.clone(segment);
        resultSegment.setContent(subStr);
        result.add(resultSegment);
        return nextHead;
    }

    private static void addEmptySegment(boolean keepEmptySegment, Segment segment, List<Segment> result) {
        if (!keepEmptySegment) {
            return;
        }
        Segment resultSegment = SerializationUtils.clone(segment);
        resultSegment.setContent(EMPTY_SEGMENT_CONTENT);
        result.add(resultSegment);
    }

    private static String getNextDelimiter(boolean backward, int startIndex, int endIndex, List<Segment> segments) {
        String nextDelimiter = null;
        if (backward) {
            for (int i = endIndex; i >= startIndex; i--) {
                Segment segment = segments.get(i);
                if (SegmentType.DELIMITER == segment.getType()) {
                    nextDelimiter = segment.getDelimiter();
                    break;
                }
            }
        } else {
            for (int i = startIndex; i <= endIndex; i++) {
                Segment segment = segments.get(i);
                if (SegmentType.DELIMITER == segment.getType()) {
                    nextDelimiter = segment.getDelimiter();
                    break;
                }
            }
        }
        return nextDelimiter;
    }
}
