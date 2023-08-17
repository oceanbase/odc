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
package com.oceanbase.odc.core.datamasking.data.metadata;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2022/8/23
 */

@Data
public abstract class Metadata<T> {
    protected String dataType;
    protected String fieldName;

    public abstract T convert(String value);

    public Metadata withType(String dataType) {
        this.dataType = dataType;
        return this;
    }

    public Metadata withFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public static boolean isNumeric(Metadata metadata) {
        return (metadata instanceof DoubleMetadata) || (metadata instanceof FloatMetadata)
                || (metadata instanceof IntegerMetadata) || (metadata instanceof LongMetadata);
    }
}
