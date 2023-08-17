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
package com.oceanbase.odc.common.util;

import java.time.Duration;
import java.time.temporal.Temporal;

/**
 * @Author: Lebie
 * @Date: 2022/3/15 下午8:30
 * @Description: []
 */
public class TimeUtils {
    /**
     * Calculate absolute interval between two temporal object.
     *
     * @param startInclusive the start instant, inclusive
     * @param endExclusive the end instant, exclusive
     * @return absolute interval milliseconds
     */
    public static long absMillisBetween(Temporal startInclusive, Temporal endExclusive) {
        return Duration.between(startInclusive, endExclusive).abs().toMillis();
    }
}
