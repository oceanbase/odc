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

import org.apache.commons.lang3.ArrayUtils;

public class ExceptionUtils extends org.apache.commons.lang3.exception.ExceptionUtils {
    private static final int CAUSE_DEPTH = 3;
    private static final int ROOT_CAUSE_DEPTH = 5;

    public static String getSimpleReason(final Throwable throwable) {
        return getCauseReason(throwable) + " ... " + getRootCauseReason(throwable);
    }

    public static String getCauseReason(final Throwable throwable) {
        String[] stackTrace = getStackTrace(throwable).split("\n\t");
        return getTraceWithMaxDepth(stackTrace, CAUSE_DEPTH);
    }

    public static String getRootCauseReason(final Throwable throwable) {
        String[] rootCauseStackTrace = getRootCauseStackTrace(throwable);
        return getTraceWithMaxDepth(rootCauseStackTrace, ROOT_CAUSE_DEPTH);
    }

    private static String getTraceWithMaxDepth(String[] stackTrace, int maxDepth) {
        StringBuilder rootReason = new StringBuilder();
        if (ArrayUtils.isNotEmpty(stackTrace)) {
            int depth = maxDepth;
            while (depth-- > 0) {
                if (stackTrace.length > depth) {
                    rootReason.append(" ").append(stackTrace[depth]);
                }
            }
        }
        return rootReason.toString();
    }

}
