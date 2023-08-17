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
package com.oceanbase.odc.core.datamasking.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/8/23
 */

@Slf4j
@Data
public class MaskConfig {
    private Map<String, FieldConfig> name2FieldConfig = new HashMap<>();

    public void addFieldConfig(FieldConfig fieldConfig) {
        if (name2FieldConfig.containsKey(fieldConfig.getFieldName())) {
            log.error("Field name: {} existed", fieldConfig.getFieldName());
            throw new UnsupportedOperationException(
                    String.format("Field name: {} existed", fieldConfig.getFieldName()));
        }
        name2FieldConfig.put(fieldConfig.getFieldName(), fieldConfig);
    }
}
