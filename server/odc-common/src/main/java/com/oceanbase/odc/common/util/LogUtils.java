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

import java.util.Objects;

public abstract class LogUtils {
    private static final int DEFAULT_PREFIX_SIZE = 200;
    private static final long DURATION_PERF_LEVEL_0_MILLIS = 1L;
    private static final long DURATION_PERF_LEVEL_1_MILLIS = 10L;
    private static final long DURATION_PERF_LEVEL_2_MILLIS = 100L;
    private static final long DURATION_PERF_LEVEL_3_MILLIS = 1000L;
    private static final long DURATION_PERF_LEVEL_4_MILLIS = 10_000L;
    private static final long DURATION_PERF_LEVEL_5_MILLIS = 100_000L;
    private static final long DURATION_PERF_LEVEL_6_MILLIS = 1000_000L;

    private LogUtils() {}

    public static String prefix(String content) {
        return prefix(content, DEFAULT_PREFIX_SIZE);
    }

    /**
     * prefix string content for reduce log output size
     */
    public static String prefix(String content, int fixedSize) {
        if (Objects.isNull(content)) {
            return null;
        }
        return StringUtils.substring(content, 0, fixedSize);
    }

    /**
     * calculate performance level by duration
     */
    public static String perfLevel(long durationMillis) {
        if (durationMillis < DURATION_PERF_LEVEL_0_MILLIS) {
            return "P0";
        }
        if (durationMillis < DURATION_PERF_LEVEL_1_MILLIS) {
            return "P1";
        }
        if (durationMillis < DURATION_PERF_LEVEL_2_MILLIS) {
            return "P2";
        }
        if (durationMillis < DURATION_PERF_LEVEL_3_MILLIS) {
            return "P3";
        }
        if (durationMillis < DURATION_PERF_LEVEL_4_MILLIS) {
            return "P4";
        }
        if (durationMillis < DURATION_PERF_LEVEL_5_MILLIS) {
            return "P5";
        }
        if (durationMillis < DURATION_PERF_LEVEL_6_MILLIS) {
            return "P6";
        }
        return "P7";
    }
}
