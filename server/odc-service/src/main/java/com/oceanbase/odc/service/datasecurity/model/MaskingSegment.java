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

import javax.validation.constraints.NotNull;

import com.oceanbase.odc.core.datamasking.algorithm.Segment;
import com.oceanbase.odc.core.datamasking.algorithm.SegmentType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author gaoda.xy
 * @date 2023/5/18 11:15
 */
@Data
@EqualsAndHashCode
public class MaskingSegment {
    @NotNull
    private Boolean mask;

    @NotNull
    private MaskingSegmentType type;

    private String replacedCharacters;

    private String delimiter;

    private Integer digitPercentage;

    private Integer digitNumber;

    public static MaskingSegment of(Boolean mask, MaskingSegmentType type, Integer digitNumber) {
        MaskingSegment segment = new MaskingSegment();
        segment.setMask(mask);
        segment.setDigitNumber(digitNumber);
        segment.setType(type);
        return segment;
    }

    public static MaskingSegment of(Boolean mask, MaskingSegmentType type, Integer digitNumber,
            String replacedCharacters) {
        MaskingSegment segment = MaskingSegment.of(mask, type, digitNumber);
        segment.setReplacedCharacters(replacedCharacters);
        return segment;
    }

    public Segment toSegmentParameter() {
        Segment segment = new Segment();
        segment.setMask(mask);
        segment.setType(SegmentType.valueOf(type.name()));
        segment.setDelimiter(delimiter);
        segment.setDigitPercentage(digitPercentage);
        segment.setDigitNumber(digitNumber);
        segment.setReplacedCharacters(replacedCharacters);
        return segment;
    }

}
