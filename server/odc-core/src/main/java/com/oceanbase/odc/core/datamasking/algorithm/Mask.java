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

import java.util.List;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.core.datamasking.data.Data;
import com.oceanbase.odc.core.datamasking.util.SegmentUtils;

/**
 * @author wenniu.ly
 * @date 2022/8/27
 */

public class Mask implements Algorithm {
    private static final String DEFAULT_FIVE_STARS = "*****";
    private List<Segment> segments;

    public Mask(List<Segment> segments) {
        Preconditions.checkArgument(segments.size() > 0, "Mask segments size should be > 0");
        this.segments = segments;
    }

    @Override
    public Data mask(Data data) {
        String origin = data.getValue();
        StringBuilder stringBuilder = new StringBuilder();
        List<Segment> resultSegments = SegmentUtils.splitSegments(segments, origin);
        for (Segment segment : resultSegments) {
            String content = segment.getContent();
            if (SegmentType.NULL == segment.getType() || !segment.getMask()) {
                stringBuilder.append(content);
            } else {
                int counter = content.length();
                while (counter > DEFAULT_FIVE_STARS.length()) {
                    stringBuilder.append(DEFAULT_FIVE_STARS);
                    counter -= DEFAULT_FIVE_STARS.length();
                }
                String leftStars = DEFAULT_FIVE_STARS.substring(0, counter);
                stringBuilder.append(leftStars);
            }
            if (SegmentType.DELIMITER == segment.getType()) {
                stringBuilder.append(segment.getDelimiter());
            }
        }
        data.setValue(stringBuilder.toString());
        return data;
    }

    @Override
    public AlgorithmEnum getType() {
        return AlgorithmEnum.MASK;
    }
}
