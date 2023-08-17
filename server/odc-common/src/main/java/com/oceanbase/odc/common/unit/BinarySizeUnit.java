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
package com.oceanbase.odc.common.unit;

/**
 * {@link BinarySizeUnit}
 *
 * @author yh263208
 * @date 2022-11-10 10:44
 * @since ODC_release_4.1.0
 */
public enum BinarySizeUnit {
    /**
     * Size unit of binary
     */
    EB(60, 0x7L),
    PB(50, 0x1fffL),
    TB(40, 0x7fffffL),
    GB(30, 0x1ffffffffL),
    MB(20, 0x7ffffffffffL),
    KB(10, 0x1fffffffffffffL),
    B(0, Long.MAX_VALUE);

    static final long SIZE_BOUNDARY = 0x0fffffffffffffffL;
    final int byteOffset;
    final long upperLimit;
    final long byteBoundary;

    BinarySizeUnit(int byteOffset, long upperLimit) {
        this.byteOffset = byteOffset;
        this.upperLimit = upperLimit;
        if (byteOffset == 0) {
            this.byteBoundary = 0;
        } else {
            this.byteBoundary = BinarySizeUnit.SIZE_BOUNDARY >> (60 - byteOffset);
        }
    }

    public BinarySize of(long sizeDigit) {
        return new BinarySize(sizeDigit, this);
    }

}
