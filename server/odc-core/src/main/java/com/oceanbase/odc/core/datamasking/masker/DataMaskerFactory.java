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
package com.oceanbase.odc.core.datamasking.masker;

import com.oceanbase.odc.core.datamasking.config.MaskConfig;

/**
 * @author wenniu.ly
 * @date 2022/8/23
 */

public class DataMaskerFactory {
    public AbstractDataMasker createDataMasker(String typeStr, MaskConfig config) {
        MaskValueType type = MaskValueType.valueOf(typeStr.toUpperCase());
        switch (type) {
            case SINGLE_VALUE:
                return new SingleValueDataMasker(config);
            case CSV:
            case JSON:
            default:
                return null;
        }
    }
}
