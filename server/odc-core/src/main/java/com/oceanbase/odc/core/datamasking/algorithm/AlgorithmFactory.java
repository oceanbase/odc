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
import java.util.Map;

import com.oceanbase.odc.core.datamasking.algorithm.pseudonymization.Pseudonymization;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/8/24
 */

@Slf4j
public class AlgorithmFactory {
    // hash rule
    public static final String HASH_TYPE_KEY = "hash_type";
    // pseudo rule
    public static final String PSEUDO_CHARACTERS_KEY = "pseudo_characters";
    // rounding rule
    public static final String ROUNDING_PRECISION_KEY = "rounding_precision";
    public static final String ROUNDING_IS_DECIMAL_KEY = "rounding_is_decimal";
    // mask & substitution
    public static final String SEGMENTS_KEY = "segments";

    public static Algorithm createAlgorithm(AlgorithmEnum algorithmType, Map<String, Object> algorithmParams) {
        log.info("Algorithm type: {}, params: {}", algorithmType, algorithmParams);
        Algorithm algorithm;
        switch (algorithmType) {
            case NULL:
                algorithm = new Null();
                break;
            case HASH:
                String hashType = String.valueOf(algorithmParams.get(HASH_TYPE_KEY));
                algorithm = new Hash(hashType);
                break;
            case PSEUDO:
                String characters = String.valueOf(algorithmParams.get(PSEUDO_CHARACTERS_KEY));
                algorithm = new Pseudonymization(characters);
                break;
            case ROUNDING:
                boolean decimal = Boolean.valueOf(String.valueOf(algorithmParams.get(ROUNDING_IS_DECIMAL_KEY)));
                int precision = Integer.valueOf(String.valueOf(algorithmParams.get(ROUNDING_PRECISION_KEY))).intValue();
                algorithm = new Rounding(decimal, precision);
                break;
            case MASK:
                algorithm = new Mask((List) algorithmParams.get(SEGMENTS_KEY));
                break;
            case SUBSTITUTION:
                algorithm = new Substitution((List) algorithmParams.get(SEGMENTS_KEY));
                break;
            default:
                algorithm = null;
        }
        return algorithm;
    }
}
