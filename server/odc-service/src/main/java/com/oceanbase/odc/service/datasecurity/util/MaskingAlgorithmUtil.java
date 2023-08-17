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
package com.oceanbase.odc.service.datasecurity.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.datamasking.algorithm.AlgorithmFactory;
import com.oceanbase.odc.core.datamasking.algorithm.Hash.HashType;
import com.oceanbase.odc.core.datamasking.algorithm.Segment;
import com.oceanbase.odc.core.datamasking.config.FieldConfig;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithmType;
import com.oceanbase.odc.service.datasecurity.model.MaskingSegment;
import com.oceanbase.odc.service.datasecurity.model.MaskingSegmentsType;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/5/18 14:16
 */
@Slf4j
public class MaskingAlgorithmUtil {
    public static final String RANGE_DELIMITER = "~";

    public static MaskConfig toSingleFieldMaskConfig(MaskingAlgorithm algorithm, String fieldName) {
        MaskConfig maskConfig = new MaskConfig();
        Map<String, Object> params = toAlgorithmParameters(algorithm);
        FieldConfig fieldConfig = FieldConfig.builder().fieldName(fieldName).algorithmType(algorithm.getType().name())
                .algorithmParams(params).build();
        maskConfig.addFieldConfig(fieldConfig);
        return maskConfig;
    }

    public static Map<String, Object> toAlgorithmParameters(MaskingAlgorithm algorithm) {
        Map<String, Object> properties = new HashMap<>();
        switch (algorithm.getType()) {
            case PSEUDO:
                String characterCollectionStr = processCharacterCollection(algorithm.getCharsets());
                properties.put(AlgorithmFactory.PSEUDO_CHARACTERS_KEY, characterCollectionStr);
                break;
            case HASH:
                HashType hashType = algorithm.getHashType();
                properties.put(AlgorithmFactory.HASH_TYPE_KEY, hashType.name());
                break;
            case ROUNDING:
                Boolean decimal = algorithm.getDecimal();
                properties.put(AlgorithmFactory.ROUNDING_IS_DECIMAL_KEY, decimal.toString());
                int precision = algorithm.getPrecision();
                properties.put(AlgorithmFactory.ROUNDING_PRECISION_KEY, String.valueOf(precision));
                break;
            case MASK:
            case SUBSTITUTION:
                List<MaskingSegment> maskSegments;
                List<Segment> segmentParams;
                if (Objects.nonNull(algorithm.getSegmentsType())
                        && MaskingSegmentsType.CUSTOM != algorithm.getSegmentsType()) {
                    if (MaskingAlgorithmType.MASK == algorithm.getType()) {
                        maskSegments = MaskingSegmentsType.generateSegments(algorithm.getSegmentsType(), null);
                    } else {
                        maskSegments = MaskingSegmentsType.generateSegments(algorithm.getSegmentsType(),
                                algorithm.getSubstitution());
                    }
                } else {
                    maskSegments = algorithm.getSegments();
                }
                segmentParams =
                        maskSegments.stream().map(MaskingSegment::toSegmentParameter).collect(Collectors.toList());
                properties.put(AlgorithmFactory.SEGMENTS_KEY, segmentParams);
                break;
            case NULL:
            default:
                break;
        }
        return properties;
    }

    public static String processCharacterCollection(List<String> charsets) {
        List<Character> characterList = new ArrayList<>();
        Set<Character> deduplicateSet = new HashSet<>();
        for (String characterStr : charsets) {
            if (characterStr.length() == 1 && !deduplicateSet.contains(characterStr.charAt(0))) {
                // it is a single character
                characterList.add(characterStr.charAt(0));
                deduplicateSet.add(characterStr.charAt(0));
            } else {
                String[] splitResult = characterStr.split(RANGE_DELIMITER);
                if (splitResult.length < 2) {
                    log.warn("Range characters is not valid: {}, will be ignored", characterStr);
                } else {
                    int startAsciiCode = splitResult[0].charAt(0);
                    int endAsciiCode = splitResult[1].charAt(0);
                    for (int i = startAsciiCode; i <= endAsciiCode; i++) {
                        if (!deduplicateSet.contains((char) i)) {
                            characterList.add((char) i);
                            deduplicateSet.add((char) i);
                        }
                    }
                }
            }
        }
        return StringUtils.join(characterList, ',');
    }
}
