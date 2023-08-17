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

import org.apache.commons.lang3.Validate;

import lombok.Getter;

/**
 * {@link BinarySize}
 *
 * @author yh263208
 * @date 2022-11-09 22:13
 * @since ODC_release_4.1.0
 */
@Getter
public class BinarySize implements Comparable<BinarySize> {

    private static final BinarySizeUnit[] ORDERED_SIZE_UNIT_DESC = new BinarySizeUnit[] {
            BinarySizeUnit.EB,
            BinarySizeUnit.PB,
            BinarySizeUnit.TB,
            BinarySizeUnit.GB,
            BinarySizeUnit.MB,
            BinarySizeUnit.KB,
            BinarySizeUnit.B,
    };
    private final long sizeDigit;
    private final BinarySizeUnit sizeUnit;
    private BinarySize mod;

    BinarySize(long sizeDigit, BinarySizeUnit sizeUnit) {
        Validate.isTrue(sizeDigit >= 0, "Size is overflow");
        long limit = sizeUnit.upperLimit;
        Validate.isTrue(sizeDigit <= limit,
                "Upper limit for " + sizeUnit + " is " + limit + ", actual is " + sizeDigit);
        this.sizeDigit = sizeDigit;
        this.sizeUnit = sizeUnit;
    }

    @Override
    public int compareTo(BinarySize that) {
        BinarySize thisSize = convert(BinarySizeUnit.B);
        BinarySize thatSize = that.convert(BinarySizeUnit.B);
        return Long.compare(thisSize.sizeDigit, thatSize.sizeDigit);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.convert(BinarySizeUnit.B).getSizeDigit());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BinarySize)) {
            return false;
        }
        BinarySize that = (BinarySize) obj;
        return compareTo(that) == 0;
    }

    public BinarySize convert(BinarySizeUnit thatSizeUnit) {
        long sizeBytes = this.sizeDigit << this.sizeUnit.byteOffset;
        BinarySize tmpMod = mod;
        while (tmpMod != null) {
            sizeBytes |= tmpMod.sizeDigit << tmpMod.sizeUnit.byteOffset;
            tmpMod = tmpMod.mod;
        }
        int index = indexOf(thatSizeUnit);
        BinarySize returnVal = null;
        BinarySize prev = null;
        for (int i = index; i < ORDERED_SIZE_UNIT_DESC.length && sizeBytes > 0; i++) {
            BinarySizeUnit tmpUnit = ORDERED_SIZE_UNIT_DESC[i];
            BinarySize current;
            if (sizeBytes <= tmpUnit.byteBoundary) {
                current = new BinarySize(0, tmpUnit);
            } else {
                current = new BinarySize(sizeBytes >> tmpUnit.byteOffset, tmpUnit);
                sizeBytes &= tmpUnit.byteBoundary;
            }
            if (returnVal == null) {
                returnVal = current;
            }
            if (prev != null) {
                prev.mod = current;
            }
            prev = current;
        }
        return returnVal == null ? new BinarySize(0, thatSizeUnit) : returnVal;
    }

    @Override
    public String toString() {
        BinarySizeUnit targetUnit = null;
        for (BinarySizeUnit unit : ORDERED_SIZE_UNIT_DESC) {
            if (this.compareTo(unit.of(1)) < 0) {
                continue;
            }
            targetUnit = unit;
            break;
        }
        if (targetUnit == null) {
            return "0 B";
        }
        StringBuilder buffer = new StringBuilder();
        BinarySize tmp = this.convert(targetUnit);
        if (tmp.mod != null) {
            buffer.append(String.format("%.1f", (tmp.sizeDigit * 1024 + tmp.mod.sizeDigit) / 1024.0));
        } else {
            buffer.append(tmp.sizeDigit);
        }
        return buffer.append(" ").append(tmp.sizeUnit.name()).toString();
    }

    private static int indexOf(BinarySizeUnit unit) {
        switch (unit) {
            case EB:
                return 0;
            case PB:
                return 1;
            case TB:
                return 2;
            case GB:
                return 3;
            case MB:
                return 4;
            case KB:
                return 5;
            case B:
                return 6;
            default:
                throw new IllegalArgumentException("Unknown size unit, " + unit);
        }
    }

}
