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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oceanbase.odc.core.datamasking.config.FieldConfig;
import com.oceanbase.odc.core.datamasking.config.MaskConfig;
import com.oceanbase.odc.core.datamasking.data.Data;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/8/23
 */

@Slf4j
public class AlgorithmSelector {
    private Map<String, Algorithm> fieldName2Algorithm = new HashMap<>();

    public AlgorithmSelector(MaskConfig maskConfig) {
        init(maskConfig);
    }

    private void init(MaskConfig maskConfig) {
        for (FieldConfig fieldConfig : maskConfig.getName2FieldConfig().values()) {
            String fieldName = fieldConfig.getFieldName();
            AlgorithmEnum algorithmType = AlgorithmEnum.valueOf(fieldConfig.getAlgorithmType().toUpperCase());
            Algorithm algorithm =
                    AlgorithmFactory.createAlgorithm(algorithmType, fieldConfig.getAlgorithmParams());
            fieldName2Algorithm.put(fieldName, algorithm);
        }
        log.info("Algorithms have been created for fields: {}", fieldName2Algorithm.keySet());
    }

    public Algorithm getAlgorithm(String fieldName) {
        return fieldName2Algorithm.get(fieldName);
    }

    public Data mask(Data data) {
        Algorithm algorithm = fieldName2Algorithm.get(data.getMetadata().getFieldName());
        if (Objects.nonNull(algorithm)) {
            return algorithm.mask(data);
        }
        return data;
    }
}
