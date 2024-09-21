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
package com.oceanbase.odc.service.loaddata.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xien.sxe
 * @date 2024/03/04
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class DataTypeWithPrecision {

    private DataType type;
    private int length;
    private int precision;

    public DataTypeWithPrecision(DataType type) {
        this.type = type;
    }

    public DataTypeWithPrecision(DataType type, int length) {
        this.type = type;
        this.length = length;
    }

    public DataTypeWithPrecision(DataType type, int length, int precision) {
        this.type = type;
        this.length = length;
        this.precision = precision;
    }

    @Override
    public String toString() {
        String type = this.type.name();
        if (length == 0 && precision == 0) {
            return type;
        } else if (length == 0) {
            return type + '(' + precision + ')';
        } else if (precision == 0) {
            return type + '(' + length + ')';
        } else {
            return type + '(' + length + ',' + precision + ')';
        }
    }
}
