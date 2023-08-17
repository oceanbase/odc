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
package com.oceanbase.odc.test.tool;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import io.github.benas.randombeans.randomizers.range.OffsetDateTimeRangeRandomizer;

public class UtcOffsetDateTimeRangeRandomizer extends OffsetDateTimeRangeRandomizer {

    public UtcOffsetDateTimeRangeRandomizer(OffsetDateTime min, OffsetDateTime max) {
        super(min, max);
    }

    public UtcOffsetDateTimeRangeRandomizer(OffsetDateTime min, OffsetDateTime max, long seed) {
        super(min, max, seed);
    }

    @Override
    public OffsetDateTime getRandomValue() {
        OffsetDateTime randomValue = super.getRandomValue();
        return OffsetDateTime.ofInstant(randomValue.toInstant(), ZoneOffset.UTC);
    }
}
