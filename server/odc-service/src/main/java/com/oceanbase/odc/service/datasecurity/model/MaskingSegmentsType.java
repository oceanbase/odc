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
package com.oceanbase.odc.service.datasecurity.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gaoda.xy
 * @date 2023/5/18 11:24
 */
public enum MaskingSegmentsType {
    CUSTOM,
    /**
     * Suitable for MASK in {@link MaskingAlgorithmType}
     */
    PRE_1_POST_1,
    PRE_3_POST_2,
    PRE_3_POST_4,
    /**
     * Suitable for SUBSTITUTION in {@link MaskingAlgorithmType}
     */
    ALL,
    PRE_3,
    POST_4;

    private static final MaskingSegment DIGIT_1_NOT_MASK_SEGMENT =
            MaskingSegment.of(false, MaskingSegmentType.DIGIT, 1);
    private static final MaskingSegment DIGIT_2_NOT_MASK_SEGMENT =
            MaskingSegment.of(false, MaskingSegmentType.DIGIT, 2);
    private static final MaskingSegment DIGIT_3_MASK_SEGMENT =
            MaskingSegment.of(true, MaskingSegmentType.DIGIT, 3);
    private static final MaskingSegment DIGIT_3_NOT_MASK_SEGMENT =
            MaskingSegment.of(false, MaskingSegmentType.DIGIT, 3);
    private static final MaskingSegment DIGIT_4_MASK_SEGMENT =
            MaskingSegment.of(true, MaskingSegmentType.DIGIT, 4);
    private static final MaskingSegment DIGIT_4_NOT_MASK_SEGMENT =
            MaskingSegment.of(false, MaskingSegmentType.DIGIT, 4);
    private static final MaskingSegment LEFT_OVER_MASK_SEGMENT =
            MaskingSegment.of(true, MaskingSegmentType.LEFT_OVER, null);
    private static final MaskingSegment LEFT_OVER_NOT_MASK_SEGMENT =
            MaskingSegment.of(false, MaskingSegmentType.LEFT_OVER, null);

    public static List<MaskingSegment> generateSegments(MaskingSegmentsType type, String replaceValue) {
        List<MaskingSegment> maskSegments = new ArrayList<>();
        switch (type) {
            case PRE_1_POST_1:
                maskSegments.add(DIGIT_1_NOT_MASK_SEGMENT);
                maskSegments.add(LEFT_OVER_MASK_SEGMENT);
                maskSegments.add(DIGIT_1_NOT_MASK_SEGMENT);
                break;
            case PRE_3_POST_2:
                maskSegments.add(DIGIT_3_NOT_MASK_SEGMENT);
                maskSegments.add(LEFT_OVER_MASK_SEGMENT);
                maskSegments.add(DIGIT_2_NOT_MASK_SEGMENT);
                break;
            case PRE_3_POST_4:
                maskSegments.add(DIGIT_3_NOT_MASK_SEGMENT);
                maskSegments.add(LEFT_OVER_MASK_SEGMENT);
                maskSegments.add(DIGIT_4_NOT_MASK_SEGMENT);
                break;
            case ALL:
                LEFT_OVER_MASK_SEGMENT.setReplacedCharacters(replaceValue);
                maskSegments.add(LEFT_OVER_MASK_SEGMENT);
                break;
            case PRE_3:
                DIGIT_3_MASK_SEGMENT.setReplacedCharacters(replaceValue);
                maskSegments.add(DIGIT_3_MASK_SEGMENT);
                maskSegments.add(LEFT_OVER_NOT_MASK_SEGMENT);
                break;
            case POST_4:
                DIGIT_4_MASK_SEGMENT.setReplacedCharacters(replaceValue);
                maskSegments.add(LEFT_OVER_NOT_MASK_SEGMENT);
                maskSegments.add(DIGIT_4_MASK_SEGMENT);
                break;
            case CUSTOM:
            default:
                break;
        }
        return maskSegments;
    }

    public static boolean isPre1Post1(List<MaskingSegment> segments) {
        return segments.size() == 3 && segments.get(0).equals(DIGIT_1_NOT_MASK_SEGMENT)
                && segments.get(1).equals(LEFT_OVER_MASK_SEGMENT) && segments.get(2).equals(DIGIT_1_NOT_MASK_SEGMENT);
    }

    public static boolean isPre3Post2(List<MaskingSegment> segments) {
        return segments.size() == 3 && segments.get(0).equals(DIGIT_3_NOT_MASK_SEGMENT)
                && segments.get(1).equals(LEFT_OVER_MASK_SEGMENT) && segments.get(2).equals(DIGIT_2_NOT_MASK_SEGMENT);
    }

    public static boolean isPre3Post4(List<MaskingSegment> segments) {
        return segments.size() == 3 && segments.get(0).equals(DIGIT_3_NOT_MASK_SEGMENT)
                && segments.get(1).equals(LEFT_OVER_MASK_SEGMENT) && segments.get(2).equals(DIGIT_4_NOT_MASK_SEGMENT);
    }

    public static boolean isAll(List<MaskingSegment> segments) {
        return segments.size() == 1 && segments.get(0).equals(LEFT_OVER_MASK_SEGMENT);
    }

    public static boolean isPre3(List<MaskingSegment> segments) {
        return segments.size() == 2 && segments.get(0).equals(DIGIT_3_MASK_SEGMENT)
                && segments.get(1).equals(LEFT_OVER_NOT_MASK_SEGMENT);
    }

    public static boolean isPost4(List<MaskingSegment> segments) {
        return segments.size() == 2 && segments.get(0).equals(LEFT_OVER_NOT_MASK_SEGMENT)
                && segments.get(1).equals(DIGIT_4_MASK_SEGMENT);
    }
}
