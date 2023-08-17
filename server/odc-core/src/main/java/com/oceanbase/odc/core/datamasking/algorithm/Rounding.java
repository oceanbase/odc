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

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.common.base.Preconditions;
import com.oceanbase.odc.core.datamasking.data.Data;
import com.oceanbase.odc.core.datamasking.data.metadata.Metadata;

/**
 * @author wenniu.ly
 * @date 2022/8/27
 */

public class Rounding implements Algorithm {
    private boolean decimal;
    private int precision;

    public Rounding(boolean decimal, int precision) {
        Preconditions.checkArgument(precision >= 0, String.format("Param precision:%s should be >= 0", precision));
        this.decimal = decimal;
        this.precision = precision;
    }

    @Override
    public Data mask(Data data) {
        boolean isNumeric = Metadata.isNumeric(data.getMetadata());
        if (isNumeric) {
            BigDecimal bigDecimal = new BigDecimal(data.getValue());
            if (decimal) {
                bigDecimal = bigDecimal.setScale(precision, RoundingMode.DOWN);
            } else {
                BigDecimal base = new BigDecimal(Math.pow(10, precision));
                bigDecimal =
                        bigDecimal.divide(base, 0, RoundingMode.DOWN).multiply(base).setScale(0, RoundingMode.DOWN);
            }
            data.setValue(bigDecimal.toString());
        }
        return data;
    }

    @Override
    public AlgorithmEnum getType() {
        return AlgorithmEnum.ROUNDING;
    }
}
