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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.LongRangeRandomizer;

public class TestRandom {

    private static EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(123L)
            .objectPoolSize(1500).randomizationDepth(3).charset(StandardCharsets.UTF_8).stringLengthRange(5, 15)
            .collectionSizeRange(1, 3).scanClasspathForConcreteTypes(true).overrideDefaultInitialization(true)
            .randomize(Integer.class, new IntegerRangeRandomizer(1, 100))
            .randomize(Long.class, new LongRangeRandomizer(1L, 10000L))
            .randomize(OffsetDateTime.class,
                    new UtcOffsetDateTimeRangeRandomizer(OffsetDateTime.parse("2019-05-16T00:00:00Z"),
                            OffsetDateTime.parse("2019-05-20T00:00:00Z")))
            .dateRange(LocalDate.parse("2019-05-16"), LocalDate.parse("2019-05-20"))
            .timeRange(LocalTime.parse("00:00:00"), LocalTime.parse("10:00:00")).build();

    public static <T> T nextObject(Class<T> classType) {
        return random.nextObject(classType);
    }

}
